package com.hyperbaton.cft.item;

import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MayorStaff extends Item {
    private static final int MAX_HOUSE_SIZE = 100;

    public MayorStaff(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if(!pContext.getLevel().isClientSide()){

            boolean foundHouse = true;
            Player player = pContext.getPlayer();

            if(clickedOnDoor(pContext)){

                BlockPos positionClicked = pContext.getClickedPos();
                Direction facingDirectionX = Direction.getFacingAxis(player, Direction.Axis.X);
                Direction facingDirectionZ = Direction.getFacingAxis(player, Direction.Axis.Z);

                DoubleBlockHalf halfOfDoor = pContext.getLevel().getBlockState(pContext.getClickedPos()).getValue(DoorBlock.HALF);
                if(halfOfDoor.equals(DoubleBlockHalf.UPPER)){
                    positionClicked = positionClicked.below();
                }

                Set<BlockPos> houseBlocks = Sets.newHashSet(positionClicked);
                // LinkedList<BlockPos> uncheckedBlocks = List.of(positionClicked, positionClicked.relative(facingDirectionX, 1), positionClicked.relative(facingDirectionZ, 1));
                foundHouse = checkBlocks(pContext.getLevel(), positionClicked.relative(facingDirectionX, 1), houseBlocks, foundHouse);

                if(foundHouse){
                    houseBlocks.stream().forEach(blockPos -> ((ServerLevel) pContext.getLevel()).sendParticles(ParticleTypes.BUBBLE, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                            1, blockPos.getX(), blockPos.getY(), blockPos.getZ(), 0.2f));
                }
                player.sendSystemMessage(Component.literal("Door at " + "(" + positionClicked.getX() + " , " + positionClicked.getY() + " , " + positionClicked.getZ() + ")"));

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

    private boolean clickedOnDoor(UseOnContext pContext){
        return pContext.getLevel().getBlockState(pContext.getClickedPos()).is(BlockTags.DOORS);
    }

    private boolean checkBlocks(Level level, BlockPos positionToCheck, Set<BlockPos> houseBlocks, boolean isHouse){
        System.out.print("Checking position: " + "(" + positionToCheck.getX() + " , " + positionToCheck.getY() + " , " + positionToCheck.getZ() + ")" + " house size: " + houseBlocks.size() + "\n");
        if(!isHouse || houseBlocks.size() > MAX_HOUSE_SIZE){
            return false;
        } else if (houseBlocks.contains(positionToCheck)) {
            return true;
        } else {
            if(isWall(level.getBlockState(positionToCheck))){
                houseBlocks.add(positionToCheck);

                return checkBlocks(level, positionToCheck.above(), houseBlocks, true)
                        && checkBlocks(level, positionToCheck.below(), houseBlocks, true)
                        && checkBlocks(level, positionToCheck.east(), houseBlocks, true)
                        && checkBlocks(level, positionToCheck.west(), houseBlocks, true)
                        && checkBlocks(level, positionToCheck.south(), houseBlocks, true)
                        && checkBlocks(level, positionToCheck.north(), houseBlocks, true);

            } else {
                return true;
            }
        }

    }

    private boolean isInside(BlockState blockState) {
        return blockState.is(Blocks.AIR);
    }

    private boolean isWall(BlockState blockState) {
        return blockState.is(BlockTags.DOORS)
                || blockState.is(BlockTags.PLANKS);
    }
}
