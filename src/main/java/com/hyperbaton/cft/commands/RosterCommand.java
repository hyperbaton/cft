package com.hyperbaton.cft.commands;

import com.hyperbaton.cft.world.BookEntry;
import com.hyperbaton.cft.world.RostersData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class RosterCommand {
    public RosterCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bookRoster")
                        .executes(command -> getRoster(command.getSource())));
    }

    private int getRoster(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        RostersData rosters = level.getDataStorage().computeIfAbsent(RostersData.factory(), "rostersData");
        source.sendSuccess(() -> formatRoster(level, rosters), true);
        return 0;
    }

    private Component formatRoster(ServerLevel level, RostersData rosters) {
        MutableComponent formatted = Component.translatable("cft.roster.header")
                .append(Component.literal("\n"));
        for (ServerPlayer player : level.players()) {
            List<BookEntry> entries = rosters.findByLeader(player.getUUID());
            formatted.append(player.getDisplayName())
                    .append(Component.literal(" (" + entries.size() + "):\n"));
            for (BookEntry entry : entries) {
                formatted.append(Component.literal("  " + entry.title() + " by " + entry.authorName() + "\n"));
            }
        }
        return formatted;
    }
}
