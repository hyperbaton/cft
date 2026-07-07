package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.FisherJob;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.*;

/**
 * Makes a fisher work: find a big-enough body of fished blocks within radius, stand at
 * the best spot (on the required structure if there is one, otherwise on the shore),
 * gaze at the water, roll the catch list periodically, and deliver the day's catch to
 * the structure's containers — or home — when the work day ends.
 */
public class FishBehavior extends Behavior<XoonglinEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    private static final double SPOT_REACH = 2.0;
    private static final int MAX_NAV_FAILURES = 5;
    // Ticks to wait before searching again when no suitable body of water is found
    private static final int SEARCH_RETRY_COOLDOWN = 600;
    // Vertical range around the search origin scanned for body blocks
    private static final int SEARCH_HEIGHT = 8;

    private enum State {
        FINDING_WATER, TRAVELING, FISHING, DEPOSITING
    }

    private State state;
    private List<BlockPos> bodyBlocks = List.of();
    private BlockPos fishingSpot;
    private BlockPos depositPos;
    private int repathTimer;
    private int waitTicks;
    private int catchTimer;
    private int lookTimer;
    private int navFailures;

    public FishBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getFisherJob(entity) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        state = State.FINDING_WATER;
        bodyBlocks = List.of();
        fishingSpot = null;
        depositPos = null;
        repathTimer = 0;
        waitTicks = 0;
        catchTimer = 0;
        lookTimer = 0;
        navFailures = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_FISH.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        FisherJob job = getFisherJob(entity);
        if (job == null) return;

        boolean shouldDeposit = job.hasCatchItems(entity)
                && (job.isQuotaDone(entity.getJobState()) || !job.canWork(entity));
        if (shouldDeposit && state != State.DEPOSITING) {
            state = State.DEPOSITING;
            depositPos = null;
            repathTimer = 0;
            navFailures = 0;
        }

        switch (state) {
            case FINDING_WATER -> tickFindingWater(level, entity, job);
            case TRAVELING -> tickTraveling(entity);
            case FISHING -> tickFishing(level, entity, job);
            case DEPOSITING -> tickDepositing(level, entity, job);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
    }

    private void tickFindingWater(ServerLevel level, XoonglinEntity entity, FisherJob job) {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        BlockPos origin = searchOrigin(entity, job);
        List<BlockPos> body = findBody(level, origin, job);
        if (body.isEmpty()) {
            waitTicks = SEARCH_RETRY_COOLDOWN;
            return;
        }

        BlockPos spot = findFishingSpot(level, entity, job, body);
        if (spot == null) {
            waitTicks = SEARCH_RETRY_COOLDOWN;
            return;
        }

        bodyBlocks = body;
        fishingSpot = spot;
        state = State.TRAVELING;
        repathTimer = 0;
        navFailures = 0;
        navigateTo(entity, fishingSpot);
    }

    private void tickTraveling(XoonglinEntity entity) {
        if (fishingSpot == null) {
            state = State.FINDING_WATER;
            return;
        }
        if (entity.position().distanceTo(Vec3.atCenterOf(fishingSpot)) < SPOT_REACH) {
            entity.getNavigation().stop();
            state = State.FISHING;
            catchTimer = 0;
            return;
        }
        if (entity.getNavigation().isDone()) {
            if (++navFailures >= MAX_NAV_FAILURES) {
                navFailures = 0;
                state = State.FINDING_WATER;
                waitTicks = 100;
                return;
            }
            repathTimer = 0;
            navigateTo(entity, fishingSpot);
        } else if (++repathTimer >= REPATH_INTERVAL) {
            repathTimer = 0;
            navigateTo(entity, fishingSpot);
        }
    }

    private void tickFishing(ServerLevel level, XoonglinEntity entity, FisherJob job) {
        if (fishingSpot == null || bodyBlocks.isEmpty()) {
            state = State.FINDING_WATER;
            return;
        }
        if (entity.position().distanceTo(Vec3.atCenterOf(fishingSpot)) > SPOT_REACH + 1.5) {
            state = State.TRAVELING;
            repathTimer = 0;
            return;
        }

        // Gaze over the water, shifting to a different point every couple of seconds
        if (++lookTimer >= 40) {
            lookTimer = 0;
            BlockPos gazeTarget = bodyBlocks.get(entity.getRandom().nextInt(bodyBlocks.size()));
            entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,
                    new BlockPosTracker(Vec3.atCenterOf(gazeTarget)));
        }

        if (++catchTimer >= job.getCatchInterval()) {
            catchTimer = 0;
            entity.swing(InteractionHand.MAIN_HAND);

            ItemStack caught = job.rollCatch(entity.getRandom());
            if (!caught.isEmpty()) {
                BlockPos splashPos = nearestBodyBlock(entity.blockPosition());
                if (splashPos != null) {
                    level.sendParticles(ParticleTypes.SPLASH,
                            splashPos.getX() + 0.5, splashPos.getY() + 0.9, splashPos.getZ() + 0.5,
                            8, 0.3, 0.1, 0.3, 0.1);
                }
                ItemStack leftover = entity.getInventory().addItem(caught);
                if (!leftover.isEmpty()) {
                    // Inventory is full: deliver what we have before continuing
                    JobUtil.dropAtHome(entity, leftover);
                    state = State.DEPOSITING;
                    depositPos = null;
                    repathTimer = 0;
                    navFailures = 0;
                }
            }
        }
    }

    private void tickDepositing(ServerLevel level, XoonglinEntity entity, FisherJob job) {
        if (!job.hasCatchItems(entity)) {
            state = State.FINDING_WATER;
            return;
        }

        List<Container> structureContainers = findStructureContainers(level, entity, job);

        if (depositPos == null) {
            if (!structureContainers.isEmpty()) {
                Structure structure = findAssignedStructure(level, entity, job);
                depositPos = ContainerUtil.findContainerPositions(level, structure).get(0);
            } else if (entity.getHome() != null && entity.getHome().getEntrance() != null) {
                depositPos = entity.getHome().getEntrance();
            } else {
                // Nowhere to deliver; drop the catch where we stand
                dropCatches(entity, job);
                state = State.FINDING_WATER;
                return;
            }
            repathTimer = 0;
            navigateTo(entity, depositPos);
            return;
        }

        boolean arrived = entity.position().distanceTo(Vec3.atCenterOf(depositPos))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get();
        if (!arrived) {
            if (entity.getNavigation().isDone()) {
                if (++navFailures >= MAX_NAV_FAILURES) {
                    // Can't reach the container; both deposit paths work remotely
                    navFailures = 0;
                    arrived = true;
                } else {
                    repathTimer = 0;
                    navigateTo(entity, depositPos);
                    return;
                }
            } else if (++repathTimer >= REPATH_INTERVAL) {
                repathTimer = 0;
                navigateTo(entity, depositPos);
                return;
            } else {
                return;
            }
        }

        entity.getNavigation().stop();
        SimpleContainer inventory = entity.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!job.isCatchItem(stack)) continue;

            ItemStack remainder;
            if (!structureContainers.isEmpty()) {
                remainder = ContainerUtil.insertIntoContainers(structureContainers, stack);
            } else {
                remainder = JobUtil.tryDepositAtHome(entity, stack);
            }
            if (!remainder.isEmpty()) {
                JobUtil.dropAtHome(entity, remainder);
            }
            inventory.setItem(i, ItemStack.EMPTY);
        }

        depositPos = null;
        state = State.FINDING_WATER;
    }

    private BlockPos searchOrigin(XoonglinEntity entity, FisherJob job) {
        if (job.getRequiredStructureType() != null) {
            BlockPos assigned = entity.getAssignedStructurePos(job.getRequiredStructureType());
            if (assigned != null) return assigned;
        }
        return entity.blockPosition();
    }

    /**
     * Finds the nearest body of fished blocks with at least the minimum size. The flood
     * fill is capped at that size — we only need to prove "big enough", so even an
     * ocean costs at most min_body_size visits per candidate.
     */
    private List<BlockPos> findBody(ServerLevel level, BlockPos origin, FisherJob job) {
        int radius = job.getRadius();
        List<BlockPos> candidates = new ArrayList<>();
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
            for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                for (int y = origin.getY() - SEARCH_HEIGHT; y <= origin.getY() + SEARCH_HEIGHT; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (job.isBodyBlock(level.getBlockState(pos))) {
                        candidates.add(pos);
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingInt(origin::distManhattan));

        Set<BlockPos> rejected = new HashSet<>();
        for (BlockPos candidate : candidates) {
            if (rejected.contains(candidate)) continue;
            Set<BlockPos> body = floodFillBody(level, candidate, job);
            if (body.size() >= job.getMinBodySize()) {
                return new ArrayList<>(body);
            }
            rejected.addAll(body);
        }
        return List.of();
    }

    private Set<BlockPos> floodFillBody(ServerLevel level, BlockPos start, FisherJob job) {
        Set<BlockPos> body = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        body.add(start);

        while (!queue.isEmpty() && body.size() < job.getMinBodySize()) {
            BlockPos current = queue.poll();
            for (BlockPos neighbor : List.of(current.north(), current.south(), current.east(),
                    current.west(), current.above(), current.below())) {
                if (!body.contains(neighbor) && job.isBodyBlock(level.getBlockState(neighbor))) {
                    body.add(neighbor);
                    queue.add(neighbor);
                    if (body.size() >= job.getMinBodySize()) break;
                }
            }
        }
        return body;
    }

    /**
     * Picks the standing position: on the required structure's own blocks (closest to
     * the water without leaving the structure), or on the shore of the body otherwise.
     */
    private BlockPos findFishingSpot(ServerLevel level, XoonglinEntity entity, FisherJob job,
                                     List<BlockPos> body) {
        Structure structure = findAssignedStructure(level, entity, job);
        if (structure != null) {
            BlockPos best = null;
            int bestDist = Integer.MAX_VALUE;
            for (List<BlockPos> blocks : structure.getBlockPositions().values()) {
                for (BlockPos pos : blocks) {
                    if (!isStandable(level, pos)) continue;
                    int dist = distanceToBody(pos, body);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos.above();
                    }
                }
            }
            return best;
        }

        // No structure: stand on the shore, on a solid block adjacent to the body
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        BlockPos entityPos = entity.blockPosition();
        for (BlockPos bodyPos : body) {
            for (BlockPos neighbor : List.of(bodyPos.north(), bodyPos.south(),
                    bodyPos.east(), bodyPos.west())) {
                if (body.contains(neighbor)) continue;
                if (!isStandable(level, neighbor)) continue;
                int dist = neighbor.distManhattan(entityPos);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = neighbor.above();
                }
            }
        }
        return best;
    }

    private boolean isStandable(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.above(2)).isAir();
    }

    private int distanceToBody(BlockPos pos, List<BlockPos> body) {
        int best = Integer.MAX_VALUE;
        for (BlockPos bodyPos : body) {
            best = Math.min(best, bodyPos.distManhattan(pos));
        }
        return best;
    }

    private BlockPos nearestBodyBlock(BlockPos pos) {
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        for (BlockPos bodyPos : bodyBlocks) {
            int dist = bodyPos.distManhattan(pos);
            if (dist < bestDist) {
                bestDist = dist;
                best = bodyPos;
            }
        }
        return best;
    }

    private Structure findAssignedStructure(ServerLevel level, XoonglinEntity entity, FisherJob job) {
        if (job.getRequiredStructureType() == null) return null;
        BlockPos assigned = entity.getAssignedStructurePos(job.getRequiredStructureType());
        if (assigned == null) return null;
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        return data.getStructures().stream()
                .filter(s -> s.getKeyBlockPos().equals(assigned))
                .filter(s -> s.getStructureTypeId().equals(job.getRequiredStructureType()))
                .findFirst().orElse(null);
    }

    private List<Container> findStructureContainers(ServerLevel level, XoonglinEntity entity, FisherJob job) {
        Structure structure = findAssignedStructure(level, entity, job);
        if (structure == null) return List.of();
        return ContainerUtil.findContainers(level, structure);
    }

    private void dropCatches(XoonglinEntity entity, FisherJob job) {
        SimpleContainer inventory = entity.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!job.isCatchItem(stack)) continue;
            net.minecraft.world.Containers.dropItemStack(entity.level(),
                    entity.getX(), entity.getY(), entity.getZ(), stack);
            inventory.setItem(i, ItemStack.EMPTY);
        }
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private FisherJob getFisherJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof FisherJob f ? f : null;
    }
}
