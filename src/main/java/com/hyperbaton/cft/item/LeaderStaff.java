package com.hyperbaton.cft.item;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.CftPacketHandler;
import com.hyperbaton.cft.network.CheckOnXoonglinPacket;
import com.hyperbaton.cft.network.HomeDetectionPacket;
import com.hyperbaton.cft.network.NeedSatisfactionData;
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
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.Map;
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

                DoubleBlockHalf halfOfDoor = pContext.getLevel().getBlockState(pContext.getClickedPos()).getValue(DoorBlock.HALF);
                if (halfOfDoor.equals(DoubleBlockHalf.UPPER)) {
                    positionClicked = positionClicked.below();
                }

                HomesData homesData = ((ServerLevel) pContext.getLevel()).getDataStorage().computeIfAbsent(HomesData.factory(), "homesData");
                BlockPos finalPositionClicked = positionClicked;
                if (homesData.getHomes().stream().anyMatch(home -> home.getEntrance().equals(finalPositionClicked))) {
                    foundHouseMessage = new HomeDetectionPacket(false, "", HomeDetectionReasons.ALREADY_REGISTERED, Collections.emptyList());
                } else {
                    foundHouseMessage = new HomeDetection().detectAnyHouse(positionClicked, (ServerLevel) pContext.getLevel(), player.getUUID());
                }

            } else {
                foundHouseMessage = new HomeDetectionPacket(false, "", HomeDetectionReasons.NOT_A_DOOR, Collections.emptyList());
            }


            PacketDistributor.sendToPlayer((ServerPlayer) player, foundHouseMessage);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player playerIn, LivingEntity entity, InteractionHand hand) {
        if (!playerIn.level().isClientSide &&
                entity instanceof XoonglinEntity) {
            if (((XoonglinEntity) entity).getLeaderId() != null &&
                    ((XoonglinEntity) entity).getLeaderId().equals(playerIn.getUUID())) {
                CheckOnXoonglinPacket message = createXoonglinInfoMessage((XoonglinEntity) entity);
                PacketDistributor.sendToPlayer((ServerPlayer) playerIn, message);
            } else {
                playerIn.sendSystemMessage(Component.literal("You are not the leader of this Xoonglin."));
            }
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    private CheckOnXoonglinPacket createXoonglinInfoMessage(XoonglinEntity entity) {
        Map<String, NeedSatisfactionData> needsData = entity.getNeeds().stream()
                .filter(needSatisfier -> !needSatisfier.getNeed().isHidden())
                .collect(Collectors.toMap(
                        needSatisfier -> needSatisfier.getNeed().getId(),
                        needSatisfier -> new NeedSatisfactionData(
                                needSatisfier.getSatisfaction(),
                                needSatisfier.getNeed().getDamageThreshold(),
                                needSatisfier.getNeed().getSatisfactionThreshold()
                        )
                ));

        return new CheckOnXoonglinPacket(
                entity.getCustomName(),
                entity.getSocialClass().getId(),
                entity.getJob(),
                entity.getHappiness(),
                needsData,
                entity.getUUID()
        );
    }


    private boolean clickedOnDoor(UseOnContext pContext) {
        return pContext.getLevel().getBlockState(pContext.getClickedPos()).is(BlockTags.DOORS);
    }
}
