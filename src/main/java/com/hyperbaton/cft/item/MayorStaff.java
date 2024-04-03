package com.hyperbaton.cft.item;

import com.hyperbaton.cft.structure.home.HomeDetection;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class MayorStaff extends Item {

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

                // Check if there is already a house at that position
                HomesData homesData = ((ServerLevel) pContext.getLevel()).getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
                BlockPos finalPositionClicked = positionClicked;
                if(homesData.getHomes().stream().anyMatch(home -> home.getEntrance().equals(finalPositionClicked))){
                    foundHouse = false;
                } else {
                    foundHouse = new HomeDetection().detectAnyHouse(positionClicked, (ServerLevel) pContext.getLevel(), player.getUUID());
                }

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
}
