package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public abstract class Need {

    public static final Codec<Need> NEED_CODEC = Codec.lazyInitialized(() -> CftRegistry.NEEDS_CODEC_REGISTRY.byNameCodec()
            .dispatch("type", Need::needType, codec -> MapCodec.assumeMapUnsafe(codec)));

    /**
     * The fields every need has, read along with the need's own fields (they're in the same JSON
     * object). A need type's codec includes them with {@link #propertiesCodec()} and passes them to
     * the {@link Need} constructor, as vanilla blocks do with their properties.
     *
     * @param damage                the damage it deals while below {@code damageThreshold}
     * @param providedHappiness     the happiness it gives if satisfied over a full period, given by its frequency
     * @param frequency             its period, in in-game days (each day is 24000 ticks or 20 real world minutes)
     * @param hidden                whether it's kept from the player, for "technical" needs
     * @param bonus                 bonus needs only add happiness when satisfied, and never subtract it
     * @param icon                  the item shown as its icon, instead of the type's default ones
     */
    public record Properties(double damage, double damageThreshold, double providedHappiness,
                             double satisfactionThreshold, double frequency, boolean hidden, boolean bonus,
                             Optional<ResourceLocation> icon) {
        public static final MapCodec<Properties> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.fieldOf("damage").forGetter(Properties::damage),
                Codec.DOUBLE.fieldOf("damage_threshold").forGetter(Properties::damageThreshold),
                Codec.DOUBLE.fieldOf("provided_happiness").forGetter(Properties::providedHappiness),
                Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(Properties::satisfactionThreshold),
                Codec.DOUBLE.fieldOf("frequency").forGetter(Properties::frequency),
                Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Properties::hidden),
                Codec.BOOL.optionalFieldOf("bonus", false).forGetter(Properties::bonus),
                ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(Properties::icon)
        ).apply(instance, Properties::new));
    }

    /** The fields every need has, for a need type's codec; it takes a single field of the codec. */
    protected static <N extends Need> RecordCodecBuilder<N, Properties> propertiesCodec() {
        return Properties.MAP_CODEC.forGetter(Need::getProperties);
    }

    private final Properties properties;

    protected Need(Properties properties) {
        this.properties = properties;
    }

    public abstract Codec<? extends Need> needType();

    public Properties getProperties() {
        return properties;
    }

    public double getDamage() {
        return properties.damage();
    }

    public double getDamageThreshold() {
        return properties.damageThreshold();
    }

    public double getProvidedHappiness() {
        return properties.providedHappiness();
    }

    public double getSatisfactionThreshold() {
        return properties.satisfactionThreshold();
    }

    public double getFrequency() {
        return properties.frequency();
    }

    public boolean isHidden() {
        return properties.hidden();
    }

    public boolean isBonus() {
        return properties.bonus();
    }

    public NeedSatisfier<? extends Need> createSatisfier() {
        return createSatisfier(this.getSatisfactionThreshold(), false);
    }

    public abstract NeedSatisfier<? extends Need> createSatisfier(double satisfaction, boolean isSatisfied);

    public abstract List<ResourceLocation> getDefaultIcons();

    public Optional<ResourceLocation> getIcon() {
        return properties.icon();
    }

    public List<ResourceLocation> getIcons() {
        return properties.icon().map(List::of).orElseGet(this::getDefaultIcons);
    }

    public abstract String getTypeName();
}
