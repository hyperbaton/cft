package com.hyperbaton.cft.structure.detector;

import com.google.common.collect.Sets;
import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.structure.*;
import com.hyperbaton.cft.structure.type.CompoundStructureType;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Predicate;

/**
 * Detects a compound: an open surface validated by flood fill from below the key block,
 * whose "border" is not made of blocks but of other, already detected structures. The
 * attendance of required structure types is checked against StructuresData, measuring
 * the distance from each structure's key block to the nearest surface block.
 */
public class CompoundDetector implements StructureDetector<CompoundStructureType> {

    // How far below the key block the surface can be, so the key block can stand
    // on a decorative post or pillar (e.g. a bell on a column)
    private static final int MAX_KEY_BLOCK_PILLAR_HEIGHT = 5;

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId,
                                           CompoundStructureType structureType) {

        // The key block may stand directly on the paving or on top of a decorative
        // pillar: descend until the surface is found. Pillar blocks are ignored.
        BlockPos surfaceStart = null;
        BlockPos probe = keyBlockPos.below();
        for (int i = 0; i < MAX_KEY_BLOCK_PILLAR_HEIGHT; i++) {
            if (BuildingDetectionUtils.isValidBlock(level.getBlockState(probe), structureType.getSurfaceBlocks())) {
                surfaceStart = probe;
                break;
            }
            probe = probe.below();
        }
        if (surfaceStart == null) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_SURFACE,
                    List.of("No compound surface found below the key block"));
        }

        // Surface detection: flood fill valid surface blocks from the found start.
        // The paving material bounds the fill, so it must differ from the surrounding ground.
        Set<BlockPos> surfaceBlocks = Sets.newHashSet();
        if (!floodFillSurface(level, surfaceStart, surfaceBlocks, structureType)) {
            return StructureDetectionResult.failure(StructureDetectionReasons.SURFACE_TOO_BIG);
        }
        if (surfaceBlocks.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_SURFACE);
        }

        // A compound cannot share blocks with an already registered compound
        // (e.g. a second bell placed on an already detected square). Only compounds
        // are checked: no other structure type has an open surface a plaza could share.
        StructuresData structuresData = level.getDataStorage()
                .computeIfAbsent(StructuresData.factory(), "structuresData");
        Set<BlockPos> occupiedBlocks = Sets.newHashSet();
        for (Structure existing : structuresData.getStructures()) {
            if (!isCompoundType(existing.getStructureTypeId())) continue;
            // Skip the compound already registered at this key block: it is "itself"
            // when this detection is a re-validation of an existing compound
            if (existing.getKeyBlockPos().equals(keyBlockPos)) continue;
            occupiedBlocks.add(existing.getKeyBlockPos());
            existing.getBlockPositions().values().forEach(occupiedBlocks::addAll);
        }
        if (surfaceBlocks.stream().anyMatch(occupiedBlocks::contains)) {
            return StructureDetectionResult.failure(StructureDetectionReasons.OVERLAPPING_STRUCTURE);
        }

        Predicate<BlockState> noSkip = bs -> false;
        List<String> surfaceErrors = BuildingDetectionUtils.checkValidBlocks(level, surfaceBlocks,
                structureType.getSurfaceBlocks(), noSkip);
        if (!surfaceErrors.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_SURFACE, surfaceErrors);
        }

        if (structureType.isRequiresSkyAccess()) {
            for (BlockPos surfacePos : surfaceBlocks) {
                // The key block's column is exempt: it may hold the key block's pillar
                if (surfacePos.getX() == keyBlockPos.getX() && surfacePos.getZ() == keyBlockPos.getZ()) {
                    continue;
                }
                if (!level.canSeeSky(surfacePos.above())) {
                    return StructureDetectionResult.failure(StructureDetectionReasons.NO_SKY_ACCESS);
                }
            }
        }

        // Required structures: count detected structures of each type close enough
        // to the compound's surface
        List<String> structureErrors = new ArrayList<>();
        for (RequiredStructure requirement : structureType.getRequiredStructures()) {
            long count = structuresData.getStructures().stream()
                    .filter(s -> s.getStructureTypeId().equals(requirement.structureType()))
                    .filter(s -> s.getLeaderId().equals(leaderId))
                    .filter(s -> distanceToSurface(s.getKeyBlockPos(), surfaceBlocks) <= requirement.maxDistance())
                    .count();
            if (count < requirement.min()) {
                structureErrors.add(String.format(
                        "Found %d structures of type %s within %d blocks, but at least %d are required",
                        count, requirement.structureType(), requirement.maxDistance(), requirement.min()));
            } else if (count > requirement.max()) {
                structureErrors.add(String.format(
                        "Found %d structures of type %s within %d blocks, but at most %d are allowed",
                        count, requirement.structureType(), requirement.maxDistance(), requirement.max()));
            }
        }
        if (!structureErrors.isEmpty()) {
            return StructureDetectionResult.failure(
                    StructureDetectionReasons.MISSING_REQUIRED_STRUCTURES, structureErrors);
        }

        if (structureType.isRequiresContainer() && !BuildingDetectionUtils.hasContainers(level, surfaceBlocks)) {
            return StructureDetectionResult.failure(StructureDetectionReasons.NO_CONTAINER);
        }

        if (surfaceBlocks.size() > CftConfig.MAX_HOUSE_SIZE.get()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.STRUCTURE_TOO_LARGE);
        }

        Map<String, List<BlockPos>> blockPositions = new HashMap<>();
        blockPositions.put(OpenAirPlatformBlockGroup.SURFACE.getKey(), new ArrayList<>(surfaceBlocks));

        Structure structure = new Structure(
                keyBlockPos, surfaceBlocks.size(), leaderId,
                structureType.getId(), structureType.getMaxUsers(), blockPositions
        );
        return StructureDetectionResult.success(structure);
    }

    private boolean floodFillSurface(ServerLevel level, BlockPos startPos, Set<BlockPos> surfaceBlocks,
                                     CompoundStructureType compoundType) {
        if (!BuildingDetectionUtils.isValidBlock(level.getBlockState(startPos), compoundType.getSurfaceBlocks())) {
            return true;
        }

        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = Sets.newHashSet();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            surfaceBlocks.add(current);

            if (surfaceBlocks.size() > CftConfig.MAX_FLOOR_SIZE.get()) {
                return false;
            }

            for (BlockPos neighbor : List.of(current.north(), current.south(), current.east(), current.west())) {
                if (!visited.contains(neighbor)
                        && BuildingDetectionUtils.isValidBlock(level.getBlockState(neighbor),
                        compoundType.getSurfaceBlocks())) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return true;
    }

    private boolean isCompoundType(String structureTypeId) {
        if (com.hyperbaton.cft.CftRegistry.STRUCTURES == null) return false;
        return com.hyperbaton.cft.CftRegistry.STRUCTURES.stream()
                .filter(st -> st.getId().equals(structureTypeId))
                .anyMatch(st -> st instanceof CompoundStructureType);
    }

    private int distanceToSurface(BlockPos pos, Set<BlockPos> surfaceBlocks) {
        int best = Integer.MAX_VALUE;
        for (BlockPos surfacePos : surfaceBlocks) {
            best = Math.min(best, surfacePos.distManhattan(pos));
        }
        return best;
    }
}
