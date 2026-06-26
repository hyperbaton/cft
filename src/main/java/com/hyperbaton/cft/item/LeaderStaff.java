package com.hyperbaton.cft.item;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.CheckOnXoonglinPacket;
import com.hyperbaton.cft.network.NeedSatisfactionData;
import com.hyperbaton.cft.network.StructureDetectionPacket;
import com.hyperbaton.cft.structure.StructureDetectionReasons;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderStaff extends Item {

    public LeaderStaff(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if (!pContext.getLevel().isClientSide()) {
            Player player = pContext.getPlayer();
            ServerLevel serverLevel = (ServerLevel) pContext.getLevel();

            if (clickedOnKeyBlock(pContext)) {
                BlockPos clickedPos = pContext.getClickedPos();
                BlockState clickedState = serverLevel.getBlockState(clickedPos);
                if (clickedState.getBlock() instanceof DoorBlock) {
                    DoubleBlockHalf half = clickedState.getValue(DoorBlock.HALF);
                    if (half == DoubleBlockHalf.UPPER) {
                        clickedPos = clickedPos.below();
                    }
                }

                StructureDetectionPacket structureMessage = detectStructure(clickedPos, serverLevel, player.getUUID());
                PacketDistributor.sendToPlayer((ServerPlayer) player, structureMessage);
            } else {
                StructureDetectionPacket message = new StructureDetectionPacket(
                        false, "", StructureDetectionReasons.NOT_A_KEY_BLOCK, Collections.emptyList());
                PacketDistributor.sendToPlayer((ServerPlayer) player, message);
            }
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
                                needSatisfier.getNeed().getSatisfactionThreshold(),
                                needSatisfier.getNeed().getIcons()
                        )
                ));

        return new CheckOnXoonglinPacket(
                entity.getCustomName(),
                entity.getSocialClass().getId(),
                entity.getJob(),
                entity.getHappiness(),
                needsData,
                entity.getUUID(),
                JobUtil.buildJobInfo(entity),
                JobUtil.buildInventoryData(entity),
                entity.getSocialClass().getJobs()
        );
    }


    private boolean clickedOnKeyBlock(UseOnContext pContext) {
        if (CftRegistry.STRUCTURES == null) return false;
        BlockState clickedState = pContext.getLevel().getBlockState(pContext.getClickedPos());
        return CftRegistry.STRUCTURES.stream().anyMatch(structureType -> structureType.matchesKeyBlock(clickedState));
    }

    private StructureDetectionPacket detectStructure(BlockPos clickedPos, ServerLevel level, UUID leaderId) {
        StructuresData structuresData = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");

        if (structuresData.getStructures().stream().anyMatch(s -> s.getKeyBlockPos().equals(clickedPos))) {
            return new StructureDetectionPacket(false, "", StructureDetectionReasons.ALREADY_REGISTERED, Collections.emptyList());
        }

        BlockState clickedState = level.getBlockState(clickedPos);
        List<StructureType> matchingTypes = CftRegistry.STRUCTURES.stream()
                .filter(st -> st.matchesKeyBlock(clickedState))
                .sorted(Comparator.comparingInt(StructureType::getPriority).reversed())
                .toList();

        StructureDetectionResult bestFailure = null;

        for (StructureType structureType : matchingTypes) {
            StructureDetectionResult result = structureType.createDetector().detect(clickedPos, level, leaderId, structureType);
            if (result.success()) {
                structuresData.addStructure(result.structure());

                return new StructureDetectionPacket(true, structureType.getId(),
                        StructureDetectionReasons.STRUCTURE_DETECTED, Collections.emptyList());
            }
            if (bestFailure == null || result.reason().ordinal() > bestFailure.reason().ordinal()) {
                bestFailure = result;
            }
        }

        if (bestFailure != null) {
            return new StructureDetectionPacket(false, "", bestFailure.reason(), bestFailure.validationDetails());
        }

        return new StructureDetectionPacket(false, "", StructureDetectionReasons.NOT_A_KEY_BLOCK, Collections.emptyList());
    }

}
