package com.hyperbaton.cft.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hyperbaton.cft.entity.ai.activity.CftActivities;
import com.hyperbaton.cft.entity.ai.behavior.*;
import com.hyperbaton.cft.entity.ai.sensor.CftSensorTypes;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Map;

public class XoonglinAi {

    public static final ImmutableList<? extends SensorType<? extends Sensor<? super XoonglinEntity>>> SENSOR_TYPES = ImmutableList.of(
            CftSensorTypes.ABLE_TO_MATE.get(), CftSensorTypes.FIND_POTENTIAL_MATES.get()
    );

    public static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            CftMemoryModuleType.HOME_CONTAINER_POSITION.get(),
            CftMemoryModuleType.SUPPLIES_NEEDED.get(),
            CftMemoryModuleType.HOME_CANDIDATE_POSITION.get(),
            CftMemoryModuleType.SUPPLY_COOLDOWN.get(),
            CftMemoryModuleType.HOME_NEEDED.get(),
            CftMemoryModuleType.CAN_MATE.get(),
            CftMemoryModuleType.MATING_CANDIDATE.get(),
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET
    );

    public static Brain<?> makeBrain(Brain<XoonglinEntity> pBrain) {
        initCoreActivity(pBrain);
        initIdleActivity(pBrain);
        initInvestigateActivity(pBrain);
        initMateActivity(pBrain);
        pBrain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        pBrain.setDefaultActivity(Activity.IDLE);
        pBrain.useDefaultActivity();
        return pBrain;
    }

    private static void initCoreActivity(Brain<XoonglinEntity> pBrain) {
        pBrain.addActivity(Activity.CORE, 2, ImmutableList.of(
                new Swim(0.8F),
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink(),
                new OpenDoorBehavior()));
    }

    private static void initIdleActivity(Brain<XoonglinEntity> pBrain) {
        pBrain.addActivity(Activity.IDLE, ImmutableList.of(
                Pair.of(3, new RandomStrollBehavior(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT)))));
    }

    private static void initInvestigateActivity(Brain<XoonglinEntity> pBrain) {
        pBrain.addActivity(Activity.INVESTIGATE, ImmutableList.of(
                Pair.of(0, new FindAndClaimHomeBehavior()),
                Pair.of(1, new GetSuppliesBehavior(
                        Map.of(CftMemoryModuleType.HOME_CONTAINER_POSITION.get(), MemoryStatus.VALUE_PRESENT,
                                CftMemoryModuleType.SUPPLIES_NEEDED.get(), MemoryStatus.VALUE_PRESENT)
                ))
        ));
    }

    private static void initMateActivity(Brain<XoonglinEntity> pBrain) {
        pBrain.addActivity(CftActivities.MATE.get(), ImmutableList.of(
                Pair.of(0, new MateBehavior(Map.of(CftMemoryModuleType.MATING_CANDIDATE.get(), MemoryStatus.VALUE_PRESENT)))
        ));
    }
}
