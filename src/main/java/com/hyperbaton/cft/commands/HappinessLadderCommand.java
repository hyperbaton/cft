package com.hyperbaton.cft.commands;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.socialclass.SocialStructureHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class HappinessLadderCommand {
    public HappinessLadderCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("happinessLadder")
                        .executes(command -> getHappinessLadder(command.getSource())));
    }

    private int getHappinessLadder(CommandSourceStack source) {
        Map<Component, Integer> happinessPerPlayer = new HashMap<>();
        source.getLevel().players().forEach(player -> happinessPerPlayer.put(player.getDisplayName(),
                getHappinessForPlayer(player, source.getLevel())));
        source.sendSuccess(() -> formatLadder(happinessPerPlayer), true);
        return 0;
    }

    private Component formatLadder(Map<Component, Integer> happinessPerPlayer) {
        MutableComponent formattedladder = Component.translatable("cft.happinessladder.header")
                .append(Component.literal("\n"));
        happinessPerPlayer.forEach((key, value) -> formattedladder.append(key)
                .append(Component.literal("   "))
                .append(Component.literal(value.toString()))
                .append(Component.literal("\n")));
        return formattedladder;
    }

    private Integer getHappinessForPlayer(ServerPlayer player, ServerLevel level) {
        return SocialStructureHelper.getAllXoonglins(level).stream()
                .filter(xoonglin -> xoonglin.getLeaderId().equals(player.getUUID()))
                .map(XoonglinEntity::getHappiness)
                .reduce(0.0, Double::sum).intValue();

    }
}
