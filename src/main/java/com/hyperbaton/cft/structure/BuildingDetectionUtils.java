package com.hyperbaton.cft.structure;

import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.hyperbaton.cft.CftConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import oshi.util.tuples.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuildingDetectionUtils {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean findFloor(Level level, BlockPos testPos, Set<BlockPos> floorBlocks,
                                    Set<BlockPos> floorPerimeterBlocks, List<ValidBlock> validBlocks,
                                    int maxFloorSize) {
        if (floorBlocks.size() + floorPerimeterBlocks.size() > maxFloorSize) {
            return false;
        }
        if (!isValidBlock(level.getBlockState(testPos), validBlocks)) {
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
        return findFloor(level, testPos.north(), floorBlocks, floorPerimeterBlocks, validBlocks, maxFloorSize)
                && findFloor(level, testPos.south(), floorBlocks, floorPerimeterBlocks, validBlocks, maxFloorSize)
                && findFloor(level, testPos.west(), floorBlocks, floorPerimeterBlocks, validBlocks, maxFloorSize)
                && findFloor(level, testPos.east(), floorBlocks, floorPerimeterBlocks, validBlocks, maxFloorSize);
    }

    public static void detectInnerCorners(Set<BlockPos> floorBlocks, Set<BlockPos> floorPerimeterBlocks) {
        Set<BlockPos> innerCorners = Sets.newHashSet();
        floorBlocks.forEach(floorBlockPos -> {
            int perimeterNeighbours = countExtendedNeighboursIn(floorPerimeterBlocks, floorBlockPos);
            Set<BlockPos> allFloor = Sets.newHashSet();
            allFloor.addAll(floorBlocks);
            allFloor.addAll(floorPerimeterBlocks);
            int exteriorNeighbours = countExteriorExtendedNeighbours(allFloor, floorBlockPos);
            if (perimeterNeighbours >= 2 && exteriorNeighbours == 1) {
                innerCorners.add(floorBlockPos);
            }
        });
        floorPerimeterBlocks.addAll(innerCorners);
        floorBlocks.removeAll(innerCorners);
    }

    public static boolean findWalls(Level level, Set<BlockPos> floorPerimeterBlocks, Set<BlockPos> wallBlocks,
                                    Set<BlockPos> roofCandidateBlocks, List<ValidBlock> validBlocks,
                                    Predicate<BlockState> passthrough, int maxHeight) {
        floorPerimeterBlocks.forEach(perimeterBlockPos -> {
            BlockPos testPos = perimeterBlockPos.above();
            while ((isWallBlock(level.getBlockState(testPos), validBlocks, passthrough)) && testPos.getY() < maxHeight) {
                wallBlocks.add(testPos);
                testPos = testPos.above();
            }
            roofCandidateBlocks.add(testPos);
        });
        return floorPerimeterBlocks.size() == roofCandidateBlocks.size();
    }

    public static boolean findInterior(Level level, Set<BlockPos> floorBlocks, Set<BlockPos> interiorBlocks,
                                       Set<BlockPos> roofCandidateBlocks, List<ValidBlock> validBlocks,
                                       int fullFloorSize, int maxHeight) {
        return findInterior(level, floorBlocks, interiorBlocks, roofCandidateBlocks, validBlocks,
                fullFloorSize, maxHeight, bs -> false);
    }

    public static boolean findInterior(Level level, Set<BlockPos> floorBlocks, Set<BlockPos> interiorBlocks,
                                       Set<BlockPos> roofCandidateBlocks, List<ValidBlock> validBlocks,
                                       int fullFloorSize, int maxHeight, Predicate<BlockState> passthrough) {
        floorBlocks.forEach(floorPos -> {
            BlockPos testPos = floorPos.above();
            while (isInteriorBlock(level.getBlockState(testPos), validBlocks, passthrough) && testPos.getY() < maxHeight) {
                interiorBlocks.add(testPos);
                testPos = testPos.above();
            }
            roofCandidateBlocks.add(testPos);
        });
        return fullFloorSize == roofCandidateBlocks.size();
    }

    /**
     * True if all positions share the same Y coordinate. Used by multi-storey
     * detection: a storey can only support another storey if its ceiling is flat.
     */
    public static boolean isSingleYLayer(Set<BlockPos> blocks) {
        return blocks.stream().mapToInt(BlockPos::getY).distinct().count() <= 1;
    }

    /**
     * Splits a known floor region into inner floor and perimeter blocks using the same
     * neighbour-count classification as findFloor, but based on region membership
     * instead of block validity. Used for the derived floors of upper storeys, whose
     * extent is fixed by the storey below (no flood fill needed).
     *
     * @return false if the region is degenerate (a block with fewer than 2 neighbours)
     */
    public static boolean partitionFloorRegion(Set<BlockPos> region, Set<BlockPos> floorBlocks,
                                               Set<BlockPos> floorPerimeterBlocks) {
        for (BlockPos pos : region) {
            int neighbours = 0;
            if (region.contains(pos.north())) neighbours++;
            if (region.contains(pos.south())) neighbours++;
            if (region.contains(pos.east())) neighbours++;
            if (region.contains(pos.west())) neighbours++;
            switch (neighbours) {
                case 2, 3 -> floorPerimeterBlocks.add(pos);
                case 4 -> floorBlocks.add(pos);
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean verifyRoof(Level level, Set<BlockPos> roofCandidateBlocks, List<ValidBlock> validBlocks) {
        return roofCandidateBlocks.stream()
                .allMatch(roofBlock -> isValidBlock(level.getBlockState(roofBlock), validBlocks));
    }

    public static boolean verifyClosure(Set<BlockPos> interiorBlocks, Set<BlockPos> floorBlocks,
                                        Set<BlockPos> wallBlocks, Set<BlockPos> roofBlocks) {
        return interiorBlocks.stream()
                .allMatch(block -> isBlockEnclosed(block, interiorBlocks, floorBlocks, wallBlocks, roofBlocks));
    }

    public static boolean hasContainers(ServerLevel level, Set<BlockPos> floorBlocks) {
        for (BlockPos pos : floorBlocks) {
            BlockPos above = pos.above();
            if (level.getBlockEntity(above) instanceof Container) {
                return true;
            }
        }
        return false;
    }

    public static List<String> checkValidBlocks(ServerLevel level, Set<BlockPos> blockList,
                                                List<ValidBlock> validBlocks,
                                                Predicate<BlockState> skipPredicate) {
        List<Pair<ValidBlock, Integer>> classifiedBlocks = blockList.stream()
                .map(level::getBlockState)
                .filter(blockState -> !skipPredicate.test(blockState))
                .collect(Collectors.groupingBy(
                        blockState -> validBlocks.stream()
                                .filter(validBlock -> isValidBlock(blockState, validBlock))
                                .findFirst()
                                // TODO: properly catch this and send message to player
                                .orElseThrow(() -> new IllegalStateException("Block not matching any ValidBlock"))
                ))
                .entrySet().stream()
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue().size()))
                .toList();

        List<Pair<ValidBlock, Integer>> notFoundValidBlocks = validBlocks.stream()
                .filter(validBlock -> classifiedBlocks.stream()
                        .map(Pair::getA)
                        .noneMatch(classified -> classified.equals(validBlock)))
                .map(validBlock -> new Pair<>(validBlock, 0))
                .toList();

        return Streams.concat(classifiedBlocks.stream(), notFoundValidBlocks.stream())
                .map(blockEntry -> satisfiesValidityConditions(blockEntry.getA(), blockEntry.getB(), blockList.size()))
                .filter(Objects::nonNull)
                .toList();
    }

    public static boolean isValidBlock(BlockState blockState, List<ValidBlock> validBlocks) {
        return validBlocks.stream().anyMatch(validBlock -> isValidBlock(blockState, validBlock));
    }

    public static boolean isValidBlock(BlockState blockState, ValidBlock validBlock) {
        if (validBlock.getTagBlock() != null) {
            return blockState.is(validBlock.getTagBlock());
        } else if (validBlock.getBlock() != null) {
            return blockState.is(validBlock.getBlock());
        } else {
            throw new IllegalArgumentException("ValidBlock doesn't have a valid block nor tag");
        }
    }

    private static boolean isWallBlock(BlockState blockState, List<ValidBlock> validBlocks,
                                       Predicate<BlockState> passthrough) {
        return passthrough.test(blockState) || isValidBlock(blockState, validBlocks);
    }

    private static boolean isInteriorBlock(BlockState blockState, List<ValidBlock> validBlocks,
                                              Predicate<BlockState> passthrough) {
        return passthrough.test(blockState) || isValidBlock(blockState, validBlocks);
    }

    private static boolean isBlockEnclosed(BlockPos block, Set<BlockPos> interiorBlocks,
                                           Set<BlockPos> floorBlocks, Set<BlockPos> wallBlocks,
                                           Set<BlockPos> roofBlocks) {
        return (interiorBlocks.contains(block.above()) || roofBlocks.contains(block.above()))
                && (interiorBlocks.contains(block.below()) || floorBlocks.contains(block.below()))
                && sideEnclosed(block.north(), interiorBlocks, wallBlocks, roofBlocks)
                && sideEnclosed(block.east(), interiorBlocks, wallBlocks, roofBlocks)
                && sideEnclosed(block.south(), interiorBlocks, wallBlocks, roofBlocks)
                && sideEnclosed(block.west(), interiorBlocks, wallBlocks, roofBlocks);
    }

    private static boolean sideEnclosed(BlockPos pos, Set<BlockPos> interiorBlocks,
                                        Set<BlockPos> wallBlocks, Set<BlockPos> roofBlocks) {
        return interiorBlocks.contains(pos) || wallBlocks.contains(pos) || roofBlocks.contains(pos);
    }

    private static int countFloorNeighbours(Level level, BlockPos testPos, List<ValidBlock> validBlocks) {
        int count = 0;
        if (isValidBlock(level.getBlockState(testPos.north()), validBlocks)) count++;
        if (isValidBlock(level.getBlockState(testPos.south()), validBlocks)) count++;
        if (isValidBlock(level.getBlockState(testPos.east()), validBlocks)) count++;
        if (isValidBlock(level.getBlockState(testPos.west()), validBlocks)) count++;
        return count;
    }

    private static int countExtendedNeighboursIn(Set<BlockPos> blocks, BlockPos testPos) {
        int count = 0;
        if (blocks.contains(testPos.north())) count++;
        if (blocks.contains(testPos.south())) count++;
        if (blocks.contains(testPos.west())) count++;
        if (blocks.contains(testPos.east())) count++;
        if (blocks.contains(testPos.north().east())) count++;
        if (blocks.contains(testPos.south().west())) count++;
        if (blocks.contains(testPos.west().north())) count++;
        if (blocks.contains(testPos.east().south())) count++;
        return count;
    }

    private static int countExteriorExtendedNeighbours(Set<BlockPos> allFloorBlocks, BlockPos testPos) {
        int count = 0;
        if (!allFloorBlocks.contains(testPos.north())) count++;
        if (!allFloorBlocks.contains(testPos.south())) count++;
        if (!allFloorBlocks.contains(testPos.west())) count++;
        if (!allFloorBlocks.contains(testPos.east())) count++;
        if (!allFloorBlocks.contains(testPos.north().east())) count++;
        if (!allFloorBlocks.contains(testPos.south().west())) count++;
        if (!allFloorBlocks.contains(testPos.west().north())) count++;
        if (!allFloorBlocks.contains(testPos.east().south())) count++;
        return count;
    }

    private static String satisfiesValidityConditions(ValidBlock validBlock, int subsetSize, int totalSize) {
        if (subsetSize < validBlock.getMinQuantity()) {
            return String.format("Found %d blocks of type %s, but the minimum required is %d",
                    subsetSize, getBlockDescription(validBlock), validBlock.getMinQuantity());
        }
        if (subsetSize > validBlock.getMaxQuantity()) {
            return String.format("Found %d blocks of type %s, but the maximum allowed is %d",
                    subsetSize, getBlockDescription(validBlock), validBlock.getMaxQuantity());
        }
        double blockPercentage = totalSize == 0 ? 0.0 : BigDecimal.valueOf(subsetSize)
                .divide(BigDecimal.valueOf(totalSize), 2, RoundingMode.HALF_UP)
                .doubleValue();
        if (blockPercentage < validBlock.getMinPercentage()) {
            return String.format("Found %.0f%% of blocks of type %s, but the minimum required is %.0f%%",
                    blockPercentage * 100, getBlockDescription(validBlock), validBlock.getMinPercentage() * 100);
        }
        if (blockPercentage > validBlock.getMaxPercentage()) {
            return String.format("Found %.0f%% of blocks of type %s, but the maximum allowed is %.0f%%",
                    blockPercentage * 100, getBlockDescription(validBlock), validBlock.getMaxPercentage() * 100);
        }
        return null;
    }

    private static String getBlockDescription(ValidBlock validBlock) {
        if (validBlock.getBlock() != null) {
            return Component.translatable(validBlock.getBlock().getDescriptionId()).getString();
        }
        return "#" + validBlock.getTagBlock().location();
    }
}
