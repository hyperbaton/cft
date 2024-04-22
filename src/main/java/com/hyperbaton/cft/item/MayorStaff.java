package com.hyperbaton.cft.item;

import com.hyperbaton.cft.capability.need.NeedCapability;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.network.CftPacketHandler;
import com.hyperbaton.cft.network.CheckOnXunguiPacket;
import com.hyperbaton.cft.structure.home.HomeDetection;
import com.hyperbaton.cft.world.HomesData;
import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

import java.util.stream.Collectors;

public class MayorStaff extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();

    public MayorStaff(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if (!pContext.getLevel().isClientSide()) {

            boolean foundHouse;
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
                    LOGGER.debug("House already registered.");
                    foundHouse = false;
                } else {
                    foundHouse = new HomeDetection().detectAnyHouse(positionClicked, (ServerLevel) pContext.getLevel(), player.getUUID());
                }

            } else {
                foundHouse = false;
            }

            if (!foundHouse) {
                player.sendSystemMessage(Component.literal("No house found."));
            } else {
                player.sendSystemMessage(Component.literal("House found."));
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player playerIn, LivingEntity entity, InteractionHand hand) {
        if (!playerIn.level().isClientSide &&
                entity instanceof XunguiEntity) {
            if (((XunguiEntity) entity).getLeaderId() != null &&
                    ((XunguiEntity) entity).getLeaderId().equals(playerIn.getUUID())) {
                // Send message to client player with information about this Xungui
                CheckOnXunguiPacket message = createXunguiInfoMessage((XunguiEntity) entity);
                CftPacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) playerIn), message);
            } else {
                playerIn.sendSystemMessage(Component.literal("You are not the leader of this Xungui."));
            }
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    private CheckOnXunguiPacket createXunguiInfoMessage(XunguiEntity entity) {
        return new CheckOnXunguiPacket(entity.getSocialClass().getId(),
                entity.getHappiness(),
                entity.getNeeds().stream().collect(Collectors
                        .toMap(needCapability -> needCapability.getNeed().getId(),
                                NeedCapability::getSatisfaction)));
    }

    private boolean clickedOnDoor(UseOnContext pContext) {
        return pContext.getLevel().getBlockState(pContext.getClickedPos()).is(BlockTags.DOORS);
    }
}
