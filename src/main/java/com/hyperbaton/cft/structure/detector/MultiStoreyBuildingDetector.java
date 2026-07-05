package com.hyperbaton.cft.structure.detector;

import com.google.common.collect.Sets;
import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.structure.*;
import com.hyperbaton.cft.structure.type.MultiStoreyBuildingStructureType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Detects a building of stacked enclosed-building storeys.
 *
 * Storey 1 is detected exactly like an enclosed building, flood-filling the floor from
 * below the key block. The floor of each subsequent storey is never flood-filled:
 * its extent is derived from the ceiling of the storey below, either as the very same
 * layer (shared: the lower ceiling doubles as the upper floor) or shifted one block up
 * (separate ceiling and floor layers). The shared interpretation is tried first.
 * Stacking stops at the first storey that fits neither way; detection succeeds if at
 * least min_storeys were found, and never looks beyond max_storeys.
 *
 * Holes for stairs/ladders between storeys are a datapack concern: connection blocks
 * (ladders, trapdoors...) must be listed in the lower storey's roofBlocks and, for
 * shared layers, in the upper storey's floorBlocks.
 */
public class MultiStoreyBuildingDetector implements StructureDetector {

    private static final Predicate<BlockState> NO_SKIP = bs -> false;

    /** Result of detecting the phases of one storey above an already-known floor. */
    private record StoreyParts(Set<BlockPos> wallBlocks, Set<BlockPos> interiorBlocks,
                               Set<BlockPos> roofBlocks, StructureDetectionReasons failure,
                               List<String> failureDetails) {
        static StoreyParts failure(StructureDetectionReasons reason, List<String> details) {
            return new StoreyParts(null, null, null, reason, details);
        }

        boolean failed() {
            return failure != null;
        }
    }

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId,
                                           StructureType structureType) {
        if (!(structureType instanceof MultiStoreyBuildingStructureType multiType)) {
            throw new IllegalArgumentException("MultiStoreyBuildingDetector requires MultiStoreyBuildingStructureType");
        }

        Map<String, List<BlockPos>> blockPositions = new HashMap<>();
        Set<BlockPos> allBlocks = Sets.newHashSet();
        Set<BlockPos> allFullFloors = Sets.newHashSet();

        // ---- Storey 1: detected like a plain enclosed building ----
        StoreyRule rule = multiType.getRuleForStorey(1);
        Set<BlockPos> floorBlockSet = Sets.newHashSet();
        Set<BlockPos> floorPerimeterBlocks = Sets.newHashSet();
        boolean foundFloor = BuildingDetectionUtils.findFloor(level, keyBlockPos.below(), floorBlockSet,
                floorPerimeterBlocks, rule.floorBlocks(), CftConfig.MAX_FLOOR_SIZE.get());
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
                rule.floorBlocks(), NO_SKIP);
        if (!floorErrors.isEmpty()) {
            return StructureDetectionResult.failure(StructureDetectionReasons.INVALID_FLOOR,
                    storeyDetails(1, StructureDetectionReasons.INVALID_FLOOR, floorErrors));
        }
        BuildingDetectionUtils.detectInnerCorners(floorBlockSet, floorPerimeterBlocks);

        StoreyParts parts = detectStoreyAboveFloor(level, rule, floorBlockSet, floorPerimeterBlocks,
                fullFloorBlocks.size());
        if (parts.failed()) {
            return StructureDetectionResult.failure(parts.failure(),
                    storeyDetails(1, parts.failure(), parts.failureDetails()));
        }

        int storeyCount = 1;
        addStoreyGroups(blockPositions, storeyCount, fullFloorBlocks, parts);
        allBlocks.addAll(fullFloorBlocks);
        allFullFloors.addAll(fullFloorBlocks);
        addStoreyBlocks(allBlocks, parts);

        Set<BlockPos> previousCeiling = parts.roofBlocks();
        StructureDetectionReasons lastFailure = null;
        List<String> lastFailureDetails = List.of();

        // ---- Upper storeys: floors derived from the ceiling below, no flood fill ----
        while (storeyCount < multiType.getMaxStoreys()) {
            int storey = storeyCount + 1;
            rule = multiType.getRuleForStorey(storey);

            if (!BuildingDetectionUtils.isSingleYLayer(previousCeiling)) {
                lastFailure = StructureDetectionReasons.CEILING_NOT_FLAT;
                lastFailureDetails = storeyDetails(storeyCount, StructureDetectionReasons.CEILING_NOT_FLAT, List.of());
                break;
            }

            // Shared layer first: the previous ceiling doubles as this storey's floor.
            // If it doesn't validate, try one block above (separate floor layer).
            Set<BlockPos> floorRegion = previousCeiling;
            if (!allValid(level, floorRegion, rule.floorBlocks())) {
                floorRegion = previousCeiling.stream().map(BlockPos::above).collect(Collectors.toSet());
                if (!allValid(level, floorRegion, rule.floorBlocks())) {
                    lastFailure = StructureDetectionReasons.INVALID_FLOOR;
                    lastFailureDetails = List.of(String.format("Storey %d: floor matches neither the shared "
                            + "layer nor the layer above the previous ceiling", storey));
                    break;
                }
            }

            floorBlockSet = Sets.newHashSet();
            floorPerimeterBlocks = Sets.newHashSet();
            if (!BuildingDetectionUtils.partitionFloorRegion(floorRegion, floorBlockSet, floorPerimeterBlocks)) {
                lastFailure = StructureDetectionReasons.INVALID_FLOOR;
                lastFailureDetails = List.of(String.format("Storey %d: floor region is degenerate", storey));
                break;
            }
            List<String> upperFloorErrors = BuildingDetectionUtils.checkValidBlocks(level, floorRegion,
                    rule.floorBlocks(), NO_SKIP);
            if (!upperFloorErrors.isEmpty()) {
                lastFailure = StructureDetectionReasons.INVALID_FLOOR;
                lastFailureDetails = storeyDetails(storey, StructureDetectionReasons.INVALID_FLOOR, upperFloorErrors);
                break;
            }
            BuildingDetectionUtils.detectInnerCorners(floorBlockSet, floorPerimeterBlocks);

            parts = detectStoreyAboveFloor(level, rule, floorBlockSet, floorPerimeterBlocks, floorRegion.size());
            if (parts.failed()) {
                lastFailure = parts.failure();
                lastFailureDetails = storeyDetails(storey, parts.failure(), parts.failureDetails());
                break;
            }

            storeyCount = storey;
            addStoreyGroups(blockPositions, storeyCount, floorRegion, parts);
            allBlocks.addAll(floorRegion);
            allFullFloors.addAll(floorRegion);
            addStoreyBlocks(allBlocks, parts);
            previousCeiling = parts.roofBlocks();
        }

        if (storeyCount < multiType.getMinStoreys()) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Found %d storeys, but the minimum is %d",
                    storeyCount, multiType.getMinStoreys()));
            details.addAll(lastFailureDetails);
            return StructureDetectionResult.failure(StructureDetectionReasons.NOT_ENOUGH_STOREYS, details);
        }

        if (structureType.isRequiresContainer() && !BuildingDetectionUtils.hasContainers(level, allFullFloors)) {
            return StructureDetectionResult.failure(StructureDetectionReasons.NO_CONTAINER);
        }

        if (allBlocks.size() > (long) CftConfig.MAX_HOUSE_SIZE.get() * storeyCount) {
            return StructureDetectionResult.failure(StructureDetectionReasons.STRUCTURE_TOO_LARGE);
        }

        Structure structure = new Structure(
                keyBlockPos, allBlocks.size(), leaderId,
                structureType.getId(), structureType.getMaxUsers(), blockPositions
        );
        return StructureDetectionResult.success(structure);
    }

    /**
     * Runs the wall, interior, roof and closure phases of one storey whose floor
     * (inner + perimeter) is already known. Mirrors the phases of
     * EnclosedBuildingDetector without modifying it.
     */
    private StoreyParts detectStoreyAboveFloor(ServerLevel level, StoreyRule rule,
                                               Set<BlockPos> floorBlockSet, Set<BlockPos> floorPerimeterBlocks,
                                               int fullFloorSize) {
        Set<BlockPos> wallBlockSet = Sets.newHashSet();
        Set<BlockPos> roofCandidateBlocks = Sets.newHashSet();
        boolean foundWalls = BuildingDetectionUtils.findWalls(level, floorPerimeterBlocks, wallBlockSet,
                roofCandidateBlocks, rule.wallBlocks(), NO_SKIP, CftConfig.MAX_HOUSE_HEIGHT.get());
        if (!foundWalls || wallBlockSet.isEmpty()) {
            return StoreyParts.failure(StructureDetectionReasons.INVALID_WALLS, List.of());
        }
        List<String> wallErrors = BuildingDetectionUtils.checkValidBlocks(level, wallBlockSet,
                rule.wallBlocks(), NO_SKIP);
        if (!wallErrors.isEmpty()) {
            return StoreyParts.failure(StructureDetectionReasons.INVALID_WALLS, wallErrors);
        }

        // If the wall tops form a flat ceiling, cap the interior scan at that level.
        // Otherwise connection holes (ladders, trapdoors...) would let the scan climb
        // through the ceiling into the storey above, misclassifying the ceiling layer
        // of the hole column as interior instead of roof.
        int interiorMaxHeight = CftConfig.MAX_HOUSE_HEIGHT.get();
        if (BuildingDetectionUtils.isSingleYLayer(roofCandidateBlocks) && !roofCandidateBlocks.isEmpty()) {
            interiorMaxHeight = roofCandidateBlocks.iterator().next().getY();
        }

        Set<BlockPos> interiorBlockSet = Sets.newHashSet();
        boolean foundInterior = BuildingDetectionUtils.findInterior(level, floorBlockSet, interiorBlockSet,
                roofCandidateBlocks, rule.interiorBlocks(), fullFloorSize, interiorMaxHeight);
        if (!foundInterior || interiorBlockSet.isEmpty()) {
            return StoreyParts.failure(StructureDetectionReasons.INVALID_INTERIOR, List.of());
        }
        List<String> interiorErrors = BuildingDetectionUtils.checkValidBlocks(level, interiorBlockSet,
                rule.interiorBlocks(), NO_SKIP);
        if (!interiorErrors.isEmpty()) {
            return StoreyParts.failure(StructureDetectionReasons.INVALID_INTERIOR, interiorErrors);
        }

        boolean foundRoof = BuildingDetectionUtils.verifyRoof(level, roofCandidateBlocks, rule.roofBlocks());
        if (!foundRoof || roofCandidateBlocks.isEmpty()) {
            return StoreyParts.failure(StructureDetectionReasons.INVALID_ROOF, List.of());
        }
        List<String> roofErrors = BuildingDetectionUtils.checkValidBlocks(level, roofCandidateBlocks,
                rule.roofBlocks(), NO_SKIP);
        if (!roofErrors.isEmpty()) {
            return StoreyParts.failure(StructureDetectionReasons.INVALID_ROOF, roofErrors);
        }

        Set<BlockPos> roofBlockSet = Sets.newHashSet(roofCandidateBlocks);
        boolean closed = BuildingDetectionUtils.verifyClosure(interiorBlockSet, floorBlockSet,
                wallBlockSet, roofBlockSet);
        if (!closed) {
            return StoreyParts.failure(StructureDetectionReasons.NO_CLOSURE, List.of());
        }

        return new StoreyParts(wallBlockSet, interiorBlockSet, roofBlockSet, null, List.of());
    }

    /**
     * Prefixes failure details with the storey they belong to, so the player knows
     * which floor of the building failed validation.
     */
    private static List<String> storeyDetails(int storey, StructureDetectionReasons reason, List<String> details) {
        if (details.isEmpty()) {
            return List.of("Storey " + storey + ": " + reason.getMessage());
        }
        return details.stream().map(detail -> "Storey " + storey + ": " + detail).toList();
    }

    private boolean allValid(ServerLevel level, Set<BlockPos> region, List<ValidBlock> validBlocks) {
        return region.stream()
                .allMatch(pos -> BuildingDetectionUtils.isValidBlock(level.getBlockState(pos), validBlocks));
    }

    private void addStoreyGroups(Map<String, List<BlockPos>> blockPositions, int storey,
                                 Set<BlockPos> fullFloorBlocks, StoreyParts parts) {
        blockPositions.put(EnclosedBuildingBlockGroup.FLOOR.getKey() + "_" + storey, new ArrayList<>(fullFloorBlocks));
        blockPositions.put(EnclosedBuildingBlockGroup.WALL.getKey() + "_" + storey, new ArrayList<>(parts.wallBlocks()));
        blockPositions.put(EnclosedBuildingBlockGroup.INTERIOR.getKey() + "_" + storey, new ArrayList<>(parts.interiorBlocks()));
        blockPositions.put(EnclosedBuildingBlockGroup.ROOF.getKey() + "_" + storey, new ArrayList<>(parts.roofBlocks()));
    }

    private void addStoreyBlocks(Set<BlockPos> allBlocks, StoreyParts parts) {
        allBlocks.addAll(parts.wallBlocks());
        allBlocks.addAll(parts.interiorBlocks());
        allBlocks.addAll(parts.roofBlocks());
    }
}
