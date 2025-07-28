package com.hyperbaton.cft.item;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.network.CftPacketHandler;
import com.hyperbaton.cft.network.CheckOnXoonglinPacket;
import com.hyperbaton.cft.network.HomeDetectionPacket;
import com.hyperbaton.cft.structure.home.HomeDetection;
import com.hyperbaton.cft.structure.home.HomeDetectionReasons;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.network.PacketDistributor;

import java.util.stream.Collectors;

public class LeaderStaff extends Item {

    public LeaderStaff(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if (!pContext.getLevel().isClientSide()) {

            HomeDetectionPacket foundHouseMessage;
            Player player = pContext.getPlayer();

            if (clickedOnDoor(pContext)) {

                BlockPos positionClicked = pContext.getClickedPos();

                // Make sure we point to the lower part of the door
                DoubleBlockHalf halfOfDoor = pContext.getLevel().getBlockState(pContext.getClickedPos()).getValue(DoorBlock.HALF);
                if (halfOfDoor.equals(DoubleBlockHalf.UPPER)) {
                    positionClicked = positionClicked.below();
                }

                // Check if there is already a house at that position
                HomesData homesData = ((ServerLevel) pContext.getLevel()).getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
                BlockPos finalPositionClicked = positionClicked;
                if (homesData.getHomes().stream().anyMatch(home -> home.getEntrance().equals(finalPositionClicked))) {
                    foundHouseMessage = new HomeDetectionPacket(false, "", HomeDetectionReasons.ALREADY_REGISTERED);
                } else {
                    foundHouseMessage = new HomeDetection().detectAnyHouse(positionClicked, (ServerLevel) pContext.getLevel(), player.getUUID());
                }

            } else {
                foundHouseMessage = new HomeDetectionPacket(false, "", HomeDetectionReasons.NOT_A_DOOR);
            }


            CftPacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), foundHouseMessage);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player playerIn, LivingEntity entity, InteractionHand hand) {
        if (!playerIn.level().isClientSide &&
                entity instanceof XoonglinEntity) {
            if (((XoonglinEntity) entity).getLeaderId() != null &&
                    ((XoonglinEntity) entity).getLeaderId().equals(playerIn.getUUID())) {
                // Send message to client player with information about this Xoonglin
                CheckOnXoonglinPacket message = createXoonglinInfoMessage((XoonglinEntity) entity);
                CftPacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) playerIn), message);
            } else {
                playerIn.sendSystemMessage(Component.literal("You are not the leader of this Xoonglin."));
            }
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    private CheckOnXoonglinPacket createXoonglinInfoMessage(XoonglinEntity entity) {
        return new CheckOnXoonglinPacket(
                entity.getCustomName(),
                entity.getSocialClass().getId(),
                entity.getHappiness(),
                entity.getNeeds().stream().filter(needSatisfier -> !needSatisfier.getNeed().isHidden())
                        .collect(Collectors
                                .toMap(needSatisfier -> needSatisfier.getNeed().getId(),
                                        NeedSatisfier::getSatisfaction)));
    }

    private boolean clickedOnDoor(UseOnContext pContext) {
        return pContext.getLevel().getBlockState(pContext.getClickedPos()).is(BlockTags.DOORS);
    }
}
