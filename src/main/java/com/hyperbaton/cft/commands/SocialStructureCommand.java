package com.hyperbaton.cft.commands;

import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.socialclass.SocialStructureHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class SocialStructureCommand {
    public SocialStructureCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("socialstructure")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(command -> getSocialStructure(command.getSource(),
                                EntityArgument.getPlayer(command, "player")))));
    }

    private int getSocialStructure(CommandSourceStack sourceStack, ServerPlayer player) throws CommandSyntaxException {
        Map<SocialClass, Integer> socialStructure = SocialStructureHelper.computeSocialStructureForPlayer(sourceStack.getLevel(), player);
        sourceStack.sendSuccess(() -> formatSocialStructure(socialStructure), true);
        return 0;
    }

    private Component formatSocialStructure(Map<SocialClass, Integer> socialStructure) {
        int population = socialStructure.values().stream().reduce(0, Integer::sum);
        MutableComponent formattedSocialStructure = Component.translatable("cft.socialstructure.message")
                .withStyle(ChatFormatting.BLUE)
                .append(Component.literal("\n"))
                .append(Component.translatable("cft.socialstructure.population").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.valueOf(population)).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("\n"));
        socialStructure.forEach((key, value) -> formattedSocialStructure.append(Component.translatable(key.getId())
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("    "))
                .append(Component.literal(value.toString()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("  ("))
                .append(Component.literal(populationFraction(value, population)).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("%)\n")));
        return formattedSocialStructure;
    }

    private String populationFraction(Integer value, int population) {
        return BigDecimal.valueOf(value)
                .setScale(8, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(population).setScale(8, RoundingMode.HALF_UP),  RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .toString();
    }
}
