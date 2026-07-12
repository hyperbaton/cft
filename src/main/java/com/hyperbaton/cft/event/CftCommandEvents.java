package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.commands.HappinessLadderCommand;
import com.hyperbaton.cft.commands.PopulationLadderCommand;
import com.hyperbaton.cft.commands.RosterCommand;
import com.hyperbaton.cft.commands.SocialStructureCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(modid = CftMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CftCommandEvents {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new SocialStructureCommand(event.getDispatcher());
        new PopulationLadderCommand(event.getDispatcher());
        new HappinessLadderCommand(event.getDispatcher());
        new RosterCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }
}
