package com.hyperbaton.cft.commands;

import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class SocialStructureCommand {
    public SocialStructureCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("socialstructure")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(command -> getSocialStructure(command.getSource(),
                                EntityArgument.getPlayer(command, "player")))));
    }

    private int getSocialStructure(CommandSourceStack sourceStack, ServerPlayer player) throws CommandSyntaxException {
        Iterator<Entity> entityIterator = sourceStack.getLevel().getEntities().getAll().iterator();
        List<XunguiEntity> xunguiList = new ArrayList<>();
        while (entityIterator.hasNext()) {
            Entity entity = entityIterator.next();
            if (entity instanceof XunguiEntity) {
                xunguiList.add((XunguiEntity) entity);
            }
        }
        Map<SocialClass, Integer> socialStructure = computeSocialStructureForPlayer(xunguiList, player);
        sourceStack.sendSuccess(() -> formatSocialStructure(socialStructure), true);
        return 0;
    }

    private Component formatSocialStructure(Map<SocialClass, Integer> socialStructure) {
        MutableComponent formattedSocialStructure = Component.translatable("cft.socialstrcture.message")
                .withStyle(ChatFormatting.BLUE)
                .append(Component.literal("\n"));
        socialStructure.forEach((key, value) -> formattedSocialStructure.append(Component.translatable(key.getId())
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("    "))
                .append(Component.literal(value.toString()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("\n")));
        return formattedSocialStructure;
    }

    private Map<SocialClass, Integer> computeSocialStructureForPlayer(List<XunguiEntity> xunguiList, ServerPlayer player) {
        Map<SocialClass, Integer> socialStructure = new HashMap<>();
        for (XunguiEntity xungui : xunguiList) {
            if (xungui.getLeaderId().equals(player.getUUID())) {
                if (socialStructure.containsKey(xungui.getSocialClass())) {
                    socialStructure.replace(xungui.getSocialClass(), socialStructure.get(xungui.getSocialClass()) + 1);
                } else {
                    socialStructure.put(xungui.getSocialClass(), 1);
                }
            }
        }
        return socialStructure;
    }
}
