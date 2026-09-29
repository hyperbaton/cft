package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.LumberjackJob;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.util.TreeUtil;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;

/**
 * Makes a lumberjack work: it finds the nearest natural tree around its base, walks next to it and
 * chops the base of its trunk until the whole tree falls. Chopping takes as long as breaking each
 * log and leaf one by one would, and each log wears the axe down; then all of them break at once and
 * their drops go to its inventory. It replants the tree's spot with saplings and takes the wood back
 * to its base, keeping a few saplings for replanting. Without an axe, it goes to its base to get one.
 */
public class ChopTreesBehavior extends Behavior<XoonglinEntity> {

    private static final int MAX_DURATION = 6000;
    private static final int REPATH_INTERVAL = 40;
    /** Chopping time each log and leaf of the tree adds. */
    private static final int LOG_TICKS = 20;
    private static final int LEAF_TICKS = 3;
    private static final int SWING_INTERVAL = 8;
    /** How close to the tree's base it must be to chop it. */
    private static final double TREE_REACH = 3.0;
    private static final double BASE_REACH = 3.0;
    /** Trees it stops getting closer to for this long are skipped: it can't get to them. */
    private static final int MAX_TICKS_WITHOUT_PROGRESS = 200;
    private static final int WAIT_TICKS = 600;
    private static final int SAPLINGS_TO_KEEP = 4;

    private enum State {
        SEARCHING(WorkStep.of("looking_for_trees")),
        MOVING_TO_TREE(WorkStep.of("walking_to_tree")),
        CHOPPING(WorkStep.of("chopping")),
        RETURNING(WorkStep.HEADING_BACK),
        DEPOSITING(WorkStep.STORING_ITEMS),
        WAITING(WorkStep.WAITING);

        /** Shown in the job tab while the behavior is in this state. */
        private final WorkStep step;

        State(WorkStep step) {
            this.step = step;
        }
    }

    private State state;
    private TreeUtil.Tree tree;
    /** Where it stands to chop the tree. */
    private BlockPos choppingSpot;
    /**
     * The tree's blocks whose chopping time and axe wear are still due. Kept while it fetches a new
     * axe, so it resumes the same tree instead of starting over.
     */
    private Deque<BlockPos> pendingBlocks = new ArrayDeque<>();
    private int choppingTicks;
    private int totalChoppingTicks;
    private int repathTimer;
    private int actionTimer;
    private double closestApproach;
    private int ticksWithoutProgress;
    private boolean noTreesFound;
    /** Logs of trees it couldn't get next to, left alone until it starts working again. */
    private Set<BlockPos> unreachableLogs = new HashSet<>();

    public ChopTreesBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, MAX_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        LumberjackJob job = getLumberjackJob(entity);
        return job != null && job.getBasePos(entity) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        state = JobUtil.findTool(entity, ItemTags.AXES).isEmpty() ? State.RETURNING : State.SEARCHING;
        tree = null;
        choppingSpot = null;
        pendingBlocks = new ArrayDeque<>();
        repathTimer = 0;
        actionTimer = 0;
        noTreesFound = false;
        unreachableLogs = new HashSet<>();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_CHOP.get());
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        if (state != null) {
            BehaviorUtils.showWorkStep(entity, state.step);
        }
        LumberjackJob job = getLumberjackJob(entity);
        BlockPos basePos = job != null ? job.getBasePos(entity) : null;
        if (basePos == null) return;

        switch (state) {
            case SEARCHING -> tickSearching(level, entity, job, basePos);
            case MOVING_TO_TREE -> tickMovingToTree(level, entity);
            case CHOPPING -> tickChopping(level, entity);
            case RETURNING -> tickReturning(entity, basePos);
            case DEPOSITING -> tickDepositing(level, entity, job, basePos);
            case WAITING -> {
                if (--actionTimer <= 0) state = State.DEPOSITING;
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        BehaviorUtils.clearWorkStep(entity);
        entity.getNavigation().stop();
        if (tree != null) {
            level.destroyBlockProgress(entity.getId(), tree.base(), -1);
        }
        tree = null;
        pendingBlocks.clear();
    }

    private void tickSearching(ServerLevel level, XoonglinEntity entity, LumberjackJob job, BlockPos basePos) {
        if (JobUtil.findTool(entity, ItemTags.AXES).isEmpty()) {
            state = State.RETURNING;
            return;
        }
        // Back with a new axe to finish the tree it was chopping
        if (tree != null && level.getBlockState(tree.base()).is(BlockTags.LOGS)) {
            startApproach(level, entity);
            return;
        }
        Set<BlockPos> excluded = structureBlocks(level);
        excluded.addAll(unreachableLogs);
        Optional<TreeUtil.Tree> found = TreeUtil.findNearestTree(level, basePos, job.getChopRadius(),
                entity.blockPosition(), excluded, job.getMaxTreeSize());
        if (found.isPresent()) {
            tree = found.get();
            pendingBlocks = new ArrayDeque<>(tree.logs());
            pendingBlocks.addAll(tree.leaves());
            totalChoppingTicks = tree.logs().size() * LOG_TICKS + tree.leaves().size() * LEAF_TICKS;
            choppingTicks = 0;
            startApproach(level, entity);
        } else {
            // No trees for now: store what it carries and wait a while before searching again
            tree = null;
            noTreesFound = true;
            state = State.RETURNING;
        }
    }

    private void startApproach(ServerLevel level, XoonglinEntity entity) {
        Optional<BlockPos> spot = findChoppingSpot(level, entity, tree);
        if (spot.isEmpty()) {
            skipTree(level, entity);
            return;
        }
        choppingSpot = spot.get();
        state = State.MOVING_TO_TREE;
        repathTimer = 0;
        closestApproach = Double.MAX_VALUE;
        ticksWithoutProgress = 0;
        navigateTo(entity, choppingSpot);
    }

    /**
     * A spot next to the tree's base where it can stand to chop it, the closest to the Xoonglin.
     * It must walk to a spot like this rather than to the log itself: navigation aimed at a solid
     * block goes to the first free block above it, which for a tree is over its canopy.
     */
    private static Optional<BlockPos> findChoppingSpot(ServerLevel level, XoonglinEntity entity, TreeUtil.Tree tree) {
        Vec3 base = tree.base().getBottomCenter();
        List<BlockPos> spots = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(tree.base().offset(-2, -1, -2), tree.base().offset(2, 1, 2))) {
            if (pos.getBottomCenter().distanceTo(base) <= TREE_REACH - 0.5 && BehaviorUtils.canStandAt(level, pos)) {
                spots.add(pos.immutable());
            }
        }
        return spots.stream().min(Comparator.comparingDouble(pos -> pos.distSqr(entity.blockPosition())));
    }

    private void tickMovingToTree(ServerLevel level, XoonglinEntity entity) {
        BlockPos base = tree.base();
        if (!level.getBlockState(base).is(BlockTags.LOGS)) {
            // Felled by someone else meanwhile
            forgetTree(level, entity);
            return;
        }
        // Navigation stops short of the spot's center by up to half a block, a bit more for small
        // Xoonglins, so being at the spot counts too even if that leaves the base slightly out of reach
        double distanceToSpot = entity.position().distanceTo(choppingSpot.getBottomCenter());
        if (entity.position().distanceTo(base.getBottomCenter()) < TREE_REACH || distanceToSpot < 1.0) {
            entity.getNavigation().stop();
            actionTimer = 0;
            state = State.CHOPPING;
            return;
        }
        if (distanceToSpot < closestApproach - 0.5) {
            closestApproach = distanceToSpot;
            ticksWithoutProgress = 0;
        } else if (++ticksWithoutProgress >= MAX_TICKS_WITHOUT_PROGRESS) {
            skipTree(level, entity);
            return;
        }
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, choppingSpot);
        }
    }

    /**
     * Chops the base of the trunk. Each of the tree's blocks adds its breaking time, and each log
     * wears the axe by one (leaves don't, it would be too punishing); once they're all done, the
     * tree falls.
     */
    private void tickChopping(ServerLevel level, XoonglinEntity entity) {
        BlockPos base = tree.base();
        BlockState baseState = level.getBlockState(base);
        if (!baseState.is(BlockTags.LOGS)) {
            // Felled by someone else meanwhile
            forgetTree(level, entity);
            return;
        }
        ItemStack axe = JobUtil.findTool(entity, ItemTags.AXES);
        if (axe.isEmpty()) {
            // Keeps the tree and what's left to chop, to resume it with a new axe
            level.destroyBlockProgress(entity.getId(), base, -1);
            state = State.RETURNING;
            return;
        }

        if (actionTimer <= 0) {
            BlockPos next = pollPendingBlock(level);
            if (next == null) {
                fellTree(level, entity, axe);
                replant(level, entity);
                state = State.RETURNING;
                return;
            }
            boolean isLog = level.getBlockState(next).is(BlockTags.LOGS);
            if (isLog) {
                if (axe == entity.getMainHandItem()) {
                    // Breaks in its hand with the vanilla sound and particles
                    axe.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
                } else {
                    axe.hurtAndBreak(1, level, entity, item -> {});
                    entity.getInventory().setChanged();
                }
            }
            actionTimer = isLog ? LOG_TICKS : LEAF_TICKS;
        }
        actionTimer--;
        choppingTicks++;

        entity.getLookControl().setLookAt(base.getCenter());
        if (choppingTicks % SWING_INTERVAL == 0) {
            entity.swing(InteractionHand.MAIN_HAND);
            SoundType sound = baseState.getSoundType(level, base, entity);
            level.playSound(null, base, sound.getHitSound(), SoundSource.NEUTRAL,
                    (sound.getVolume() + 1.0F) / 8.0F, sound.getPitch() * 0.5F);
        }
        level.destroyBlockProgress(entity.getId(), base,
                Math.min(9, choppingTicks * 10 / Math.max(1, totalChoppingTicks)));
    }

    /** The next of the tree's blocks still standing, or null if none is left. */
    private BlockPos pollPendingBlock(ServerLevel level) {
        BlockPos pos;
        while ((pos = pendingBlocks.poll()) != null) {
            BlockState blockState = level.getBlockState(pos);
            if (blockState.is(BlockTags.LOGS) || blockState.is(BlockTags.LEAVES)) return pos;
        }
        return null;
    }

    /** The tree falls: all its logs and leaves break at once, and their drops go to its inventory. */
    private void fellTree(ServerLevel level, XoonglinEntity entity, ItemStack axe) {
        level.destroyBlockProgress(entity.getId(), tree.base(), -1);
        List<BlockPos> blocks = new ArrayList<>(tree.logs());
        blocks.addAll(tree.leaves());
        for (BlockPos pos : blocks) {
            BlockState blockState = level.getBlockState(pos);
            if (!blockState.is(BlockTags.LOGS) && !blockState.is(BlockTags.LEAVES)) continue;
            List<ItemStack> drops = Block.getDrops(blockState, level, pos, level.getBlockEntity(pos), entity, axe);
            level.destroyBlock(pos, false, entity);
            for (ItemStack drop : drops) {
                ItemStack remainder = entity.getInventory().addItem(drop);
                if (!remainder.isEmpty()) {
                    Containers.dropItemStack(level, entity.getX(), entity.getY(), entity.getZ(), remainder);
                }
            }
        }
        entity.getInventory().setChanged();
    }

    /** Leaves the tree alone until it starts working again, and looks for another one. */
    private void skipTree(ServerLevel level, XoonglinEntity entity) {
        unreachableLogs.addAll(tree.logs());
        forgetTree(level, entity);
    }

    private void forgetTree(ServerLevel level, XoonglinEntity entity) {
        level.destroyBlockProgress(entity.getId(), tree.base(), -1);
        tree = null;
        pendingBlocks.clear();
        state = State.SEARCHING;
    }

    /** Plants a sapling in each spot the tree grew from, preferring the tree's own kind. */
    private void replant(ServerLevel level, XoonglinEntity entity) {
        Optional<Item> matching = TreeUtil.matchingSapling(tree.logId(level));
        for (BlockPos spot : tree.plantingSpots()) {
            ItemStack sapling = matching.map(item -> findInInventory(entity, stack -> stack.is(item)))
                    .filter(stack -> !stack.isEmpty() && TreeUtil.canPlant(level, stack, spot))
                    .orElseGet(() -> findInInventory(entity, stack -> TreeUtil.canPlant(level, stack, spot)));
            if (!sapling.isEmpty()) {
                TreeUtil.plant(level, sapling, spot);
            }
        }
        entity.getInventory().setChanged();
        tree = null;
    }

    private void tickReturning(XoonglinEntity entity, BlockPos basePos) {
        if (entity.position().distanceTo(basePos.getCenter()) < BASE_REACH) {
            entity.getNavigation().stop();
            state = State.DEPOSITING;
            return;
        }
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, basePos);
        }
    }

    /**
     * Stores everything but the axe and a few saplings, then restocks an axe and saplings from the
     * base if it's missing them. Without an axe to use, it waits at the base and checks again later.
     */
    private void tickDepositing(ServerLevel level, XoonglinEntity entity, LumberjackJob job, BlockPos basePos) {
        List<Container> containers = baseContainers(level, entity, job, basePos);
        int saplingsKept = 0;
        boolean axeKept = entity.getMainHandItem().is(ItemTags.AXES);
        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            ItemStack stack = entity.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!axeKept && stack.is(ItemTags.AXES)) {
                axeKept = true;
                continue;
            }
            ItemStack toStore = stack.copy();
            if (stack.is(ItemTags.SAPLINGS) && saplingsKept < SAPLINGS_TO_KEEP) {
                int keep = Math.min(stack.getCount(), SAPLINGS_TO_KEEP - saplingsKept);
                saplingsKept += keep;
                toStore.shrink(keep);
                stack.setCount(keep);
            } else {
                entity.getInventory().setItem(i, ItemStack.EMPTY);
            }
            if (toStore.isEmpty()) continue;
            ItemStack remainder = ContainerUtil.insertIntoContainers(containers, toStore);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(level, basePos.getX() + 0.5, basePos.getY() + 1.0, basePos.getZ() + 0.5, remainder);
            }
        }

        if (!axeKept) {
            takeFromContainers(entity, containers, stack -> stack.is(ItemTags.AXES), 1);
            JobUtil.equipTool(entity, ItemTags.AXES);
        }
        if (saplingsKept < SAPLINGS_TO_KEEP) {
            takeFromContainers(entity, containers, stack -> stack.is(ItemTags.SAPLINGS), SAPLINGS_TO_KEEP - saplingsKept);
        }
        entity.getInventory().setChanged();

        if (JobUtil.findTool(entity, ItemTags.AXES).isEmpty() || noTreesFound) {
            noTreesFound = false;
            actionTimer = WAIT_TICKS;
            state = State.WAITING;
        } else {
            state = State.SEARCHING;
        }
    }

    private static void takeFromContainers(XoonglinEntity entity, List<Container> containers,
                                           Predicate<ItemStack> matches, int maxCount) {
        int remaining = maxCount;
        for (Container container : containers) {
            for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty() || !matches.test(stack) || !entity.getInventory().canAddItem(stack)) continue;
                ItemStack taken = container.removeItem(i, Math.min(remaining, stack.getCount()));
                remaining -= taken.getCount();
                entity.getInventory().addItem(taken);
                container.setChanged();
            }
            if (remaining <= 0) return;
        }
    }

    /** The containers of its lumber camp, or of its home if it works without one. */
    private static List<Container> baseContainers(ServerLevel level, XoonglinEntity entity, LumberjackJob job,
                                                  BlockPos basePos) {
        if (job.getRequiredStructureType() != null) {
            return level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData")
                    .findByKeyBlock(basePos)
                    .map(camp -> ContainerUtil.findContainers(level, camp))
                    .orElse(List.of());
        }
        List<Container> containers = new ArrayList<>();
        if (entity.getHome() != null) {
            for (BlockPos pos : entity.getHome().getInteriorBlocks()) {
                if (level.getBlockEntity(pos) instanceof Container container) {
                    containers.add(container);
                }
            }
        }
        return containers;
    }

    /** Blocks of every registered structure, whose logs must never be taken for trees. */
    private static Set<BlockPos> structureBlocks(ServerLevel level) {
        Set<BlockPos> blocks = new HashSet<>();
        for (Structure structure : level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData")
                .getStructures()) {
            blocks.addAll(structure.getAllBlockPositions());
        }
        return blocks;
    }

    private static ItemStack findInInventory(XoonglinEntity entity, Predicate<ItemStack> matches) {
        for (int i = 0; i < entity.getInventory().getContainerSize(); i++) {
            ItemStack stack = entity.getInventory().getItem(i);
            if (!stack.isEmpty() && matches.test(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private static LumberjackJob getLumberjackJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof LumberjackJob lumberjackJob ? lumberjackJob : null;
    }
}
