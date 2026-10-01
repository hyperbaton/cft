package com.hyperbaton.cft.compat.jade;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

/**
 * Shows a Xoonglin's class, job and happiness when looking at it. Its own leader also
 * sees the most pressing need and whether it's ready to mate.
 */
public enum XoonglinJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "xoonglin");

    private static final String KEY_HAPPINESS = "happiness";
    private static final String KEY_MAX_HAPPINESS = "maxHappiness";
    private static final String KEY_JOB = "job";
    private static final String KEY_LEADER_NAME = "leaderName";
    private static final String KEY_OWN = "own";
    private static final String KEY_WORST_NEED = "worstNeed";
    private static final String KEY_WORST_NEED_CRITICAL = "worstNeedCritical";
    private static final String KEY_UNSATISFIED = "unsatisfied";
    private static final String KEY_CAN_MATE = "canMate";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof XoonglinEntity xoonglin) || xoonglin.getSocialClass() == null) return;

        data.putDouble(KEY_HAPPINESS, xoonglin.getHappiness());
        data.putDouble(KEY_MAX_HAPPINESS, xoonglin.getSocialClass().getMaxHappiness());
        if (xoonglin.getJob() != null) {
            data.putString(KEY_JOB, xoonglin.getJob().toString());
        }

        boolean own = xoonglin.getLeaderId() != null && xoonglin.getLeaderId().equals(accessor.getPlayer().getUUID());
        data.putBoolean(KEY_OWN, own);
        if (!own) {
            Player leader = xoonglin.getLeaderId() != null ? xoonglin.level().getPlayerByUUID(xoonglin.getLeaderId()) : null;
            if (leader != null) {
                data.putString(KEY_LEADER_NAME, leader.getGameProfile().getName());
            }
            return;
        }

        xoonglin.getWorstNeed().ifPresent(worst -> {
            data.putString(KEY_WORST_NEED, worst.getNeedId().toString());
            data.putBoolean(KEY_WORST_NEED_CRITICAL, XoonglinEntity.isCritical(worst));
        });
        data.putInt(KEY_UNSATISFIED, xoonglin.getUnsatisfiedVisibleNeeds().size());
        data.putBoolean(KEY_CAN_MATE, xoonglin.canMate());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof XoonglinEntity xoonglin)) return;
        CompoundTag data = accessor.getServerData();

        Component classLine = Component.translatable(xoonglin.getEntityData().get(XoonglinEntity.SOCIAL_CLASS_NAME))
                .withStyle(ChatFormatting.AQUA);
        if (data.contains(KEY_JOB)) {
            ResourceLocation jobId = ResourceLocation.parse(data.getString(KEY_JOB));
            classLine = classLine.copy()
                    .append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("job." + jobId.getNamespace() + "." + jobId.getPath())
                            .withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(classLine);

        if (data.contains(KEY_HAPPINESS)) {
            tooltip.add(Component.translatable("jade.cft.happiness",
                    String.format("%.1f", data.getDouble(KEY_HAPPINESS)),
                    String.format("%.0f", data.getDouble(KEY_MAX_HAPPINESS))));
        }

        if (data.contains(KEY_LEADER_NAME)) {
            tooltip.add(Component.translatable("jade.cft.leader", data.getString(KEY_LEADER_NAME))
                    .withStyle(ChatFormatting.GRAY));
        }

        if (!data.getBoolean(KEY_OWN)) return;

        if (data.contains(KEY_WORST_NEED)) {
            boolean critical = data.getBoolean(KEY_WORST_NEED_CRITICAL);
            ItemStack icon = iconOf(xoonglin);
            if (!icon.isEmpty()) {
                tooltip.add(IElementHelper.get().smallItem(icon));
                tooltip.append(needLine(data, critical));
            } else {
                tooltip.add(needLine(data, critical));
            }
        } else {
            tooltip.add(Component.translatable("jade.cft.all_needs_met").withStyle(ChatFormatting.GREEN));
        }

        if (data.getBoolean(KEY_CAN_MATE)) {
            tooltip.add(Component.translatable("jade.cft.ready_to_mate").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static Component needLine(CompoundTag data, boolean critical) {
        int others = data.getInt(KEY_UNSATISFIED) - 1;
        Component line = Component.translatable(critical ? "jade.cft.needs_critical" : "jade.cft.needs",
                Component.translatable(data.getString(KEY_WORST_NEED)));
        if (others > 0) {
            line = line.copy().append(Component.translatable("jade.cft.more_needs", others));
        }
        return line.copy().withStyle(critical ? ChatFormatting.RED : ChatFormatting.YELLOW);
    }

    private static ItemStack iconOf(XoonglinEntity xoonglin) {
        ResourceLocation id = ResourceLocation.tryParse(xoonglin.getNeedAlertIcon());
        return id != null && BuiltInRegistries.ITEM.containsKey(id)
                ? new ItemStack(BuiltInRegistries.ITEM.get(id))
                : ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
