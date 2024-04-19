package com.hyperbaton.cft.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hyperbaton.cft.entity.behaviour.GetSuppliesBehaviour;
import com.hyperbaton.cft.entity.behaviour.SearchHomeBehaviour;
import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.memory.CftMemoryModuleType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Map;

public class XunguiAi {
    public static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            CftMemoryModuleType.HOME_CONTAINER_POSITION.get(),
            CftMemoryModuleType.SUPPLIES_NEEDED.get()
    );
    public static final ImmutableList<? extends SensorType<? extends Sensor<? super XunguiEntity>>> SENSOR_TYPES = ImmutableList.of();

    public static Brain<?> makeBrain(Brain<XunguiEntity> pBrain) {
        initCoreActivity(pBrain);
        initIdleActivity(pBrain);
        // TODO: Create a proper activity for this
        initInvestigateActivity(pBrain);
        pBrain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        pBrain.setDefaultActivity(Activity.IDLE);
        pBrain.useDefaultActivity();
        return pBrain;
    }

    private static void initCoreActivity(Brain<XunguiEntity> pBrain) {
        pBrain.addActivity(Activity.CORE, 2, ImmutableList.of(
                new Swim(0.8F),
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink()));
    }

    private static void initIdleActivity(Brain<XunguiEntity> pBrain) {
        pBrain.addActivity(Activity.IDLE, ImmutableList.of(
                Pair.of(3, SetEntityLookTargetSometimes.create(EntityType.PLAYER, 6.0F, UniformInt.of(30, 60))),
                Pair.of(4, new RandomLookAround(UniformInt.of(150, 250), 30.0F, 0.0F, 0.0F)),
                Pair.of(5, new RunOne(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                        ImmutableList.of(
                                Pair.of(RandomStroll.stroll(2.0F), 1),
                                Pair.of(SetWalkTargetFromLookTarget.create(2.0F, 3), 1),
                                Pair.of(new DoNothing(30, 60), 1))))));
    }

    private static void initInvestigateActivity(Brain<XunguiEntity> pBrain) {
        pBrain.addActivity(Activity.INVESTIGATE, ImmutableList.of(
                Pair.of(1, new GetSuppliesBehaviour(
                        Map.of(CftMemoryModuleType.HOME_CONTAINER_POSITION.get(), MemoryStatus.VALUE_PRESENT,
                                CftMemoryModuleType.SUPPLIES_NEEDED.get(), MemoryStatus.VALUE_PRESENT)
                )),
                Pair.of(0, new SearchHomeBehaviour(
                        Map.of()
                ))
        ));
    }
}
