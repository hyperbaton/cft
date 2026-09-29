package com.hyperbaton.cft.entity.ai.schedule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;

import java.util.List;

/**
 * A daily schedule as written in a datapack: a list of transitions, each switching to an
 * activity at a clock hour (0 to 24, where 6 is sunrise, as the Minecraft day starts then).
 * The last transition of the day lasts until the first one of the next day.
 * <p>
 * It is turned into a vanilla {@link Schedule}, the same kind villagers use, which is what the
 * Xoonglin's brain works with. The transitions are kept so the definition can be encoded
 * again, since social classes and jobs are synced to clients.
 */
public class ScheduleDefinition {

    public static final Codec<ScheduleDefinition> CODEC = Transition.CODEC.listOf()
            .xmap(ScheduleDefinition::new, ScheduleDefinition::getTransitions);

    public record Transition(double at, Activity activity) {
        public static final Codec<Transition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.doubleRange(0, 24).fieldOf("at").forGetter(Transition::at),
                BuiltInRegistries.ACTIVITY.byNameCodec().fieldOf("activity").forGetter(Transition::activity)
        ).apply(inst, Transition::new));

        /** Converts the clock hour into day time ticks, where 0 is sunrise (6:00). */
        int dayTime() {
            return (int) Math.round(((at - 6.0 + 24.0) % 24.0) * 1000.0) % 24000;
        }
    }

    private final List<Transition> transitions;
    private final Schedule schedule;

    public ScheduleDefinition(List<Transition> transitions) {
        this.transitions = List.copyOf(transitions);
        ScheduleBuilder builder = new ScheduleBuilder(new Schedule());
        this.transitions.forEach(transition -> builder.changeActivityAt(transition.dayTime(), transition.activity()));
        this.schedule = builder.build();
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    /** The vanilla schedule, unregistered, like the ones villagers use. */
    public Schedule getSchedule() {
        return schedule;
    }
}
