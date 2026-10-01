package com.hyperbaton.cft.commands;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.CftEntities;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.spawner.XoonglinSpawner;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /xoonglin summon [pos] [class] [leader]}: summons a Xoonglin set up like a spawned one.
 * Without a class it gets a random one; without a leader, whoever runs the command leads it.
 */
public class XoonglinCommand {

    private static final SuggestionProvider<CommandSourceStack> SOCIAL_CLASSES = (context, builder) ->
            SharedSuggestionProvider.suggestResource(CftRegistry.SOCIAL_CLASSES.keySet(), builder);

    private static final DynamicCommandExceptionType UNKNOWN_CLASS = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.cft.xoonglin.unknown_class", String.valueOf(id)));
    private static final SimpleCommandExceptionType NO_CLASSES = new SimpleCommandExceptionType(
            Component.translatable("commands.cft.xoonglin.no_classes"));
    private static final SimpleCommandExceptionType NO_LEADER = new SimpleCommandExceptionType(
            Component.translatable("commands.cft.xoonglin.no_leader"));
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(
            Component.translatable("commands.summon.invalidPosition"));
    private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(
            Component.translatable("commands.summon.failed"));

    public XoonglinCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("xoonglin")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("summon")
                        .executes(context -> summon(context.getSource(), context.getSource().getPosition(), null, null))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(context -> summon(context.getSource(),
                                        Vec3Argument.getVec3(context, "pos"), null, null))
                                .then(Commands.argument("class", ResourceLocationArgument.id())
                                        .suggests(SOCIAL_CLASSES)
                                        .executes(context -> summon(context.getSource(),
                                                Vec3Argument.getVec3(context, "pos"),
                                                ResourceLocationArgument.getId(context, "class"), null))
                                        .then(Commands.argument("leader", EntityArgument.player())
                                                .executes(context -> summon(context.getSource(),
                                                        Vec3Argument.getVec3(context, "pos"),
                                                        ResourceLocationArgument.getId(context, "class"),
                                                        EntityArgument.getPlayer(context, "leader"))))))));
    }

    private static int summon(CommandSourceStack source, Vec3 pos, @Nullable ResourceLocation classId,
                              @Nullable ServerPlayer leader) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        if (!Level.isInSpawnableBounds(BlockPos.containing(pos))) {
            throw INVALID_POSITION.create();
        }

        SocialClass socialClass;
        if (classId != null) {
            socialClass = CftRegistry.SOCIAL_CLASSES.get(classId);
            if (socialClass == null) throw UNKNOWN_CLASS.create(classId);
        } else {
            socialClass = CftRegistry.SOCIAL_CLASSES.getRandom(level.random)
                    .orElseThrow(NO_CLASSES::create).value();
        }

        ServerPlayer leaderPlayer = leader != null ? leader : source.getPlayer();
        if (leaderPlayer == null) throw NO_LEADER.create();

        XoonglinEntity xoonglin = CftEntities.XOONGLIN.get().create(level);
        if (xoonglin == null) throw FAILED.create();
        xoonglin.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        // Set up first, so finalizeSpawn doesn't pick a random class of its own
        XoonglinSpawner.setUpXoonglin(xoonglin, socialClass, leaderPlayer.getUUID());
        EventHooks.finalizeMobSpawn(xoonglin, level, level.getCurrentDifficultyAt(xoonglin.blockPosition()),
                MobSpawnType.COMMAND, null);
        if (!level.tryAddFreshEntityWithPassengers(xoonglin)) throw FAILED.create();

        Component className = Component.translatable(CftRegistry.getSocialClassId(socialClass).toString());
        source.sendSuccess(() -> Component.translatable("commands.cft.xoonglin.summon.success",
                xoonglin.getDisplayName(), className, leaderPlayer.getDisplayName()), true);
        return 1;
    }
}
