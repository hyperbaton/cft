package com.hyperbaton.cft.item;

import com.google.common.collect.Comparators;
import com.google.common.collect.Sets;
import com.hyperbaton.cft.structure.home.XunguiHome;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.Set;

public class MayorStaff extends Item {
    private static final int MAX_HOUSE_SIZE = 1000;
    private static final int MAX_FLOOR_SIZE = 100;
    private static final int MAX_HEIGHT = 319;

    public MayorStaff(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if(!pContext.getLevel().isClientSide()){

            boolean foundHouse;
            Player player = pContext.getPlayer();

            if(clickedOnDoor(pContext)){

                BlockPos positionClicked = pContext.getClickedPos();

                // Make sure we point to the lower part of the door
                DoubleBlockHalf halfOfDoor = pContext.getLevel().getBlockState(pContext.getClickedPos()).getValue(DoorBlock.HALF);
                if(halfOfDoor.equals(DoubleBlockHalf.UPPER)){
                    positionClicked = positionClicked.below();
                }

                Set<BlockPos> houseBlocks = Sets.newHashSet(positionClicked);
                Set<BlockPos> floorBlocks = Sets.newHashSet();
                Set<BlockPos> floorPerimeterBlocks = Sets.newHashSet();

                // Detect the floor of the house
                boolean foundFloor = findFloor(pContext.getLevel(), positionClicked.below(), floorBlocks, floorPerimeterBlocks);
                if(floorBlocks.isEmpty()) { // TODO: Proper treatment of this case
                    player.sendSystemMessage(Component.literal("No house found."));
                    return InteractionResult.SUCCESS;
                }
                // The inner corners are misidentified as non perimeter blocks in the previous method.
                detectInnerCorners(pContext.getLevel(), floorBlocks, floorPerimeterBlocks);
                houseBlocks.addAll(floorBlocks);
                houseBlocks.addAll(floorPerimeterBlocks);


                // Detect the walls of the house
                Set<BlockPos> wallBlocks = Sets.newHashSet();
                boolean foundWall = findWall(pContext.getLevel(), floorPerimeterBlocks, wallBlocks);
                if(!foundWall){
                    player.sendSystemMessage(Component.literal("No house found."));
                    return InteractionResult.SUCCESS;
                }
                houseBlocks.addAll(wallBlocks);
                // Get the starting height of the roof
                int lowestRoofHeight = wallBlocks.stream().map(Vec3i::getY).max(Comparators::max).orElse(positionClicked.getY());

                // Detect the indoor area of the house
                Set<BlockPos> interiorBlocks = Sets.newHashSet();
                boolean foundInterior = findInterior(pContext.getLevel(), floorBlocks, interiorBlocks);
                houseBlocks.addAll(interiorBlocks);

                // Detect the roof of the house
                Set<BlockPos> roofBlocks = Sets.newHashSet();
                boolean foundRoof = findRoof(pContext.getLevel(), floorBlocks, roofBlocks, lowestRoofHeight);
                houseBlocks.addAll(roofBlocks);

                foundHouse = foundFloor && foundWall && foundInterior && foundRoof;

                if(foundHouse){
                    houseBlocks.forEach(blockPos -> ((ServerLevel) pContext.getLevel()).sendParticles(ParticleTypes.ENTITY_EFFECT, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                            5, 0, 0, 0, 0.1));
                    ((ServerLevel) pContext.getLevel()).getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData")
                            .addHome(new XunguiHome(positionClicked, houseBlocks.size(), player.getId(), 0, "PLACEHOLDER"));
                    
                }
                player.sendSystemMessage(Component.literal("House size:" + houseBlocks.size()));

            } else {
                foundHouse = false;
            }

            if(!foundHouse){
                player.sendSystemMessage(Component.literal("No house found."));
            } else {
                player.sendSystemMessage(Component.literal("House found."));
            }
        }

        return InteractionResult.SUCCESS;
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

    private boolean clickedOnDoor(UseOnContext pContext){
        return pContext.getLevel().getBlockState(pContext.getClickedPos()).is(BlockTags.DOORS);
    }

    private boolean isWall(BlockState blockState) {
        return blockState.is(BlockTags.DOORS)
                || blockState.is(BlockTags.PLANKS);
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
}
