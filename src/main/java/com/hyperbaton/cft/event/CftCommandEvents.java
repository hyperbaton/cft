package com.hyperbaton.cft.event;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.commands.HappinessLadderCommand;
import com.hyperbaton.cft.commands.PopulationLadderCommand;
import com.hyperbaton.cft.commands.SocialStructureCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;

@Mod.EventBusSubscriber(modid = CftMod.MOD_ID)
public class CftCommandEvents {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new SocialStructureCommand(event.getDispatcher());
        new PopulationLadderCommand(event.getDispatcher());
        new HappinessLadderCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }
}
