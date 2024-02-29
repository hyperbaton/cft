package com.hyperbaton.cft.structure.home;

import com.google.common.collect.Comparators;
import com.google.common.collect.Sets;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.Set;

public class HomeDetection {
    private static final int MAX_HOUSE_SIZE = 1000;
    private static final int MAX_FLOOR_SIZE = 100;
    private static final int MAX_HEIGHT = 319;

    public boolean detectHouse(BlockPos entrance, ServerLevel level, Player leader){
        Set<BlockPos> houseBlocks = Sets.newHashSet(entrance);
        Set<BlockPos> floorBlocks = Sets.newHashSet();
        Set<BlockPos> floorPerimeterBlocks = Sets.newHashSet();

        // Detect the floor of the house
        boolean foundFloor = findFloor(level, entrance.below(), floorBlocks, floorPerimeterBlocks);
        if(floorBlocks.isEmpty()) { // TODO: Proper treatment of this case
            leader.sendSystemMessage(Component.literal("No house found. Invalid floor"));
            return false;
        }
        // The inner corners are misidentified as non perimeter blocks in the previous method.
        detectInnerCorners(level, floorBlocks, floorPerimeterBlocks);
        houseBlocks.addAll(floorBlocks);
        houseBlocks.addAll(floorPerimeterBlocks);


        // Detect the walls of the house
        Set<BlockPos> wallBlocks = Sets.newHashSet();
        boolean foundWall = findWall(level, floorPerimeterBlocks, wallBlocks);
        if(!foundWall){
            leader.sendSystemMessage(Component.literal("No house found. Invalid walls"));
            return false;
        }
        houseBlocks.addAll(wallBlocks);
        // Get the starting height of the roof
        int lowestRoofHeight = wallBlocks.stream().map(Vec3i::getY).max(Comparators::max).orElse(entrance.getY());

        // Detect the indoor area of the house
        Set<BlockPos> interiorBlocks = Sets.newHashSet();
        boolean foundInterior = findInterior(level, floorBlocks, interiorBlocks);
        houseBlocks.addAll(interiorBlocks);

        // Detect the roof of the house
        Set<BlockPos> roofBlocks = Sets.newHashSet();
        boolean foundRoof = findRoof(level, floorBlocks, roofBlocks, lowestRoofHeight);
        houseBlocks.addAll(roofBlocks);

        boolean foundHouse = foundFloor && foundWall && foundInterior && foundRoof;

        // Detect the container for supplying the house
        BlockPos containerPos = findContainer(level, floorBlocks);

        if(containerPos == null) {
            foundHouse = false;
        }

        if(foundHouse){
            houseBlocks.forEach(blockPos -> ((ServerLevel) level).sendParticles(ParticleTypes.ENTITY_EFFECT, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                    5, 0, 0, 0, 0.1));
            HomesData homesData = ((ServerLevel) level).getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
            homesData.addHome(new XunguiHome(entrance, containerPos, houseBlocks.size(), leader.getId(), 0, "PLACEHOLDER"));
            homesData.setDirty();
            leader.sendSystemMessage(Component.literal("House size:" + houseBlocks.size()));

        }
        return foundHouse;
    }

    private BlockPos findContainer(ServerLevel level, Set<BlockPos> floorBlocks) {
        Set<BlockPos> containers = Sets.newHashSet();
        for(BlockPos pos : floorBlocks){
            if(isContainer(level.getBlockState(pos.above()))) {
                containers.add(pos);
            }
        }
        if(containers.size() == 1){
            return containers.stream().findFirst().orElse(null);
        }
        return null;
    }

    private void detectInnerCorners(Level level, Set<BlockPos> floorBlocks, Set<BlockPos> floorPerimeterBlocks) {
        Set<BlockPos> innerCorners = Sets.newHashSet();
        floorBlocks.forEach(floorBlockPos -> {
            int perimeterNeighbours = countPerimeterExtendedNeighbours(floorPerimeterBlocks, floorBlockPos);
            int exteriorNeighbours = countExteriorExtendedNeighbours(floorBlocks, floorPerimeterBlocks, floorBlockPos);
            if(perimeterNeighbours >= 2 && exteriorNeighbours == 1){
                innerCorners.add(floorBlockPos);
            }
        });
        floorPerimeterBlocks.addAll(innerCorners);
        floorBlocks.removeAll(innerCorners);
    }

    private int countExteriorExtendedNeighbours(Set<BlockPos> floorBlocks, Set<BlockPos> floorPerimeterBlocks, BlockPos testPos) {
        int count = 0;
        Set<BlockPos> allFloorBlocks = Sets.newHashSet();
        allFloorBlocks.addAll(floorBlocks);
        allFloorBlocks.addAll(floorPerimeterBlocks);
        if(!allFloorBlocks.contains(testPos.north())) {
            count ++;
        }
        if(!allFloorBlocks.contains(testPos.south())) {
            count ++;
        }
        if(!allFloorBlocks.contains(testPos.west())) {
            count ++;
        }
        if(!allFloorBlocks.contains(testPos.east())) {
            count ++;
        }
        if(!allFloorBlocks.contains(testPos.north().east())) {
            count ++;
        }
        if(!allFloorBlocks.contains(testPos.south().west())) {
            count ++;
        }
        if(!allFloorBlocks.contains(testPos.west().north())) {
            count ++;
        }
        if(!allFloorBlocks.contains(testPos.east().south())) {
            count ++;
        }
        return count;
    }

    private int countPerimeterExtendedNeighbours(Set<BlockPos> floorPerimeterBlocks, BlockPos testPos) {
        int count = 0;
        if(floorPerimeterBlocks.contains(testPos.north())) {
            count ++;
        }
        if(floorPerimeterBlocks.contains(testPos.south())) {
            count ++;
        }
        if(floorPerimeterBlocks.contains(testPos.west())) {
            count ++;
        }
        if(floorPerimeterBlocks.contains(testPos.east())) {
            count ++;
        }
        if(floorPerimeterBlocks.contains(testPos.north().east())) {
            count ++;
        }
        if(floorPerimeterBlocks.contains(testPos.south().west())) {
            count ++;
        }
        if(floorPerimeterBlocks.contains(testPos.west().north())) {
            count ++;
        }
        if(floorPerimeterBlocks.contains(testPos.east().south())) {
            count ++;
        }
        return count;
    }

    // TODO: Improve roofs. Allow for slanted roofs.
    private boolean findRoof(Level level, Set<BlockPos> floorBlocks, Set<BlockPos> roofBlocks, int lowestRoofHeight) {
        floorBlocks.forEach(floorPos -> {
            BlockPos testPos = floorPos.above();
            while (!isRoof(level.getBlockState(testPos)) && testPos.getY() < MAX_HEIGHT){
                testPos = testPos.above();
            }
            if(isRoof(level.getBlockState(testPos))) {
                roofBlocks.add(testPos);
            }
        });
        return !roofBlocks.isEmpty()
                && roofBlocks.stream().allMatch(roofPos ->
                (roofPos.getY() == lowestRoofHeight)
                        && isRoof(level.getBlockState(roofPos)));
    }

    private boolean findInterior(Level level, Set<BlockPos> floorBlocks, Set<BlockPos> interiorBlocks) {
        floorBlocks.forEach(floorPos -> {
            BlockPos testPos = floorPos.above();
            while (isInterior(level.getBlockState(testPos))){
                interiorBlocks.add(testPos);
                if(testPos.getY() >= MAX_HEIGHT){ // TODO: This should say there is no house
                    break;
                } else {
                    testPos = testPos.above();
                }
            }
        });
        return true;
    }

    private boolean findWall(Level level, Set<BlockPos> floorPerimeterBlocks, Set<BlockPos> wallBlocks) {
        List<Integer> heights = Lists.newArrayList();
        floorPerimeterBlocks.forEach(perimeterBlockPos -> {
            BlockPos testPos = perimeterBlockPos.above();
            while (isWall(level.getBlockState(testPos))){
                wallBlocks.add(testPos);
                testPos = testPos.above();
            }
            heights.add(testPos.below().getY());
        });
        // Check that all wall pieces have the same size
        return heights.stream().distinct().count() == 1;
    }

    private boolean findFloor(Level level, BlockPos testPos, Set<BlockPos> floorBlocks, Set<BlockPos> floorPerimeterBlocks) {
        if(floorBlocks.size() + floorPerimeterBlocks.size() > MAX_FLOOR_SIZE){
            return false;
        }
        if(!isFloor(level.getBlockState(testPos))){
            return true;
        }
        if(floorBlocks.contains(testPos) || floorPerimeterBlocks.contains(testPos)){
            return true;
        }
        int floorNeighbours = countFloorNeighbours(level, testPos);
        switch (floorNeighbours){
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
        return findFloor(level, testPos.north(), floorBlocks, floorPerimeterBlocks)
                && findFloor(level, testPos.south(), floorBlocks, floorPerimeterBlocks)
                && findFloor(level, testPos.west(), floorBlocks, floorPerimeterBlocks)
                && findFloor(level, testPos.east(), floorBlocks, floorPerimeterBlocks);
    }

    private int countFloorNeighbours(Level level, BlockPos testPos) {
        int count = 0;
        if(isFloor(level.getBlockState(testPos.north()))) {
            count ++;
        }
        if(isFloor(level.getBlockState(testPos.south()))) {
            count ++;
        }
        if(isFloor(level.getBlockState(testPos.east()))) {
            count ++;
        }
        if(isFloor(level.getBlockState(testPos.west()))) {
            count ++;
        }
        return count;
    }

    private boolean isFloor(BlockState blockState) {
        return blockState.is(BlockTags.PLANKS);
    }

    private boolean isInterior(BlockState blockState) {
        return blockState.is(Blocks.AIR);
    }

    private boolean isRoof(BlockState blockState) {
        return blockState.is(BlockTags.PLANKS);
    }

    private boolean isWall(BlockState blockState) {
        return blockState.is(BlockTags.DOORS)
                || blockState.is(BlockTags.PLANKS);
    }

    private boolean isContainer(BlockState blockState) {
        return blockState.is(Blocks.CHEST);
    }
}
