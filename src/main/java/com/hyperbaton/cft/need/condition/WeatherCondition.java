package com.hyperbaton.cft.need.condition;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.util.CodecUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The weather in the Xoonglin's dimension is one of the given ones. It's the dimension's weather,
 * as in the {@code /weather} command, even where it doesn't rain (deserts, or indoors).
 */
public record WeatherCondition(List<Weather> weather) implements NeedCondition {

    public static final Codec<WeatherCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtil.singleOrList(Weather.CODEC).fieldOf("weather").forGetter(WeatherCondition::weather)
    ).apply(instance, WeatherCondition::new));

    @Override
    public boolean test(XoonglinEntity xoonglin) {
        return weather.contains(Weather.of(xoonglin.level()));
    }

    @Override
    public Codec<? extends NeedCondition> conditionType() {
        return CftRegistry.WEATHER_CONDITION.get();
    }

    public enum Weather implements StringRepresentable {
        CLEAR, RAIN, THUNDER;

        public static final Codec<Weather> CODEC = StringRepresentable.fromEnum(Weather::values);

        public static Weather of(Level level) {
            if (level.isThundering()) return THUNDER;
            return level.isRaining() ? RAIN : CLEAR;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }
}
