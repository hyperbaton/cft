package com.hyperbaton.cft.structure.detector;

import com.google.common.collect.Sets;
import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.structure.*;
import com.hyperbaton.cft.structure.type.EnclosedBuildingStructureType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Predicate;

public class EnclosedBuildingDetector implements StructureDetector {

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId,
                                           StructureType structureType) {
        if (!(structureType instanceof EnclosedBuildingStructureType enclosedType)) {
            throw new IllegalArgumentException("EnclosedBuildingDetector requires EnclosedBuildingStructureType");
        }

        Set<BlockPos> allBlocks = Sets.newHashSet();
        Set<BlockPos> floorBlockSet = Sets.newHashSet();
        Set<BlockPos> floorPerimeterBlocks = Sets.newHashSet();

        // No skip predicate — all blocks are validated through their HomeValidBlock entries
        Predicate<BlockState> noSkip = bs -> false;

        // Floor detection: start below the key block
        boolean foundFloor = BuildingDetectionUtils.findFloor(level, keyBlockPos.below(), floorBlockSet,
                floorPerimeterBlocks, enclosedType.getFloorBlocks(), CftConfig.MAX_FLOOR_SIZE.get());
        if (floorBlockSet.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.NO_FLOOR);
        }
        if (!foundFloor) {
            return StructureDetectionResult.failure(StructureDetectionReasons.FLOOR_TOO_BIG);
        }

        Set<BlockPos> fullFloorBlocks = Sets.newHashSet();
        fullFloorBlocks.addAll(floorBlockSet);
        fullFloorBlocks.addAll(floorPerimeterBlocks);

        List<String> floorErrors = BuildingDetectionUtils.checkValidBlocks(level, fullFloorBlocks,
                enclosedType.getFloorBlocks(), noSkip);
        if (!floorErrors.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_FLOOR, floorErrors);
        }

        BuildingDetectionUtils.detectInnerCorners(floorBlockSet, floorPerimeterBlocks);
        allBlocks.addAll(floorBlockSet);
        allBlocks.addAll(floorPerimeterBlocks);

        // Wall detection — no passthrough needed, doors and key blocks must be listed in wallBlocks
        Set<BlockPos> wallBlockSet = Sets.newHashSet();
        Set<BlockPos> roofCandidateBlocks = Sets.newHashSet();
        Predicate<BlockState> noPassthrough = bs -> false;
        boolean foundWalls = BuildingDetectionUtils.findWalls(level, floorPerimeterBlocks, wallBlockSet,
                roofCandidateBlocks, enclosedType.getWallBlocks(), noPassthrough,
                CftConfig.MAX_HOUSE_HEIGHT.get());
        if (!foundWalls || wallBlockSet.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_WALLS);
        }
        List<String> wallErrors = BuildingDetectionUtils.checkValidBlocks(level, wallBlockSet,
                enclosedType.getWallBlocks(), noSkip);
        if (!wallErrors.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_WALLS, wallErrors);
        }
        allBlocks.addAll(wallBlockSet);

        // Interior detection — key block and containers must be listed in interiorBlocks
        Set<BlockPos> interiorBlockSet = Sets.newHashSet();
        boolean foundInterior = BuildingDetectionUtils.findInterior(level, floorBlockSet, interiorBlockSet,
                roofCandidateBlocks, enclosedType.getInteriorBlocks(), fullFloorBlocks.size(),
                CftConfig.MAX_HOUSE_HEIGHT.get());
        if (!foundInterior || interiorBlockSet.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_INTERIOR);
        }
        List<String> interiorErrors = BuildingDetectionUtils.checkValidBlocks(level, interiorBlockSet,
                enclosedType.getInteriorBlocks(), noSkip);
        if (!interiorErrors.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_INTERIOR, interiorErrors);
        }
        allBlocks.addAll(interiorBlockSet);

        // Roof verification
        boolean foundRoof = BuildingDetectionUtils.verifyRoof(level, roofCandidateBlocks, enclosedType.getRoofBlocks());
        if (!foundRoof || roofCandidateBlocks.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_ROOF);
        }
        List<String> roofErrors = BuildingDetectionUtils.checkValidBlocks(level, roofCandidateBlocks,
                enclosedType.getRoofBlocks(), noSkip);
        if (!roofErrors.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_ROOF, roofErrors);
        }
        Set<BlockPos> roofBlockSet = Sets.newHashSet(roofCandidateBlocks);
        allBlocks.addAll(roofBlockSet);

        // Closure verification
        boolean closed = BuildingDetectionUtils.verifyClosure(interiorBlockSet, floorBlockSet, wallBlockSet, roofBlockSet);
        if (!closed) {
            return StructureDetectionResult.failure(StructureDetectionReasons.NO_CLOSURE);
        }

        // Container detection
        BlockPos containerPos = null;
        if (structureType.isRequiresContainer()) {
            containerPos = BuildingDetectionUtils.findContainer(level, fullFloorBlocks);
            if (containerPos == null) {
                return StructureDetectionResult.failure(StructureDetectionReasons.NO_CONTAINER);
            }
        }

        // Size check
        if (allBlocks.size() > CftConfig.MAX_HOUSE_SIZE.get()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.STRUCTURE_TOO_LARGE);
        }

        // Build block position groups
        Map<String, List<BlockPos>> blockPositions = new HashMap<>();
        blockPositions.put("floor", new ArrayList<>(fullFloorBlocks));
        blockPositions.put("wall", new ArrayList<>(wallBlockSet));
        blockPositions.put("interior", new ArrayList<>(interiorBlockSet));
        blockPositions.put("roof", new ArrayList<>(roofBlockSet));

        Structure structure = new Structure(
                keyBlockPos, containerPos, allBlocks.size(), leaderId,
                structureType.getId(), structureType.getMaxUsers(), blockPositions
        );

        return StructureDetectionResult.success(structure);
    }
}
