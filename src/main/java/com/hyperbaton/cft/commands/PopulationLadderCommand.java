package com.hyperbaton.cft.commands;

import com.hyperbaton.cft.socialclass.SocialStructureHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class PopulationLadderCommand {
    public PopulationLadderCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("populationLadder")
                        .executes(command -> getPopulationLadder(command.getSource())));
    }

    private int getPopulationLadder(CommandSourceStack source) {
        Map<Component, Integer> populationPerPlayer = new HashMap<>();
        source.getLevel().players().forEach(player -> populationPerPlayer.put(player.getDisplayName(),
                getPopulationForPlayer(player, source.getLevel())));
        source.sendSuccess(() -> formatLadder(populationPerPlayer), true);
        return 0;
    }

    private Component formatLadder(Map<Component, Integer> populationPerPlayer) {
        MutableComponent formattedladder = Component.translatable("cft.populationladder.header")
                .append(Component.literal("\n"));
        populationPerPlayer.forEach((key, value) -> formattedladder.append(key)
                .append(Component.literal("   "))
                .append(Component.literal(value.toString()))
                .append(Component.literal("\n")));
        return formattedladder;
    }

    private Integer getPopulationForPlayer(ServerPlayer player, ServerLevel level) {
        return SocialStructureHelper.computeSocialStructureForPlayer(level, player)
                .values().stream().reduce(0, Integer::sum);
    }
}
