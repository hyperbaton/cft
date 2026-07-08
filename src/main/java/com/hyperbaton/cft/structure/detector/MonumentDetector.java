package com.hyperbaton.cft.structure.detector;

import com.google.common.collect.Sets;
import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.structure.*;
import com.hyperbaton.cft.structure.type.MonumentStructureType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Predicate;

public class MonumentDetector implements StructureDetector<MonumentStructureType> {

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId,
                                           MonumentStructureType structureType) {

        List<Set<BlockPos>> layers = new ArrayList<>();
        Set<BlockPos> allBlocks = Sets.newHashSet();

        Set<BlockPos> layer0 = detectLayer(level, keyBlockPos, 0, structureType);
        if (layer0.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_MONUMENT_LAYER);
        }
        layers.add(layer0);
        allBlocks.addAll(layer0);

        for (int layerIndex = 1; layerIndex <= structureType.getMaxHeight() - 1; layerIndex++) {
            Set<BlockPos> previousLayer = layers.get(layerIndex - 1);
            Set<BlockPos> candidates = Sets.newHashSet();
            for (BlockPos pos : previousLayer) {
                BlockPos above = pos.above();
                List<ValidBlock> layerBlocks = structureType.getBlocksForLayer(layerIndex);
                if (layerBlocks == null) {
                    break;
                }
                if (BuildingDetectionUtils.isValidBlock(level.getBlockState(above), layerBlocks)) {
                    candidates.add(above);
                }
            }

            if (candidates.isEmpty()) {
                break;
            }

            Set<BlockPos> fullLayer = expandLayer(level, candidates, layerIndex, structureType);
            layers.add(fullLayer);
            allBlocks.addAll(fullLayer);

            if (allBlocks.size() > CftConfig.MAX_HOUSE_SIZE.get()) {
                return StructureDetectionResult.failure(StructureDetectionReasons.STRUCTURE_TOO_LARGE);
            }
        }

        int totalLayers = layers.size();
        if (totalLayers < structureType.getMinHeight()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.MONUMENT_TOO_SHORT);
        }
        if (totalLayers > structureType.getMaxHeight()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.MONUMENT_TOO_TALL);
        }

        Predicate<BlockState> noSkip = bs -> false;
        for (int i = 0; i < totalLayers; i++) {
            List<ValidBlock> layerBlocks = structureType.getBlocksForLayer(i);
            if (layerBlocks == null) {
                return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_MONUMENT_LAYER);
            }
            List<String> errors = BuildingDetectionUtils.checkValidBlocks(level, layers.get(i), layerBlocks, noSkip);
            if (!errors.isEmpty()) {
                return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_MONUMENT_LAYER, errors);
            }
        }

        for (IdenticalLayerGroup group : structureType.getIdenticalLayerGroups()) {
            int from = group.from();
            int to = Math.min(group.to(), totalLayers - 1);
            if (from >= totalLayers || from > to) {
                continue;
            }

            Set<BlockPos> referenceLayer = layers.get(from);
            int referenceY = from + keyBlockPos.getY();

            for (int i = from + 1; i <= to; i++) {
                Set<BlockPos> otherLayer = layers.get(i);
                if (!areLayersIdentical(level, referenceLayer, referenceY, otherLayer, i + keyBlockPos.getY())) {
                    return StructureDetectionResult.failure(StructureDetectionReasons.LAYERS_NOT_IDENTICAL);
                }
            }
        }

        if (structureType.isRequiresContainer() && !BuildingDetectionUtils.hasContainers(level, layers.get(0))) {
            return StructureDetectionResult.failure(StructureDetectionReasons.NO_CONTAINER);
        }

        Map<String, List<BlockPos>> blockPositions = new HashMap<>();
        blockPositions.put(MonumentBlockGroup.BODY.getKey(), new ArrayList<>(allBlocks));

        Structure structure = new Structure(
                keyBlockPos, allBlocks.size(), leaderId,
                structureType.getId(), structureType.getMaxUsers(), blockPositions
        );

        return StructureDetectionResult.success(structure);
    }

    private Set<BlockPos> detectLayer(ServerLevel level, BlockPos startPos, int layerIndex,
                                       MonumentStructureType monumentType) {
        List<ValidBlock> layerBlocks = monumentType.getBlocksForLayer(layerIndex);
        if (layerBlocks == null) {
            return Set.of();
        }

        Set<BlockPos> layer = Sets.newHashSet();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = Sets.newHashSet();

        if (!BuildingDetectionUtils.isValidBlock(level.getBlockState(startPos), layerBlocks)) {
            return layer;
        }

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            layer.add(current);

            if (layer.size() > CftConfig.MAX_FLOOR_SIZE.get()) {
                return layer;
            }

            for (BlockPos neighbor : getHorizontalNeighbors(current)) {
                if (!visited.contains(neighbor)
                        && BuildingDetectionUtils.isValidBlock(level.getBlockState(neighbor), layerBlocks)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return layer;
    }

    private Set<BlockPos> expandLayer(ServerLevel level, Set<BlockPos> candidates, int layerIndex,
                                       MonumentStructureType monumentType) {
        List<ValidBlock> layerBlocks = monumentType.getBlocksForLayer(layerIndex);
        if (layerBlocks == null) {
            return candidates;
        }

        Set<BlockPos> layer = Sets.newHashSet(candidates);
        Deque<BlockPos> queue = new ArrayDeque<>(candidates);
        Set<BlockPos> visited = Sets.newHashSet(candidates);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            if (layer.size() > CftConfig.MAX_FLOOR_SIZE.get()) {
                return layer;
            }

            for (BlockPos neighbor : getHorizontalNeighbors(current)) {
                if (!visited.contains(neighbor)
                        && BuildingDetectionUtils.isValidBlock(level.getBlockState(neighbor), layerBlocks)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    layer.add(neighbor);
                }
            }
        }

        return layer;
    }

    private boolean areLayersIdentical(ServerLevel level, Set<BlockPos> referenceLayer, int referenceY,
                                        Set<BlockPos> otherLayer, int otherY) {
        if (referenceLayer.size() != otherLayer.size()) {
            return false;
        }

        for (BlockPos refPos : referenceLayer) {
            BlockPos correspondingPos = new BlockPos(refPos.getX(), otherY, refPos.getZ());
            if (!otherLayer.contains(correspondingPos)) {
                return false;
            }
            if (!level.getBlockState(refPos).is(level.getBlockState(correspondingPos).getBlock())) {
                return false;
            }
        }

        return true;
    }

    private List<BlockPos> getHorizontalNeighbors(BlockPos pos) {
        return List.of(pos.north(), pos.south(), pos.east(), pos.west());
    }
}
