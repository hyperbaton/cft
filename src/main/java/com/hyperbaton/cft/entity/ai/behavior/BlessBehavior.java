package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.BlesserJob;
import com.hyperbaton.cft.job.EffectApplication;
import com.hyperbaton.cft.job.ItemQuantity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
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
 * Drives a blesser: keep a stock of supplies from the base (structure or home), seek the
 * nearest target within the radius of the base missing one of its configured effects,
 * walk to it and bless it on a cooldown, spending one dose of items per blessing.
 */
public class BlessBehavior extends Behavior<XoonglinEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    private static final double BLESS_REACH = 2.5;
    private static final int MAX_NAV_FAILURES = 5;
    // Ticks to idle before scanning again when there is nothing to do
    private static final int IDLE_WAIT = 40;
    // Ticks to wait before re-checking the base when it lacks supplies
    private static final int RESTOCK_COOLDOWN = 600;

    private enum State {
        FETCHING, SEEKING, BLESSING, WAITING
    }

    private State state;
    private BlockPos basePos;
    private BlockPos fetchPos;
    private UUID targetId;
    private int repathTimer;
    private int waitTicks;
    private int blessCooldown;
    private int navFailures;

    public BlessBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getBlesserJob(entity) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        state = State.SEEKING;
        basePos = null;
        fetchPos = null;
        targetId = null;
        repathTimer = 0;
        waitTicks = 0;
        blessCooldown = 0;
        navFailures = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_BLESS.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        BlesserJob job = getBlesserJob(entity);
        if (job == null) return;

        basePos = computeBase(entity, job);
        if (basePos == null) return;

        if (blessCooldown > 0) blessCooldown--;

        // Whatever we are doing, divert to restock if we cannot bless and the base can supply us
        if (state != State.FETCHING && !job.hasDose(entity) && baseHasSupplies(level, entity, job)) {
            state = State.FETCHING;
            fetchPos = null;
            repathTimer = 0;
            navFailures = 0;
        }

        switch (state) {
            case FETCHING -> tickFetching(level, entity, job);
            case SEEKING -> tickSeeking(level, entity, job);
            case BLESSING -> tickBlessing(level, entity, job);
            case WAITING -> tickWaiting();
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        targetId = null;
    }

    private void tickFetching(ServerLevel level, XoonglinEntity entity, BlesserJob job) {
        List<Container> containers = baseContainers(level, entity, job);
        if (containers.isEmpty() || !baseHasSupplies(level, entity, job)) {
            LOGGER.warn("[Bless] {} has no supplies at base {}, waiting",
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

    private void tickSeeking(ServerLevel level, XoonglinEntity entity, BlesserJob job) {
        if (!job.hasDose(entity)) {
            // No supplies on hand, and the base couldn't restock us either (checked in
            // tick()); don't go chasing a target we can't actually bless yet.
            state = State.WAITING;
            waitTicks = IDLE_WAIT;
            return;
        }
        LivingEntity target = findNearestTarget(level, entity, job);
        if (target == null) {
            state = State.WAITING;
            waitTicks = IDLE_WAIT;
            return;
        }
        targetId = target.getUUID();
        state = State.BLESSING;
        repathTimer = 0;
        navFailures = 0;
        navigateTo(entity, target.blockPosition());
    }

    private void tickBlessing(ServerLevel level, XoonglinEntity entity, BlesserJob job) {
        LivingEntity target = resolveTarget(level, entity, job);
        if (target == null) {
            targetId = null;
            state = State.SEEKING;
            return;
        }

        double distance = entity.position().distanceTo(target.position());
        if (distance > BLESS_REACH) {
            if (entity.getNavigation().isDone()) {
                if (++navFailures >= MAX_NAV_FAILURES) {
                    navFailures = 0;
                    targetId = null;
                    state = State.SEEKING;
                    return;
                }
                repathTimer = 0;
                navigateTo(entity, target.blockPosition());
            } else if (++repathTimer >= REPATH_INTERVAL) {
                repathTimer = 0;
                navigateTo(entity, target.blockPosition());
            }
            return;
        }

        entity.getNavigation().stop();
        // BlockPosTracker (not EntityTracker): EntityTracker.isVisibleBy reads the
        // visible_mobs memory, which xoonglin brains do not register, and would crash
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,
                new BlockPosTracker(target.getEyePosition()));

        if (blessCooldown > 0) return;
        if (!job.hasDose(entity)) {
            state = State.SEEKING;
            return;
        }

        job.consumeDose(entity);
        for (EffectApplication application : job.getEffects()) {
            Holder<MobEffect> holder = resolveEffect(application);
            if (holder == null) continue;
            target.addEffect(new MobEffectInstance(holder, application.duration(), application.amplifier()));
        }
        entity.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                target.getX(), target.getEyeY(), target.getZ(),
                6, 0.4, 0.4, 0.4, 0.1);
        blessCooldown = job.getCooldown();

        if (!needsBlessing(target, job)) {
            targetId = null;
            state = State.SEEKING;
        }
    }

    private void tickWaiting() {
        if (--waitTicks <= 0) {
            state = State.SEEKING;
        }
    }

    private Holder<MobEffect> resolveEffect(EffectApplication application) {
        return BuiltInRegistries.MOB_EFFECT
                .getHolder(ResourceKey.create(Registries.MOB_EFFECT, application.effect()))
                .orElse(null);
    }

    /** True if the target is missing at least one of the job's configured effects. */
    private boolean needsBlessing(LivingEntity target, BlesserJob job) {
        for (EffectApplication application : job.getEffects()) {
            Holder<MobEffect> holder = resolveEffect(application);
            if (holder != null && !target.hasEffect(holder)) return true;
        }
        return false;
    }

    private LivingEntity resolveTarget(ServerLevel level, XoonglinEntity entity, BlesserJob job) {
        if (targetId == null) return null;
        Entity found = level.getEntity(targetId);
        if (!(found instanceof LivingEntity target) || !target.isAlive()) return null;
        if (!needsBlessing(target, job)) return null;
        if (target.blockPosition().distManhattan(basePos) > job.getRadius() * 2L) return null;
        return target;
    }

    private LivingEntity findNearestTarget(ServerLevel level, XoonglinEntity entity, BlesserJob job) {
        AABB area = new AABB(basePos).inflate(job.getRadius());
        UUID leaderId = entity.getLeaderId();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (XoonglinEntity candidate : level.getEntitiesOfClass(XoonglinEntity.class, area)) {
            if (candidate.getUUID().equals(entity.getUUID())) continue;
            if (leaderId == null || !leaderId.equals(candidate.getLeaderId())) continue;
            if (!needsBlessing(candidate, job)) continue;
            double dist = entity.position().distanceToSqr(candidate.position());
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }

        // Optionally bless the leader player, if within the base radius and missing an effect
        if (job.isBlessPlayer() && leaderId != null) {
            Player player = level.getPlayerByUUID(leaderId);
            if (player != null && player.isAlive() && needsBlessing(player, job)
                    && player.blockPosition().distManhattan(basePos) <= job.getRadius()) {
                double dist = entity.position().distanceToSqr(player.position());
                if (dist < bestDist) {
                    best = player;
                }
            }
        }
        return best;
    }

    private BlockPos computeBase(XoonglinEntity entity, BlesserJob job) {
        if (job.getRequiredStructureType() != null) {
            return entity.getAssignedStructurePos(job.getRequiredStructureType());
        }
        return entity.getHome() != null ? entity.getHome().getEntrance() : null;
    }

    private Structure structureOrNull(ServerLevel level, XoonglinEntity entity, BlesserJob job) {
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
    private List<BlockPos> baseContainerPositions(ServerLevel level, XoonglinEntity entity, BlesserJob job) {
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
    private List<Container> baseContainers(ServerLevel level, XoonglinEntity entity, BlesserJob job) {
        List<Container> containers = new ArrayList<>();
        for (BlockPos pos : baseContainerPositions(level, entity, job)) {
            if (level.getBlockEntity(pos) instanceof Container container) {
                containers.add(container);
            }
        }
        return containers;
    }

    private boolean baseHasSupplies(ServerLevel level, XoonglinEntity entity, BlesserJob job) {
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

    private void takeSupplies(XoonglinEntity entity, BlesserJob job, List<Container> containers) {
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

    private BlesserJob getBlesserJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof BlesserJob b ? b : null;
    }
}
