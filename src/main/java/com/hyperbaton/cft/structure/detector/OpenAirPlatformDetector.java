package com.hyperbaton.cft.structure.detector;

import com.google.common.collect.Sets;
import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.structure.*;
import com.hyperbaton.cft.structure.type.OpenAirPlatformStructureType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.function.Predicate;

public class OpenAirPlatformDetector implements StructureDetector {

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId,
                                           StructureType structureType) {
        if (!(structureType instanceof OpenAirPlatformStructureType platformType)) {
            throw new IllegalArgumentException("OpenAirPlatformDetector requires OpenAirPlatformStructureType");
        }

        int groundY = keyBlockPos.getY() - 1;
        int borderY = groundY + 1;

        Set<BlockPos> borderBlocks = Sets.newHashSet();
        if (!traceBorder(level, keyBlockPos, borderY, platformType, borderBlocks)) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_BORDER);
        }
        if (borderBlocks.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_BORDER);
        }

        if (!isClosedOneBlockWideLoop(borderBlocks)) {
            return StructureDetectionResult.failure(StructureDetectionReasons.BORDER_NOT_CLOSED);
        }

        Predicate<net.minecraft.world.level.block.state.BlockState> noSkip = bs -> false;

        Set<BlockPos> borderColumnBlocks = collectBorderColumn(borderBlocks, groundY, platformType.getWallHeight());
        List<String> borderErrors = BuildingDetectionUtils.checkValidBlocks(level, borderColumnBlocks,
                platformType.getBorderBlocks(), noSkip);
        if (!borderErrors.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_BORDER, borderErrors);
        }

        Set<BlockPos> groundPerimeterBlocks = Sets.newHashSet();
        for (BlockPos borderPos : borderBlocks) {
            groundPerimeterBlocks.add(new BlockPos(borderPos.getX(), groundY, borderPos.getZ()));
        }
        List<String> groundPerimeterErrors = BuildingDetectionUtils.checkValidBlocks(level, groundPerimeterBlocks,
                platformType.getGroundPerimeterBlocks(), noSkip);
        if (!groundPerimeterErrors.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_GROUND_PERIMETER, groundPerimeterErrors);
        }

        BlockPos interiorStart = findInteriorStart(keyBlockPos, borderBlocks, groundY);
        if (interiorStart == null) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_SURFACE);
        }

        Set<BlockPos> surfaceBlocks = Sets.newHashSet();
        if (!floodFillSurface(level, interiorStart, groundY, borderBlocks, surfaceBlocks, platformType)) {
            return StructureDetectionResult.failure(StructureDetectionReasons.SURFACE_TOO_BIG);
        }
        if (surfaceBlocks.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_SURFACE);
        }

        List<String> surfaceErrors = BuildingDetectionUtils.checkValidBlocks(level, surfaceBlocks,
                platformType.getSurfaceBlocks(), noSkip);
        if (!surfaceErrors.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_SURFACE, surfaceErrors);
        }

        for (BlockPos surfacePos : surfaceBlocks) {
            if (!level.canSeeSky(surfacePos.above())) {
                return StructureDetectionResult.failure(StructureDetectionReasons.NO_SKY_ACCESS);
            }
        }

        if (structureType.isRequiresContainer() && !BuildingDetectionUtils.hasContainers(level, surfaceBlocks)) {
            return StructureDetectionResult.failure(StructureDetectionReasons.NO_CONTAINER);
        }

        Set<BlockPos> allBlocks = Sets.newHashSet();
        allBlocks.addAll(borderColumnBlocks);
        allBlocks.addAll(groundPerimeterBlocks);
        allBlocks.addAll(surfaceBlocks);

        if (allBlocks.size() > CftConfig.MAX_HOUSE_SIZE.get()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.STRUCTURE_TOO_LARGE);
        }

        Map<String, List<BlockPos>> blockPositions = new HashMap<>();
        blockPositions.put(OpenAirPlatformBlockGroup.BORDER.getKey(), new ArrayList<>(borderColumnBlocks));
        blockPositions.put(OpenAirPlatformBlockGroup.GROUND_PERIMETER.getKey(), new ArrayList<>(groundPerimeterBlocks));
        blockPositions.put(OpenAirPlatformBlockGroup.SURFACE.getKey(), new ArrayList<>(surfaceBlocks));

        Structure structure = new Structure(
                keyBlockPos, allBlocks.size(), leaderId,
                structureType.getId(), structureType.getMaxUsers(), blockPositions
        );

        return StructureDetectionResult.success(structure);
    }

    private boolean traceBorder(ServerLevel level, BlockPos keyBlockPos, int borderY,
                                OpenAirPlatformStructureType platformType, Set<BlockPos> borderBlocks) {
        BlockPos keyAtBorderY = new BlockPos(keyBlockPos.getX(), borderY, keyBlockPos.getZ());
        if (isBorderBlock(level, keyAtBorderY, platformType)) {
            borderBlocks.add(keyAtBorderY);
        }

        BlockPos startBorder = findAdjacentBorder(level, keyBlockPos, borderY, platformType);
        if (startBorder == null && borderBlocks.isEmpty()) {
            return false;
        }

        Set<BlockPos> visited = Sets.newHashSet(borderBlocks);
        Deque<BlockPos> queue = new ArrayDeque<>();
        if (startBorder != null && !visited.contains(startBorder)) {
            queue.add(startBorder);
            visited.add(startBorder);
        }

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            borderBlocks.add(current);

            if (borderBlocks.size() > CftConfig.MAX_FLOOR_SIZE.get() * 4) {
                return false;
            }

            for (BlockPos neighbor : getHorizontalNeighbors(current)) {
                if (!visited.contains(neighbor) && isBorderBlock(level, neighbor, platformType)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return borderBlocks.size() >= 4;
    }

    private boolean isClosedOneBlockWideLoop(Set<BlockPos> borderBlocks) {
        for (BlockPos pos : borderBlocks) {
            int neighborCount = 0;
            for (BlockPos neighbor : getHorizontalNeighbors(pos)) {
                if (borderBlocks.contains(neighbor)) {
                    neighborCount++;
                }
            }
            if (neighborCount != 2) {
                return false;
            }
        }
        return true;
    }

    private BlockPos findInteriorStart(BlockPos keyBlockPos, Set<BlockPos> borderBlocks, int groundY) {
        Set<BlockPos> borderXZ = Sets.newHashSet();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos bp : borderBlocks) {
            borderXZ.add(new BlockPos(bp.getX(), groundY, bp.getZ()));
            minX = Math.min(minX, bp.getX());
            maxX = Math.max(maxX, bp.getX());
            minZ = Math.min(minZ, bp.getZ());
            maxZ = Math.max(maxZ, bp.getZ());
        }

        double centerX = (minX + maxX) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;

        int keyX = keyBlockPos.getX();
        int keyZ = keyBlockPos.getZ();

        double dx = centerX - keyX;
        double dz = centerZ - keyZ;

        BlockPos candidate;
        if (Math.abs(dx) >= Math.abs(dz)) {
            candidate = new BlockPos(keyX + (dx > 0 ? 1 : -1), groundY, keyZ);
        } else {
            candidate = new BlockPos(keyX, groundY, keyZ + (dz > 0 ? 1 : -1));
        }

        if (!borderXZ.contains(candidate)) {
            return candidate;
        }

        for (BlockPos neighbor : getHorizontalNeighbors(new BlockPos(keyX, groundY, keyZ))) {
            if (!borderXZ.contains(neighbor)) {
                return neighbor;
            }
        }

        return null;
    }

    private BlockPos findAdjacentBorder(ServerLevel level, BlockPos keyBlockPos, int borderY,
                                         OpenAirPlatformStructureType platformType) {
        for (BlockPos neighbor : getHorizontalNeighbors(new BlockPos(keyBlockPos.getX(), borderY, keyBlockPos.getZ()))) {
            if (isBorderBlock(level, neighbor, platformType)) {
                return neighbor;
            }
        }
        return null;
    }

    private boolean isBorderBlock(ServerLevel level, BlockPos pos, OpenAirPlatformStructureType platformType) {
        return BuildingDetectionUtils.isValidBlock(level.getBlockState(pos), platformType.getBorderBlocks());
    }

    private Set<BlockPos> collectBorderColumn(Set<BlockPos> borderBaseBlocks, int groundY, int wallHeight) {
        Set<BlockPos> columnBlocks = Sets.newHashSet();
        for (BlockPos basePos : borderBaseBlocks) {
            for (int dy = 1; dy <= wallHeight; dy++) {
                columnBlocks.add(new BlockPos(basePos.getX(), groundY + dy, basePos.getZ()));
            }
        }
        return columnBlocks;
    }

    private boolean floodFillSurface(ServerLevel level, BlockPos startPos, int groundY,
                                      Set<BlockPos> borderBlocks, Set<BlockPos> surfaceBlocks,
                                      OpenAirPlatformStructureType platformType) {
        Set<BlockPos> borderXZ = Sets.newHashSet();
        for (BlockPos bp : borderBlocks) {
            borderXZ.add(new BlockPos(bp.getX(), groundY, bp.getZ()));
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

            for (BlockPos neighbor : getHorizontalNeighbors(current)) {
                BlockPos groundNeighbor = new BlockPos(neighbor.getX(), groundY, neighbor.getZ());
                if (!visited.contains(groundNeighbor) && !borderXZ.contains(groundNeighbor)) {
                    if (BuildingDetectionUtils.isValidBlock(level.getBlockState(groundNeighbor), platformType.getSurfaceBlocks())) {
                        visited.add(groundNeighbor);
                        queue.add(groundNeighbor);
                    }
                }
            }
        }

        return true;
    }

    private List<BlockPos> getHorizontalNeighbors(BlockPos pos) {
        return List.of(pos.north(), pos.south(), pos.east(), pos.west());
    }
}
