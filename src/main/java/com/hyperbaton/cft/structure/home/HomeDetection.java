package com.hyperbaton.cft.structure.home;

import com.google.common.collect.Sets;
import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.HomeNeed;
import com.hyperbaton.cft.need.HomeValidBlock;
import com.hyperbaton.cft.network.HomeDetectionPacket;
import com.hyperbaton.cft.world.HomesData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import oshi.util.tuples.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HomeDetection {
    private static final Logger LOGGER = LogUtils.getLogger();

    public HomeDetectionPacket detectAnyHouse(BlockPos positionClicked, ServerLevel level, UUID leaderId) {
        List<HomeNeed> homeNeeds = CftRegistry.NEEDS.stream()
                .filter(need -> need instanceof HomeNeed)
                .map(need -> (HomeNeed) need)
                .toList();
        List<HomeDetectionPacket> detectedHomePackets = new ArrayList<>();
        for (HomeNeed homeNeed : homeNeeds) {
            HomeDetectionPacket detectedHomePacket = detectHouse(positionClicked, level, leaderId, homeNeed);
            detectedHomePackets.add(detectedHomePacket);
            if (detectedHomePacket.isHomeDetected()) break;
        }
        return selectDetectionPacket(detectedHomePackets);
    }

    public static HomeDetectionPacket detectHouse(BlockPos entrance, ServerLevel level, UUID leaderId, HomeNeed homeNeed) {
        Set<BlockPos> houseBlocks = Sets.newHashSet(entrance);
        Set<BlockPos> floorBlocks = Sets.newHashSet();
        Set<BlockPos> floorPerimeterBlocks = Sets.newHashSet();
        Set<BlockPos> fullFloorBlocks = Sets.newHashSet();

        // Detect the floor of the house
        boolean foundFloor = findFloor(level, entrance.below(), floorBlocks, floorPerimeterBlocks,
                homeNeed.getFloorBlocks());
        if (floorBlocks.isEmpty()) {
            return houseNotFound(HomeDetectionReasons.NO_FLOOR, entrance, level, homeNeed.getId(), Collections.emptyList());
        }
        if (!foundFloor) {
            return houseNotFound(HomeDetectionReasons.FLOOR_TOO_BIG, entrance, level, homeNeed.getId(), Collections.emptyList());
        }
        List<String> floorErrors = checkValidBlocks(level,
                Stream.of(floorBlocks, floorPerimeterBlocks).flatMap(Collection::stream).collect(Collectors.toSet()),
                homeNeed.getFloorBlocks());
        if (!floorErrors.isEmpty()) {
            return houseNotFound(HomeDetectionReasons.INVALID_FLOOR, entrance, level, homeNeed.getId(), Collections.emptyList());
        }
        // The inner corners are misidentified as non perimeter blocks in the previous method.
        detectInnerCorners(level, floorBlocks, floorPerimeterBlocks);
        fullFloorBlocks.addAll(floorBlocks);
        fullFloorBlocks.addAll(floorPerimeterBlocks);
        houseBlocks.addAll(fullFloorBlocks);

        // Detect the walls of the house
        Set<BlockPos> wallBlocks = Sets.newHashSet();
        Set<BlockPos> roofCandidateBlocks = Sets.newHashSet();
        boolean foundWall = findWall(level, floorPerimeterBlocks, wallBlocks, roofCandidateBlocks,
                homeNeed.getWallBlocks());
        if (!foundWall || wallBlocks.isEmpty()) {
            return houseNotFound(HomeDetectionReasons.INVALID_WALLS, entrance, level, homeNeed.getId(), Collections.emptyList());
        }
        List<String> wallErrors = checkValidBlocks(level, wallBlocks, homeNeed.getWallBlocks());
        if (!wallErrors.isEmpty()) {
            return houseNotFound(HomeDetectionReasons.INVALID_WALLS, entrance, level, homeNeed.getId(), wallErrors);
        }
        if (tooManyDoors(level, wallBlocks)) {
            return houseNotFound(HomeDetectionReasons.TOO_MANY_DOORS, entrance, level, homeNeed.getId(), Collections.emptyList());
        }
        houseBlocks.addAll(wallBlocks);

        // Detect the indoor area of the house
        Set<BlockPos> interiorBlocks = Sets.newHashSet();
        boolean foundInterior = findInterior(level, floorBlocks, interiorBlocks, roofCandidateBlocks,
                homeNeed.getInteriorBlocks(), fullFloorBlocks.size());
        if (!foundInterior || interiorBlocks.isEmpty()) {
            return houseNotFound(HomeDetectionReasons.INVALID_INTERIOR, entrance, level, homeNeed.getId(), Collections.emptyList());
        }
        List<String> interiorErrors = checkValidBlocks(level, interiorBlocks, homeNeed.getInteriorBlocks());
        if (!interiorErrors.isEmpty()) {
            return houseNotFound(HomeDetectionReasons.INVALID_INTERIOR, entrance, level, homeNeed.getId(), interiorErrors);
        }
        houseBlocks.addAll(interiorBlocks);

        // Verify the detected roof of the house is valid
        Set<BlockPos> roofBlocks = Sets.newHashSet();
        boolean foundRoof = verifyRoofCandidates(level, roofCandidateBlocks, homeNeed.getRoofBlocks());
        if (!foundRoof || roofCandidateBlocks.isEmpty()) {
            return houseNotFound(HomeDetectionReasons.INVALID_ROOF, entrance, level, homeNeed.getId(), Collections.emptyList());
        }
        List<String> roofErrors = checkValidBlocks(level, roofBlocks, homeNeed.getRoofBlocks());
        if (!roofErrors.isEmpty()) {
            return houseNotFound(HomeDetectionReasons.INVALID_ROOF, entrance, level, homeNeed.getId(), roofErrors);
        }
        roofBlocks.addAll(roofCandidateBlocks);
        houseBlocks.addAll(roofBlocks);

        // Verify there are no gaps that expose the interior of the house
        boolean checkClosure = verifyClosure(interiorBlocks, floorBlocks, wallBlocks, roofBlocks);

        if (!checkClosure) {
            return houseNotFound(HomeDetectionReasons.NO_CLOSURE, entrance, level, homeNeed.getId(), Collections.emptyList());
        }

        // Detect the container for supplying the house
        BlockPos containerPos = findContainer(level, floorBlocks);

        if (containerPos == null) {
            return houseNotFound(HomeDetectionReasons.NO_CONTAINER, entrance, level, homeNeed.getId(), Collections.emptyList());
        }

        if (houseBlocks.size() > CftConfig.MAX_HOUSE_SIZE.get()) {
            return houseNotFound(HomeDetectionReasons.HOUSE_TOO_LARGE, entrance, level, homeNeed.getId(), Collections.emptyList());
        }

        // At this point, a house has been found
        // DEBUG MODE: Send particles
        //houseBlocks.forEach(blockPos -> level.sendParticles(ParticleTypes.ENTITY_EFFECT, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
        //        5, 0, 0, 0, 0.1));
        HomesData homesData = level.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
        // If the home doesn't exist yet, add it to the list
        if (homesData.getHomes().stream().noneMatch(home -> home.getEntrance().equals(entrance))) {
            homesData.addHome(new XoonglinHome(entrance, containerPos, houseBlocks.size(), leaderId, null, homeNeed.getId(),
                    floorBlocks.stream().toList(),
                    wallBlocks.stream().toList(),
                    interiorBlocks.stream().toList(),
                    roofBlocks.stream().toList()));
        }

        LOGGER.debug("House size:" + houseBlocks.size());

        return new HomeDetectionPacket(true, homeNeed.getId(), HomeDetectionReasons.HOUSE_DETECTED, Collections.emptyList());
    }

    private static boolean verifyClosure(Set<BlockPos> interiorBlocks, Set<BlockPos> floorBlocks,
                                         Set<BlockPos> wallBlocks, Set<BlockPos> roofBlocks) {
        return interiorBlocks.stream()
                .allMatch(interiorBlock -> isBlockInside(interiorBlock, interiorBlocks, floorBlocks, wallBlocks, roofBlocks));
    }

    private static boolean isBlockInside(BlockPos interiorBlock, Set<BlockPos> interiorBlocks, Set<BlockPos> floorBlocks,
                                         Set<BlockPos> wallBlocks, Set<BlockPos> roofBlocks) {
        return topIsInside(interiorBlock.above(), interiorBlocks, roofBlocks)
                && sideIsInside(interiorBlock.north(), interiorBlocks, wallBlocks, roofBlocks)
                && sideIsInside(interiorBlock.east(), interiorBlocks, wallBlocks, roofBlocks)
                && sideIsInside(interiorBlock.south(), interiorBlocks, wallBlocks, roofBlocks)
                && sideIsInside(interiorBlock.west(), interiorBlocks, wallBlocks, roofBlocks)
                && bottomIsInside(interiorBlock.below(), interiorBlocks, floorBlocks);
    }

    private static boolean bottomIsInside(BlockPos below, Set<BlockPos> interiorBlocks, Set<BlockPos> floorBlocks) {
        return interiorBlocks.contains(below) || floorBlocks.contains(below);
    }

    private static boolean sideIsInside(BlockPos east, Set<BlockPos> interiorBlocks, Set<BlockPos> wallBlocks, Set<BlockPos> roofBlocks) {
        return interiorBlocks.contains(east)
                || wallBlocks.contains(east)
                || roofBlocks.contains(east);
    }

    private static boolean topIsInside(BlockPos above, Set<BlockPos> interiorBlocks, Set<BlockPos> roofBlocks) {
        return interiorBlocks.contains(above) || roofBlocks.contains(above);
    }

    private static List<String> checkValidBlocks(ServerLevel level, Set<BlockPos> blockList, List<HomeValidBlock> validBlocks) {
        return blockList.stream()
                .map(level::getBlockState)
                .collect(Collectors.groupingBy(blockState -> blockState.getBlock().getDescriptionId()))
                .values().stream()
                .map(blocks -> new Pair<>(blocks.stream().findFirst(), blocks.size()))
                .flatMap(blockEntry -> satisfiesValidityConditions(blockEntry, validBlocks, blockList.size()).stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static List<String> satisfiesValidityConditions(Pair<Optional<BlockState>, Integer> blockEntry, List<HomeValidBlock> validBlocks, int size) {
        return validBlocks.stream()
                .filter(validBlock -> blockEntry.getA().isPresent() && isValidBlock(blockEntry.getA().get(), validBlock))
                .map(validBlock -> {
                            if (validBlock.getMinQuantity() <= blockEntry.getB()) {
                                return "There are " + validBlock.getMinQuantity() + " " + blockEntry.getA().get().getBlock().getDescriptionId() + " on the floor but the minimum required is " + validBlock.getMinQuantity() + ". ";
                            }
                            if (validBlock.getMaxQuantity() >= blockEntry.getB()) {
                                return "There are " + validBlock.getMaxQuantity() + " " + blockEntry.getA().get().getBlock().getDescriptionId() + " on the floor but the maximum allowed is " + validBlock.getMaxQuantity() + ". ";
                            }
                            double blockPercentage = BigDecimal.valueOf(blockEntry.getB().doubleValue())
                                    .divide(BigDecimal.valueOf(size), 2, RoundingMode.HALF_UP)
                                    .doubleValue();
                            if (validBlock.getMinPercentage() <= blockPercentage) {
                                return "There are " + blockPercentage + "% " + blockEntry.getA().get().getBlock().getDescriptionId() + " on the floor but the minimum required is " + validBlock.getMinPercentage() + ". ";
                            }
                            if (validBlock.getMaxPercentage() >= blockPercentage) {
                                return "There are " + blockPercentage + "% " + blockEntry.getA().get().getBlock().getDescriptionId() + " on the floor but the maximum allowed is " + validBlock.getMaxPercentage() + ". ";
                            }
                            return null;
                        }
                )
                .filter(Objects::isNull)
                .toList();
    }

    private static BlockPos findContainer(ServerLevel level, Set<BlockPos> floorBlocks) {
        Set<BlockPos> containers = Sets.newHashSet();
        for (BlockPos pos : floorBlocks) {
            if (isContainer(level.getBlockState(pos.above()))) {
                containers.add(pos.above());
            }
        }
        if (containers.size() == 1) {
            return containers.stream().findFirst().orElse(null);
        }
        return null;
    }

    private static void detectInnerCorners(Level level, Set<BlockPos> floorBlocks, Set<BlockPos> floorPerimeterBlocks) {
        Set<BlockPos> innerCorners = Sets.newHashSet();
        floorBlocks.forEach(floorBlockPos -> {
            int perimeterNeighbours = countPerimeterExtendedNeighbours(floorPerimeterBlocks, floorBlockPos);
            int exteriorNeighbours = countExteriorExtendedNeighbours(floorBlocks, floorPerimeterBlocks, floorBlockPos);
            if (perimeterNeighbours >= 2 && exteriorNeighbours == 1) {
                innerCorners.add(floorBlockPos);
            }
        });
        floorPerimeterBlocks.addAll(innerCorners);
        floorBlocks.removeAll(innerCorners);
    }

    private static int countExteriorExtendedNeighbours(Set<BlockPos> floorBlocks, Set<BlockPos> floorPerimeterBlocks, BlockPos testPos) {
        int count = 0;
        Set<BlockPos> allFloorBlocks = Sets.newHashSet();
        allFloorBlocks.addAll(floorBlocks);
        allFloorBlocks.addAll(floorPerimeterBlocks);
        if (!allFloorBlocks.contains(testPos.north())) {
            count++;
        }
        if (!allFloorBlocks.contains(testPos.south())) {
            count++;
        }
        if (!allFloorBlocks.contains(testPos.west())) {
            count++;
        }
        if (!allFloorBlocks.contains(testPos.east())) {
            count++;
        }
        if (!allFloorBlocks.contains(testPos.north().east())) {
            count++;
        }
        if (!allFloorBlocks.contains(testPos.south().west())) {
            count++;
        }
        if (!allFloorBlocks.contains(testPos.west().north())) {
            count++;
        }
        if (!allFloorBlocks.contains(testPos.east().south())) {
            count++;
        }
        return count;
    }

    private static int countPerimeterExtendedNeighbours(Set<BlockPos> floorPerimeterBlocks, BlockPos testPos) {
        int count = 0;
        if (floorPerimeterBlocks.contains(testPos.north())) {
            count++;
        }
        if (floorPerimeterBlocks.contains(testPos.south())) {
            count++;
        }
        if (floorPerimeterBlocks.contains(testPos.west())) {
            count++;
        }
        if (floorPerimeterBlocks.contains(testPos.east())) {
            count++;
        }
        if (floorPerimeterBlocks.contains(testPos.north().east())) {
            count++;
        }
        if (floorPerimeterBlocks.contains(testPos.south().west())) {
            count++;
        }
        if (floorPerimeterBlocks.contains(testPos.west().north())) {
            count++;
        }
        if (floorPerimeterBlocks.contains(testPos.east().south())) {
            count++;
        }
        return count;
    }

    private static boolean verifyRoofCandidates(Level level, Set<BlockPos> roofCandidatesBlocks, List<HomeValidBlock> validFloorBlocks) {
        return roofCandidatesBlocks.stream().allMatch(roofBlock -> isRoof(level.getBlockState(roofBlock), validFloorBlocks));
    }

    private static boolean findInterior(Level level, Set<BlockPos> floorBlocks, Set<BlockPos> interiorBlocks, Set<BlockPos> roofCandidateBlocks, List<HomeValidBlock> validBlocks, int fullFloorSize) {
        floorBlocks.forEach(floorPos -> {
            BlockPos testPos = floorPos.above();
            while (isInterior(level.getBlockState(testPos), validBlocks) && testPos.getY() < CftConfig.MAX_HOUSE_HEIGHT.get()) {
                interiorBlocks.add(testPos);
                testPos = testPos.above();
            }
            roofCandidateBlocks.add(testPos);
        });
        return fullFloorSize == roofCandidateBlocks.size();
    }

    private static boolean findWall(Level level, Set<BlockPos> floorPerimeterBlocks, Set<BlockPos> wallBlocks, Set<BlockPos> roofCandidateBlocks, List<HomeValidBlock> validBlocks) {
        floorPerimeterBlocks.forEach(perimeterBlockPos -> {
            BlockPos testPos = perimeterBlockPos.above();
            while (isWall(level.getBlockState(testPos), validBlocks) && testPos.getY() < CftConfig.MAX_HOUSE_HEIGHT.get()) {
                wallBlocks.add(testPos);
                testPos = testPos.above();
            }
            roofCandidateBlocks.add(testPos);
        });
        // Check that all wall pieces have the same size
        return floorPerimeterBlocks.size() == roofCandidateBlocks.size();
    }

    private static boolean findFloor(Level level, BlockPos testPos, Set<BlockPos> floorBlocks, Set<BlockPos> floorPerimeterBlocks, List<HomeValidBlock> validBlocks) {
        if (floorBlocks.size() + floorPerimeterBlocks.size() > CftConfig.MAX_FLOOR_SIZE.get()) {
            return false;
        }
        if (!isFloor(level.getBlockState(testPos), validBlocks)) {
            return true;
        }
        if (floorBlocks.contains(testPos) || floorPerimeterBlocks.contains(testPos)) {
            return true;
        }
        int floorNeighbours = countFloorNeighbours(level, testPos, validBlocks);
        switch (floorNeighbours) {
            case 1:
                return true;
            case 2:
            case 3:
                floorPerimeterBlocks.add(testPos);
                break;
            case 4:
                floorBlocks.add(testPos);
                break;
            default:
                return false;
        }
        return findFloor(level, testPos.north(), floorBlocks, floorPerimeterBlocks, validBlocks)
                && findFloor(level, testPos.south(), floorBlocks, floorPerimeterBlocks, validBlocks)
                && findFloor(level, testPos.west(), floorBlocks, floorPerimeterBlocks, validBlocks)
                && findFloor(level, testPos.east(), floorBlocks, floorPerimeterBlocks, validBlocks);
    }

    private static int countFloorNeighbours(Level level, BlockPos testPos, List<HomeValidBlock> validBlocks) {
        int count = 0;
        if (isFloor(level.getBlockState(testPos.north()), validBlocks)) {
            count++;
        }
        if (isFloor(level.getBlockState(testPos.south()), validBlocks)) {
            count++;
        }
        if (isFloor(level.getBlockState(testPos.east()), validBlocks)) {
            count++;
        }
        if (isFloor(level.getBlockState(testPos.west()), validBlocks)) {
            count++;
        }
        return count;
    }

    private static boolean isFloor(BlockState blockState, List<HomeValidBlock> validBlocks) {
        return validBlocks.stream().anyMatch(validBlock -> isValidBlock(blockState, validBlock));
    }

    private static boolean isInterior(BlockState blockState, List<HomeValidBlock> validBlocks) {
        return isContainer(blockState)
                || validBlocks.stream().anyMatch(validBlock -> isValidBlock(blockState, validBlock));
    }

    private static boolean isRoof(BlockState blockState, List<HomeValidBlock> validBlocks) {
        return validBlocks.stream().anyMatch(validBlock -> isValidBlock(blockState, validBlock));
    }

    private static boolean isWall(BlockState blockState, List<HomeValidBlock> validBlocks) {
        return blockState.is(BlockTags.DOORS)
                || validBlocks.stream().anyMatch(validBlock -> isValidBlock(blockState, validBlock));
    }

    private static boolean tooManyDoors(ServerLevel level, Set<BlockPos> wallBlocks) {
        return wallBlocks.stream()
                .filter(pos -> level.getBlockState(pos).is(BlockTags.DOORS))
                .limit(3) // A door occupies two blocks. If we find more than that, there is no correct home.
                .count() > 2;
    }

    private static boolean isValidBlock(BlockState blockState, HomeValidBlock validBlock) {
        if (validBlock.getTagBlock() != null) {
            return blockState.is(validBlock.getTagBlock());
        } else if (validBlock.getBlock() != null) {
            return blockState.is(validBlock.getBlock());
        } else {
            throw new IllegalArgumentException("HomeValidBlock doesn't have a valid block nor tag");
        }
    }

    private static boolean isContainer(BlockState blockState) {
        return blockState.getBlock() instanceof ChestBlock;
    }

    /**
     * Treatment of the case a house is not found: log the reason and remove it from the stored data
     */
    private static HomeDetectionPacket houseNotFound(HomeDetectionReasons reason, BlockPos entrance, ServerLevel level, String homeNeedId, List<String> validationDetails) {
        LOGGER.debug("No house found. " + reason);
        HomesData homesData = level.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
        Optional<XoonglinHome> homeToRemove = homesData.getHomes().stream().filter(home -> home.getEntrance().equals(entrance)).findFirst();
        if (homeToRemove.isPresent()) {
            homesData.getHomes().remove(homeToRemove.get());
            homesData.setDirty();
        }
        return new HomeDetectionPacket(false, homeNeedId, reason, validationDetails);
    }

    private HomeDetectionPacket selectDetectionPacket(List<HomeDetectionPacket> detectedHomePackets) {
        return detectedHomePackets.stream()
                .filter(HomeDetectionPacket::isHomeDetected) // Prioritize fully detected homes
                .findFirst() // If any home is fully detected, return it immediately
                .or(() -> detectedHomePackets.stream()
                        .filter(packet -> packet.getDetectionReason() != null) // Only consider those with an error
                        .max(Comparator.comparingInt(packet -> packet.getDetectionReason().ordinal())) // Pick the highest-priority error
                )
                .orElse(null); // If no valid detection is found, return null
    }

}
