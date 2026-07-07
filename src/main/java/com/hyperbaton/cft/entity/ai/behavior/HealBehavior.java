package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.HealerJob;
import com.hyperbaton.cft.job.ItemQuantity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drives a healer: keep a stock of healing items from the base (structure or home),
 * seek the nearest damaged patient within the radius of the base, walk to it and heal
 * it on a cooldown, spending one dose of items per heal.
 */
public class HealBehavior extends Behavior<XoonglinEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    private static final double HEAL_REACH = 2.5;
    private static final int MAX_NAV_FAILURES = 5;
    // Ticks to idle before scanning again when there is nothing to do
    private static final int IDLE_WAIT = 40;
    // Ticks to wait before re-checking the base when it lacks supplies
    private static final int RESTOCK_COOLDOWN = 600;

    private enum State {
        FETCHING, SEEKING, TREATING, WAITING
    }

    private State state;
    private BlockPos basePos;
    private BlockPos fetchPos;
    private UUID patientId;
    private int repathTimer;
    private int waitTicks;
    private int healCooldown;
    private int navFailures;

    public HealBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getHealerJob(entity) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        state = State.SEEKING;
        basePos = null;
        fetchPos = null;
        patientId = null;
        repathTimer = 0;
        waitTicks = 0;
        healCooldown = 0;
        navFailures = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_HEAL.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        HealerJob job = getHealerJob(entity);
        if (job == null) return;

        basePos = computeBase(entity, job);
        if (basePos == null) return;

        if (healCooldown > 0) healCooldown--;

        // Whatever we are doing, divert to restock if we cannot heal and the base can supply us
        if (state != State.FETCHING && !job.hasDose(entity) && baseHasSupplies(level, entity, job)) {
            state = State.FETCHING;
            fetchPos = null;
            repathTimer = 0;
            navFailures = 0;
        }

        switch (state) {
            case FETCHING -> tickFetching(level, entity, job);
            case SEEKING -> tickSeeking(level, entity, job);
            case TREATING -> tickTreating(level, entity, job);
            case WAITING -> tickWaiting();
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        patientId = null;
    }

    private void tickFetching(ServerLevel level, XoonglinEntity entity, HealerJob job) {
        List<Container> containers = baseContainers(level, entity, job);
        if (containers.isEmpty() || !baseHasSupplies(level, entity, job)) {
            LOGGER.warn("[Heal] {} has no supplies at base {}, waiting",
                    entity.getName().getString(), basePos);
            state = State.WAITING;
            waitTicks = RESTOCK_COOLDOWN;
            return;
        }

        if (fetchPos == null) {
            fetchPos = baseContainerPositions(level, entity, job).stream().findFirst().orElse(basePos);
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(fetchPos))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            entity.getNavigation().stop();
            takeSupplies(entity, job, containers);
            containers.forEach(Container::setChanged);
            state = State.SEEKING;
            return;
        }

        if (entity.getNavigation().isDone()) {
            if (++navFailures >= MAX_NAV_FAILURES) {
                navFailures = 0;
                // Can't reach the container; take supplies remotely rather than looping
                takeSupplies(entity, job, containers);
                containers.forEach(Container::setChanged);
                state = State.SEEKING;
                return;
            }
            repathTimer = 0;
            navigateTo(entity, fetchPos);
        } else if (++repathTimer >= REPATH_INTERVAL) {
            repathTimer = 0;
            navigateTo(entity, fetchPos);
        }
    }

    private void tickSeeking(ServerLevel level, XoonglinEntity entity, HealerJob job) {
        LivingEntity patient = findNearestPatient(level, entity, job);
        if (patient == null) {
            state = State.WAITING;
            waitTicks = IDLE_WAIT;
            return;
        }
        patientId = patient.getUUID();
        state = State.TREATING;
        repathTimer = 0;
        navFailures = 0;
        navigateTo(entity, patient.blockPosition());
    }

    private void tickTreating(ServerLevel level, XoonglinEntity entity, HealerJob job) {
        LivingEntity patient = resolvePatient(level, entity, job);
        if (patient == null) {
            patientId = null;
            state = State.SEEKING;
            return;
        }

        double distance = entity.position().distanceTo(patient.position());
        if (distance > HEAL_REACH) {
            if (entity.getNavigation().isDone()) {
                if (++navFailures >= MAX_NAV_FAILURES) {
                    navFailures = 0;
                    patientId = null;
                    state = State.SEEKING;
                    return;
                }
                repathTimer = 0;
                navigateTo(entity, patient.blockPosition());
            } else if (++repathTimer >= REPATH_INTERVAL) {
                repathTimer = 0;
                navigateTo(entity, patient.blockPosition());
            }
            return;
        }

        entity.getNavigation().stop();
        // BlockPosTracker (not EntityTracker): EntityTracker.isVisibleBy reads the
        // visible_mobs memory, which xoonglin brains do not register, and would crash
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,
                new BlockPosTracker(patient.getEyePosition()));

        if (healCooldown > 0) return;
        if (!job.hasDose(entity)) {
            state = State.SEEKING;
            return;
        }

        job.consumeDose(entity);
        patient.heal((float) job.getHealAmount());
        entity.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.HEART,
                patient.getX(), patient.getEyeY(), patient.getZ(),
                6, 0.4, 0.4, 0.4, 0.1);
        healCooldown = job.getCooldown();

        if (patient.getHealth() >= patient.getMaxHealth()) {
            patientId = null;
            state = State.SEEKING;
        }
    }

    private void tickWaiting() {
        if (--waitTicks <= 0) {
            state = State.SEEKING;
        }
    }

    private LivingEntity resolvePatient(ServerLevel level, XoonglinEntity entity, HealerJob job) {
        if (patientId == null) return null;
        Entity found = level.getEntity(patientId);
        if (!(found instanceof LivingEntity patient) || !patient.isAlive()) return null;
        if (patient.getHealth() >= patient.getMaxHealth()) return null;
        if (patient.blockPosition().distManhattan(basePos) > job.getRadius() * 2L) return null;
        return patient;
    }

    private LivingEntity findNearestPatient(ServerLevel level, XoonglinEntity entity, HealerJob job) {
        AABB area = new AABB(basePos).inflate(job.getRadius());
        UUID leaderId = entity.getLeaderId();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (XoonglinEntity candidate : level.getEntitiesOfClass(XoonglinEntity.class, area)) {
            if (candidate.getUUID().equals(entity.getUUID())) continue;
            if (leaderId == null || !leaderId.equals(candidate.getLeaderId())) continue;
            if (candidate.getHealth() >= candidate.getMaxHealth()) continue;
            double dist = entity.position().distanceToSqr(candidate.position());
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }

        // Optionally treat the leader player, if damaged and within the base radius
        if (job.isHealPlayer() && leaderId != null) {
            Player player = level.getPlayerByUUID(leaderId);
            if (player != null && player.isAlive() && player.getHealth() < player.getMaxHealth()
                    && player.blockPosition().distManhattan(basePos) <= job.getRadius()) {
                double dist = entity.position().distanceToSqr(player.position());
                if (dist < bestDist) {
                    best = player;
                }
            }
        }
        return best;
    }

    private BlockPos computeBase(XoonglinEntity entity, HealerJob job) {
        if (job.getRequiredStructureType() != null) {
            return entity.getAssignedStructurePos(job.getRequiredStructureType());
        }
        return entity.getHome() != null ? entity.getHome().getEntrance() : null;
    }

    private Structure structureOrNull(ServerLevel level, XoonglinEntity entity, HealerJob job) {
        if (job.getRequiredStructureType() == null) return null;
        BlockPos assigned = entity.getAssignedStructurePos(job.getRequiredStructureType());
        if (assigned == null) return null;
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        return data.getStructures().stream()
                .filter(s -> s.getKeyBlockPos().equals(assigned))
                .filter(s -> s.getStructureTypeId().equals(job.getRequiredStructureType()))
                .findFirst().orElse(null);
    }

    /** Container positions of the base: the structure's, or the home's if none. */
    private List<BlockPos> baseContainerPositions(ServerLevel level, XoonglinEntity entity, HealerJob job) {
        Structure structure = structureOrNull(level, entity, job);
        if (structure != null) {
            return ContainerUtil.findContainerPositions(level, structure);
        }
        List<BlockPos> positions = new ArrayList<>();
        if (entity.getHome() != null) {
            for (BlockPos pos : entity.getHome().getInteriorBlocks()) {
                if (level.getBlockEntity(pos) instanceof Container) {
                    positions.add(pos);
                }
            }
        }
        return positions;
    }

    /** Containers of the base: the structure's, or the home's if there is no structure. */
    private List<Container> baseContainers(ServerLevel level, XoonglinEntity entity, HealerJob job) {
        List<Container> containers = new ArrayList<>();
        for (BlockPos pos : baseContainerPositions(level, entity, job)) {
            if (level.getBlockEntity(pos) instanceof Container container) {
                containers.add(container);
            }
        }
        return containers;
    }

    private boolean baseHasSupplies(ServerLevel level, XoonglinEntity entity, HealerJob job) {
        if (job.getItems().isEmpty()) return false;
        List<Container> containers = baseContainers(level, entity, job);
        for (ItemQuantity item : job.getItems()) {
            int found = 0;
            for (Container container : containers) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty() && item.ingredient().test(stack)) {
                        found += stack.getCount();
                    }
                }
            }
            if (found < item.quantity()) {
                return false;
            }
        }
        return true;
    }

    private void takeSupplies(XoonglinEntity entity, HealerJob job, List<Container> containers) {
        for (ItemQuantity item : job.getItems()) {
            int wanted = item.quantity() * job.getDosesPerFetch();
            for (Container container : containers) {
                for (int i = 0; i < container.getContainerSize() && wanted > 0; i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty() || !item.ingredient().test(stack)) continue;
                    int take = Math.min(wanted, stack.getCount());
                    ItemStack taken = stack.copyWithCount(take);
                    ItemStack leftover = entity.getInventory().addItem(taken);
                    int actuallyTaken = take - leftover.getCount();
                    stack.shrink(actuallyTaken);
                    wanted -= actuallyTaken;
                    if (!leftover.isEmpty()) {
                        wanted = 0; // inventory full
                    }
                }
            }
        }
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private HealerJob getHealerJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof HealerJob h ? h : null;
    }
}
