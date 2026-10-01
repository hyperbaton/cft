package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.HearingNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.util.RegistryEntries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.Optional;

/**
 * Satisfied while the Xoonglin has recently heard the right amount of some sounds: at least
 * {@code min_events} and, if given, at most {@code max_events} of them within the last
 * {@code window} seconds. It hears both sounds played on the server and game events (vibrations),
 * since each covers sounds the other misses, like jukeboxes (game events) or bells (sounds).
 */
public class HearingNeed extends Need {

    public static final Codec<HearingNeed> HEARING_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            propertiesCodec(),
            RegistryEntries.codec(Registries.SOUND_EVENT).optionalFieldOf("sounds", RegistryEntries.empty()).forGetter(HearingNeed::getSounds),
            RegistryEntries.codec(Registries.GAME_EVENT).optionalFieldOf("game_events", RegistryEntries.empty()).forGetter(HearingNeed::getGameEvents),
            Codec.INT.optionalFieldOf("min_events", 1).forGetter(HearingNeed::getMinEvents),
            Codec.INT.optionalFieldOf("max_events").forGetter(HearingNeed::getMaxEvents),
            Codec.INT.optionalFieldOf("window", 60).forGetter(HearingNeed::getWindow)
    ).apply(instance, HearingNeed::new));

    private final RegistryEntries<SoundEvent> sounds;
    private final RegistryEntries<GameEvent> gameEvents;
    private final int minEvents;
    private final Integer maxEvents;
    /** In seconds */
    private final int window;

    public HearingNeed(Properties properties, RegistryEntries<SoundEvent> sounds,
                       RegistryEntries<GameEvent> gameEvents, int minEvents, Optional<Integer> maxEvents,
                       int window) {
        super(properties);
        this.sounds = sounds;
        this.gameEvents = gameEvents;
        this.minEvents = Math.max(0, minEvents);
        this.maxEvents = maxEvents.orElse(null);
        this.window = Math.max(1, window);
    }

    public boolean isSatisfiedBy(int heardEvents) {
        return heardEvents >= minEvents && (maxEvents == null || heardEvents <= maxEvents);
    }

    /** How many of the latest events are worth remembering: more never change whether it's satisfied. */
    public int getEventsToRemember() {
        return Math.max(1, maxEvents != null ? maxEvents + 1 : minEvents);
    }

    public RegistryEntries<SoundEvent> getSounds() {
        return sounds;
    }

    public RegistryEntries<GameEvent> getGameEvents() {
        return gameEvents;
    }

    public int getMinEvents() {
        return minEvents;
    }

    public Optional<Integer> getMaxEvents() {
        return Optional.ofNullable(maxEvents);
    }

    public int getWindow() {
        return window;
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.hearing").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return List.of(ResourceLocation.withDefaultNamespace("jukebox"));
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.HEARING_NEED.get();
    }

    @Override
    public NeedSatisfier<HearingNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new HearingNeedSatisfier(satisfaction, isSatisfied, this);
    }
}
