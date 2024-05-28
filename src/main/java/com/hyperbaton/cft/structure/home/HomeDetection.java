package com.hyperbaton.cft.structure.home;

import com.google.common.collect.Sets;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.capability.need.HomeNeed;
import com.hyperbaton.cft.capability.need.HomeValidBlock;
import com.hyperbaton.cft.world.HomesData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import oshi.util.tuples.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HomeDetection {
    private static final int MAX_HOUSE_SIZE = 1000;
    private static final int MAX_FLOOR_SIZE = 100;
    private static final int MAX_HEIGHT = 319;
    private static final Logger LOGGER = LogUtils.getLogger();

    public boolean detectAnyHouse(BlockPos positionClicked, ServerLevel level, UUID leaderId) {
        List<HomeNeed> homeNeeds = CftRegistry.NEEDS.stream()
                .filter(need -> need instanceof HomeNeed)
                .map(need -> (HomeNeed) need)
                .toList();
        boolean detectedHome = false;
        for (HomeNeed homeNeed : homeNeeds) {
            detectedHome = detectHouse(positionClicked, level, leaderId, homeNeed);
            if (detectedHome) break;
        }
        return detectedHome;
    }

    public static boolean detectHouse(BlockPos entrance, ServerLevel level, UUID leaderId, HomeNeed homeNeed) {
        Set<BlockPos> houseBlocks = Sets.newHashSet(entrance);
        Set<BlockPos> floorBlocks = Sets.newHashSet();
        Set<BlockPos> floorPerimeterBlocks = Sets.newHashSet();
        Set<BlockPos> fullFloorBlocks = Sets.newHashSet();

        // Detect the floor of the house
        boolean foundFloor = findFloor(level, entrance.below(), floorBlocks, floorPerimeterBlocks,
                homeNeed.getFloorBlocks().stream().map(HomeValidBlock::getBlock).toList());
        if (floorBlocks.isEmpty()
                || !checkValidBlocks(level,
                Stream.of(floorBlocks, floorPerimeterBlocks).flatMap(Collection::stream).collect(Collectors.toSet()),
                homeNeed.getFloorBlocks())) {
            return houseNotFound("Invalid floor", entrance, level);
        }
        // The inner corners are misidentified as non perimeter blocks in the previous method.
        detectInnerCorners(level, floorBlocks, floorPerimeterBlocks);
        fullFloorBlocks.addAll(floorBlocks);
        fullFloorBlocks.addAll(floorPerimeterBlocks);
        houseBlocks.addAll(fullFloorBlocks);

        // Detect the walls of the house
        Set<BlockPos> wallBlocks = Sets.newHashSet();
        boolean foundWall = findWall(level, floorPerimeterBlocks, wallBlocks,
                homeNeed.getWallBlocks().stream().map(HomeValidBlock::getBlock).toList());
        if (!foundWall
                || !checkValidBlocks(level, wallBlocks, homeNeed.getWallBlocks())) {
            return houseNotFound("Invalid walls", entrance, level);
        }
        houseBlocks.addAll(wallBlocks);
        // Get the starting height of the roof
        int lowestRoofHeight = 1 + wallBlocks.stream().map(Vec3i::getY).distinct().max(Comparator.naturalOrder()).orElse(entrance.getY());

        // Detect the indoor area of the house
        Set<BlockPos> interiorBlocks = Sets.newHashSet();
        boolean foundInterior = findInterior(level, floorBlocks, interiorBlocks,
                homeNeed.getInteriorBlocks().stream().map(HomeValidBlock::getBlock).toList());
        if (!foundInterior
                || !checkValidBlocks(level, interiorBlocks, homeNeed.getInteriorBlocks())) {
            return houseNotFound("Invalid interior", entrance, level);
        }
        houseBlocks.addAll(interiorBlocks);

        // Detect the roof of the house
        Set<BlockPos> roofBlocks = Sets.newHashSet();
        boolean foundRoof = findRoof(level, fullFloorBlocks, roofBlocks, interiorBlocks, lowestRoofHeight,
                homeNeed.getRoofBlocks().stream().map(HomeValidBlock::getBlock).toList(),
                homeNeed.getInteriorBlocks().stream().map(HomeValidBlock::getBlock).toList());
        if (!foundRoof
                || !checkValidBlocks(level, roofBlocks, homeNeed.getRoofBlocks())) {
            return houseNotFound("Invalid roof", entrance, level);
        }
        houseBlocks.addAll(roofBlocks);

        // Detect the container for supplying the house
        BlockPos containerPos = findContainer(level, floorBlocks);

        if (containerPos == null) {
            return houseNotFound("No container present", entrance, level);
        }

        if (houseBlocks.size() > MAX_HOUSE_SIZE) {
            return houseNotFound("House too large", entrance, level);
        }

        // At this point, a house has been found
        houseBlocks.forEach(blockPos -> ((ServerLevel) level).sendParticles(ParticleTypes.ENTITY_EFFECT, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                5, 0, 0, 0, 0.1));
        HomesData homesData = level.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
        // If the home doesn't exist yet, add it to the list
        if (homesData.getHomes().stream().noneMatch(home -> home.getEntrance().equals(entrance))) {
            homesData.addHome(new XunguiHome(entrance, containerPos, houseBlocks.size(), leaderId, null, homeNeed.getId(),
                    floorBlocks.stream().toList(),
                    wallBlocks.stream().toList(),
                    interiorBlocks.stream().toList(),
                    roofBlocks.stream().toList()));
            homesData.setDirty();
        }

        LOGGER.debug("House size:" + houseBlocks.size());

        return true;
    }

    private static boolean checkValidBlocks(ServerLevel level, Set<BlockPos> blockList, List<HomeValidBlock> validBlocks) {
        return blockList.stream()
                .map(level::getBlockState)
                .map(BlockBehaviour.BlockStateBase::getBlock)
                .collect(Collectors.groupingBy(Block::getDescriptionId))
                .entrySet().stream()
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue().size()))
                .allMatch(blockEntry -> satisfiesValidityConditions(blockEntry, validBlocks, blockList.size()));
    }

    private static boolean satisfiesValidityConditions(Pair<String, Integer> blockEntry, List<HomeValidBlock> validBlocks, int size) {
        return validBlocks.stream()
                .filter(validBlock -> validBlock.getBlock().getDescriptionId().equals(blockEntry.getA()))
                .allMatch(validBlock -> validBlock.getMinQuantity() <= blockEntry.getB()
                        && validBlock.getMaxQuantity() >= blockEntry.getB()
                        && validBlock.getMinPercentage() <= BigDecimal.valueOf(blockEntry.getB().doubleValue()).divide(BigDecimal.valueOf(size), 2, RoundingMode.HALF_UP).doubleValue()
                        && validBlock.getMaxPercentage() >= BigDecimal.valueOf(blockEntry.getB().doubleValue()).divide(BigDecimal.valueOf(size), 2, RoundingMode.HALF_UP).doubleValue()
                );
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

    // TODO: Improve roofs. Allow for slanted roofs.
    private static boolean findRoof(Level level, Set<BlockPos> floorBlocks, Set<BlockPos> roofBlocks,  Set<BlockPos> interiorBlocks, int lowestRoofHeight, List<Block> validFloorBlocks, List<Block> validInteriorBlocks) {
        Set<BlockPos> testSetOfBlocks = floorBlocks.stream()
                .map(blockPos -> blockPos.relative(Direction.Axis.Y, lowestRoofHeight - blockPos.getY()))
                .collect(Collectors.toSet());
        AtomicBoolean wrongRoofFound = new AtomicBoolean(false);
        while (!testSetOfBlocks.isEmpty() && !wrongRoofFound.get()) {
            Set<BlockPos> nextTestSetOfBlocks = new HashSet<>();
            testSetOfBlocks.forEach(testPos -> {
                if(isRoof(level.getBlockState(testPos), validFloorBlocks) && testPos.getY() < MAX_HEIGHT) {
                    roofBlocks.add(testPos);
                } else if (isInterior(level.getBlockState(testPos), validInteriorBlocks) && testPos.getY() < MAX_HEIGHT) {
                    interiorBlocks.add(testPos);
                    nextTestSetOfBlocks.add(testPos.above());
                } else {
                    wrongRoofFound.set(true);
                }
            });
            testSetOfBlocks.clear();
            testSetOfBlocks.addAll(nextTestSetOfBlocks);
        }
        return !roofBlocks.isEmpty() && !wrongRoofFound.get();
    }

    private static boolean findInterior(Level level, Set<BlockPos> floorBlocks, Set<BlockPos> interiorBlocks, List<Block> validBlocks) {
        floorBlocks.forEach(floorPos -> {
            BlockPos testPos = floorPos.above();
            while (isInterior(level.getBlockState(testPos), validBlocks)) {
                interiorBlocks.add(testPos);
                if (testPos.getY() >= MAX_HEIGHT) { // TODO: This should say there is no house
                    break;
                } else {
                    testPos = testPos.above();
                }
            }
        });
        return true;
    }

    private static boolean findWall(Level level, Set<BlockPos> floorPerimeterBlocks, Set<BlockPos> wallBlocks, List<Block> validBlocks) {
        List<Integer> heights = Lists.newArrayList();
        floorPerimeterBlocks.forEach(perimeterBlockPos -> {
            BlockPos testPos = perimeterBlockPos.above();
            while (isWall(level.getBlockState(testPos), validBlocks)) {
                wallBlocks.add(testPos);
                testPos = testPos.above();
            }
            heights.add(testPos.below().getY());
        });
        // Check that all wall pieces have the same size
        return heights.stream().distinct().count() == 1;
    }

    private static boolean findFloor(Level level, BlockPos testPos, Set<BlockPos> floorBlocks, Set<BlockPos> floorPerimeterBlocks, List<Block> validBlocks) {
        if (floorBlocks.size() + floorPerimeterBlocks.size() > MAX_FLOOR_SIZE) {
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

    private static int countFloorNeighbours(Level level, BlockPos testPos, List<Block> validBlocks) {
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

    private static boolean isFloor(BlockState blockState, List<Block> validBlocks) {
        return validBlocks.contains(blockState.getBlock());
    }

    private static boolean isInterior(BlockState blockState, List<Block> validBlocks) {
        return validBlocks.contains(blockState.getBlock())
                || blockState.is(Blocks.CHEST);
    }

    private static boolean isRoof(BlockState blockState, List<Block> validBlocks) {
        return validBlocks.contains(blockState.getBlock());
    }

    private static boolean isWall(BlockState blockState, List<Block> validBlocks) {
        return blockState.is(BlockTags.DOORS)
                || validBlocks.contains(blockState.getBlock());
    }

    private static boolean isContainer(BlockState blockState) {
        return blockState.is(Blocks.CHEST);
    }

    /**
     * Treatment of the case a house is not found: log the reason and remove it from the stored data
     */
    private static boolean houseNotFound(String reason, BlockPos entrance, ServerLevel level) {
        LOGGER.debug("No house found. " + reason);
        HomesData homesData = level.getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
        Optional<XunguiHome> homeToRemove = homesData.getHomes().stream().filter(home -> home.getEntrance().equals(entrance)).findFirst();
        if (homeToRemove.isPresent()) {
            homesData.getHomes().remove(homeToRemove.get());
            homesData.setDirty();
        }
        return false;
    }
}
