package com.hyperbaton.cft.entity.ai;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lets addons extend the Xoonglin brain without changing CFT: posted on the NeoForge event bus
 * ({@code NeoForge.EVENT_BUS}) while the brain is built, on both the server and the client.
 *
 * <p>For example, a new need can register the memory its behavior needs with
 * {@link RegisterMemories}, add that behavior to the INVESTIGATE activity with
 * {@link AddBehaviors}, and send the Xoonglin there by starting an errand with
 * {@link ErrandUtils}.
 */
public abstract class XoonglinBrainEvent extends Event {

    /**
     * Posted once, the first time a Xoonglin brain is built: add the memories and sensors your
     * behaviors use. The brain ignores any memory it wasn't built with.
     */
    public static class RegisterMemories extends XoonglinBrainEvent {
        private final List<MemoryModuleType<?>> memories;
        private final List<SensorType<? extends Sensor<? super XoonglinEntity>>> sensors;

        RegisterMemories(List<MemoryModuleType<?>> memories,
                         List<SensorType<? extends Sensor<? super XoonglinEntity>>> sensors) {
            this.memories = memories;
            this.sensors = sensors;
        }

        public void addMemory(MemoryModuleType<?> memory) {
            if (!memories.contains(memory)) memories.add(memory);
        }

        public void addSensor(SensorType<? extends Sensor<? super XoonglinEntity>> sensor) {
            if (!sensors.contains(sensor)) sensors.add(sensor);
        }
    }

    /**
     * Posted for every Xoonglin brain built: add behaviors to its activities. Create a new
     * behavior each time, since behaviors keep the state of the Xoonglin running them.
     *
     * <p>CFT picks the activity (WORK, INVESTIGATE, IDLE, REST, its own MATE...), so add
     * behaviors to one of those: a behavior in another activity never runs.
     */
    public static class AddBehaviors extends XoonglinBrainEvent {
        private final Map<Activity, List<Pair<Integer, ? extends BehaviorControl<? super XoonglinEntity>>>> behaviors;

        AddBehaviors(Map<Activity, List<Pair<Integer, ? extends BehaviorControl<? super XoonglinEntity>>>> behaviors) {
            this.behaviors = behaviors;
        }

        /**
         * @param priority lower values start first; behaviors of the active activity whose
         *                 conditions hold all run at once, so it only orders their starting
         */
        public void addBehavior(Activity activity, int priority, BehaviorControl<? super XoonglinEntity> behavior) {
            behaviors.computeIfAbsent(activity, a -> new ArrayList<>()).add(Pair.of(priority, behavior));
        }
    }
}
