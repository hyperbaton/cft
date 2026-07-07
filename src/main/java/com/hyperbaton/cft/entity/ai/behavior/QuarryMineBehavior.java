package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.QuarryMinerJob;
import com.hyperbaton.cft.structure.OpenAirPlatformBlockGroup;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Digs a quarry (an open-air-platform structure) layer by layer, top-down. The miner
 * keeps a ladder column at one edge of the footprint (behind the key block) so it can
 * climb in and out. Movement in the shaft is driven directly (mob pathfinding does not
 * reliably climb ladders): the miner first walks on solid ground to the shaft, then a
 * controlled traversal tracks its own depth and keeps a ladder on every cell it passes.
 */
public class QuarryMineBehavior extends Behavior<XoonglinEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    private static final double MINE_REACH = 2.0;
    private static final double LADDER_XZ_REACH = 1.4;
    private static final int MAX_NAV_FAILURES = 6;
    private static final int FLOOD_RECHECK = 200;
    private static final int LADDER_RECHECK = 200;
    private static final int LADDERS_PER_FETCH = 32;
    // Blocks moved per tick along the ladder (4 ticks per block)
    private static final double CLIMB_SPEED = 0.25;

    private enum State { DESCENDING, MINING, ASCENDING, DEPOSITING, WAITING }
    private enum AfterAscend { DEPOSIT, WAIT_FLOODED, DONE }

    private State state;
    private AfterAscend afterAscend;
    private BlockPos structureKey;
    private List<BlockPos> footprint = List.of();
    private int surfaceY;
    private int minY;
    private BlockPos ladderColumn;
    private Direction ladderFacing;   // direction the ladder faces (away from the wall)
    private int workingY;             // the layer currently being mined
    private boolean inShaft;          // true once traversal (setPos) has control
    private int shaftFeetY;           // the miner's tracked feet level while in the shaft
    private boolean quarryDepleted;   // bottom reached: retire the quarry after depositing
    private int repathTimer;
    private int waitTicks;
    private int blockCooldown;
    private int navFailures;

    public QuarryMineBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        QuarryMinerJob job = getJob(entity);
        return job != null && entity.getAssignedStructurePos(job.getRequiredStructureType()) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        QuarryMinerJob job = getJob(entity);
        Structure structure = findStructure(level, entity, job);
        if (structure == null) return;

        structureKey = structure.getKeyBlockPos();
        footprint = new ArrayList<>(structure.getBlockPositions()
                .getOrDefault(OpenAirPlatformBlockGroup.SURFACE.getKey(), List.of()));
        if (footprint.isEmpty()) return;

        surfaceY = footprint.get(0).getY();
        minY = surfaceY - job.getMaxDepth() + 1;
        pickLadderColumn(level);

        workingY = findCurrentLayer(level);
        quarryDepleted = false;
        beginDescending();
        blockCooldown = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_MINE.get()).isPresent()
                && !footprint.isEmpty() && ladderColumn != null;
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        QuarryMinerJob job = getJob(entity);
        if (job == null || footprint.isEmpty() || ladderColumn == null) return;

        if (blockCooldown > 0) blockCooldown--;

        switch (state) {
            case DESCENDING -> tickDescending(level, entity, job);
            case MINING -> tickMining(level, entity, job);
            case ASCENDING -> tickAscending(level, entity, job);
            case DEPOSITING -> tickDepositing(level, entity, job);
            case WAITING -> tickWaiting(level, entity);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
    }

    private void tickDescending(ServerLevel level, XoonglinEntity entity, QuarryMinerJob job) {
        workingY = findCurrentLayer(level);
        if (workingY < minY) {
            // Everything diggable is gone: deposit the last load, then retire the quarry
            quarryDepleted = true;
            leaveVia(entity, AfterAscend.DEPOSIT);
            return;
        }
        if (isFlooded(level)) {
            flag(entity, CftMemoryModuleType.QUARRY_FLOODED.get());
            leaveVia(entity, AfterAscend.WAIT_FLOODED);
            return;
        }

        // Never enter the shaft without ladders: go to the chest and restock first.
        // From within the shaft we must climb out; on the surface we can walk there.
        if (job.countLadders(entity) <= 0) {
            if (inShaft) {
                leaveVia(entity, AfterAscend.DEPOSIT);
            } else {
                state = State.DEPOSITING;
            }
            return;
        }

        // Phase 1: reach the shaft on solid ground, then hand control to the traversal
        if (!inShaft) {
            int feetY = Mth.floor(entity.getY());
            if (feetY <= surfaceY) {
                // Inside the pit on a cleared layer — walk to the ladder column.
                // Only take over once actually standing on it (or as close as pathfinding
                // could get), so it walks the whole way instead of being snapped.
                boolean onColumn = horizNear(entity, ladderColumn, 0.5);
                boolean walkedAsClose = entity.getNavigation().isDone()
                        && horizNear(entity, ladderColumn, 1.3);
                if (onColumn || walkedAsClose) {
                    inShaft = true;
                    shaftFeetY = feetY;
                } else {
                    BlockPos colCell = new BlockPos(ladderColumn.getX(), feetY, ladderColumn.getZ());
                    if (navFail(entity, colCell)) briefWait();
                }
            } else {
                // On the surface/rim — walk to the rim, then drop onto the shaft top
                if (horizNear(entity, ladderColumn, LADDER_XZ_REACH + 1.0)) {
                    inShaft = true;
                    shaftFeetY = surfaceY + 1;
                    hold(entity, shaftFeetY);
                } else {
                    if (navFail(entity, rimStandPos(level))) briefWait();
                }
            }
            return;
        }

        // Phase 2: controlled descent, one level at a time
        if (!hold(entity, shaftFeetY)) return;   // still gliding to this level
        if (shaftFeetY <= workingY) {
            state = State.MINING;
            navFailures = 0;
            return;
        }
        BlockPos below = new BlockPos(ladderColumn.getX(), shaftFeetY - 1, ladderColumn.getZ());
        if (isMinable(level, below)) {
            lookAt(entity, below);
            if (blockCooldown <= 0) {
                mineBlock(level, entity, below);
                blockCooldown = job.getMineSpeed();
            }
            return;
        }
        if (!(level.getBlockState(below).getBlock() instanceof LadderBlock)) {
            if (job.countLadders(entity) <= 0) {
                leaveVia(entity, AfterAscend.DEPOSIT);
                return;
            }
            placeLadder(level, entity, below);
            return;
        }
        shaftFeetY--;
    }

    private void tickMining(ServerLevel level, XoonglinEntity entity, QuarryMinerJob job) {
        if (isFlooded(level)) {
            flag(entity, CftMemoryModuleType.QUARRY_FLOODED.get());
            leaveVia(entity, AfterAscend.WAIT_FLOODED);
            return;
        }
        if (!job.canWork(entity) || job.isQuotaDone(entity.getJobState())) {
            leaveVia(entity, AfterAscend.DONE);
            return;
        }

        BlockPos target = nextMinable(level, workingY);
        if (target == null) {
            beginDescending();     // layer done — go one deeper
            return;
        }

        lookAt(entity, target);
        if (entity.position().distanceTo(Vec3.atCenterOf(target)) > MINE_REACH) {
            if (navFail(entity, target)) navFailures = 0; // skip this block for now
            return;
        }
        navFailures = 0;
        if (blockCooldown > 0) return;
        entity.getNavigation().stop();
        mineBlock(level, entity, target);
        blockCooldown = job.getMineSpeed();
        if (isInventoryFull(entity)) {
            leaveVia(entity, AfterAscend.DEPOSIT);
        }
    }

    private void tickAscending(ServerLevel level, XoonglinEntity entity, QuarryMinerJob job) {
        if (!hold(entity, shaftFeetY)) return;   // still gliding to this level

        // Replenish a missing ladder on the cell we are passing
        if (shaftFeetY <= surfaceY) {
            BlockPos here = new BlockPos(ladderColumn.getX(), shaftFeetY, ladderColumn.getZ());
            if (!(level.getBlockState(here).getBlock() instanceof LadderBlock)
                    && !isMinable(level, here) && job.countLadders(entity) > 0) {
                placeLadder(level, entity, here);
            }
        }

        if (shaftFeetY >= surfaceY + 1) {
            // Out of the shaft: step onto the rim so we do not drop back in
            BlockPos rim = rimStandPos(level);
            entity.setPos(rim.getX() + 0.5, rim.getY(), rim.getZ() + 0.5);
            entity.setDeltaMovement(Vec3.ZERO);
            inShaft = false;
            switch (afterAscend) {
                case DEPOSIT -> state = State.DEPOSITING;
                case WAIT_FLOODED -> { state = State.WAITING; waitTicks = 40; }
                case DONE -> {
                    if (hasMinedItems(entity)) state = State.DEPOSITING;
                    else entity.getBrain().eraseMemory(CftMemoryModuleType.MUST_MINE.get());
                }
            }
            return;
        }
        shaftFeetY++;
    }

    private void tickDepositing(ServerLevel level, XoonglinEntity entity, QuarryMinerJob job) {
        List<Container> containers = containers(level, entity, job);
        BlockPos containerPos = containerPos(level, entity, job);
        if (containers.isEmpty() || containerPos == null) {
            entity.getBrain().eraseMemory(CftMemoryModuleType.MUST_MINE.get());
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(containerPos))
                > CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            if (entity.getNavigation().isDone() || ++repathTimer >= REPATH_INTERVAL) {
                repathTimer = 0;
                approach(entity, containerPos);
            }
            return;
        }

        entity.getNavigation().stop();
        depositLoad(entity, containers);

        if (quarryDepleted) {
            retireQuarry(level, entity, job);
            entity.getBrain().eraseMemory(CftMemoryModuleType.MUST_MINE.get());
            return;
        }

        takeLadders(entity, job, containers);

        boolean moreWork = job.canWork(entity) && !job.isQuotaDone(entity.getJobState())
                && findCurrentLayer(level) >= minY;
        if (!moreWork) {
            entity.getBrain().eraseMemory(CftMemoryModuleType.MUST_MINE.get());
        } else if (job.countLadders(entity) <= 0) {
            flag(entity, CftMemoryModuleType.QUARRY_NEEDS_LADDERS.get());
            LOGGER.warn("[Quarry] {} is out of ladders at {}, waiting",
                    entity.getName().getString(), structureKey);
            state = State.WAITING;
            waitTicks = LADDER_RECHECK;
        } else {
            beginDescending();
        }
    }

    private void tickWaiting(ServerLevel level, XoonglinEntity entity) {
        if (--waitTicks > 0) return;

        if (entity.getBrain().hasMemoryValue(CftMemoryModuleType.QUARRY_FLOODED.get())) {
            workingY = findCurrentLayer(level);
            if (isFlooded(level)) {
                waitTicks = FLOOD_RECHECK;
            } else {
                entity.getBrain().eraseMemory(CftMemoryModuleType.QUARRY_FLOODED.get());
                beginDescending();
            }
            return;
        }
        if (entity.getBrain().hasMemoryValue(CftMemoryModuleType.QUARRY_NEEDS_LADDERS.get())) {
            if (ladderInContainer(level, entity)) {
                entity.getBrain().eraseMemory(CftMemoryModuleType.QUARRY_NEEDS_LADDERS.get());
                state = State.DEPOSITING;
            } else {
                waitTicks = LADDER_RECHECK;
            }
            return;
        }
        beginDescending();
    }

    private void beginDescending() {
        state = State.DESCENDING;
        inShaft = false;
        repathTimer = 0;
        navFailures = 0;
    }

    private void leaveVia(XoonglinEntity entity, AfterAscend intent) {
        this.afterAscend = intent;
        this.state = State.ASCENDING;
        this.inShaft = true;
        this.shaftFeetY = Mth.floor(entity.getY());
        this.repathTimer = 0;
    }

    private void briefWait() {
        state = State.WAITING;
        waitTicks = 40;
        navFailures = 0;
    }

    private void flag(XoonglinEntity entity, MemoryModuleType<Boolean> memory) {
        entity.getBrain().setMemory(memory, Boolean.TRUE);
    }

    /**
     * Pins the miner to the ladder column and glides it toward the given feet level at
     * the climb speed (no physics). Any horizontal offset is glided out first (so there
     * is no snap), then it climbs vertically. Returns true once at that level.
     */
    private boolean hold(XoonglinEntity entity, int feetY) {
        entity.getNavigation().stop();
        entity.fallDistance = 0;
        entity.setDeltaMovement(Vec3.ZERO);
        double cx = ladderColumn.getX() + 0.5;
        double cz = ladderColumn.getZ() + 0.5;
        double x = entity.getX();
        double z = entity.getZ();
        double y = entity.getY();

        if (Math.abs(cx - x) > CLIMB_SPEED || Math.abs(cz - z) > CLIMB_SPEED) {
            // Slide horizontally onto the column first, keeping the same height
            entity.setPos(moveToward(x, cx), y, moveToward(z, cz));
            return false;
        }
        double dy = feetY - y;
        if (Math.abs(dy) <= CLIMB_SPEED) {
            entity.setPos(cx, feetY, cz);
            return true;
        }
        entity.setPos(cx, y + Math.signum(dy) * CLIMB_SPEED, cz);
        return false;
    }

    private double moveToward(double cur, double target) {
        double d = target - cur;
        return Math.abs(d) <= CLIMB_SPEED ? target : cur + Math.signum(d) * CLIMB_SPEED;
    }

    private boolean overLadderColumn(XoonglinEntity entity) {
        return horizNear(entity, ladderColumn, LADDER_XZ_REACH);
    }

    private boolean horizNear(XoonglinEntity entity, BlockPos pos, double dist) {
        double dx = entity.getX() - (pos.getX() + 0.5);
        double dz = entity.getZ() - (pos.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz) <= dist;
    }

    private void lookAt(XoonglinEntity entity, BlockPos pos) {
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(Vec3.atCenterOf(pos)));
    }

    private boolean navFail(XoonglinEntity entity, BlockPos pos) {
        if (entity.getNavigation().isDone()) {
            if (++navFailures >= MAX_NAV_FAILURES) {
                return true;
            }
            repathTimer = 0;
            approach(entity, pos);
        } else if (++repathTimer >= REPATH_INTERVAL) {
            repathTimer = 0;
            approach(entity, pos);
        }
        return false;
    }

    private void mineBlock(ServerLevel level, XoonglinEntity entity, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(st, level, pos, level.getBlockEntity(pos));
        level.destroyBlock(pos, false);
        SimpleContainer inv = entity.getInventory();
        for (ItemStack drop : drops) {
            ItemStack leftover = inv.addItem(drop);
            if (!leftover.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level,
                        entity.getX(), entity.getY(), entity.getZ(), leftover);
            }
        }
        entity.swing(InteractionHand.MAIN_HAND);
    }

    private void placeLadder(ServerLevel level, XoonglinEntity entity, BlockPos pos) {
        level.setBlock(pos, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladderFacing), 3);
        SimpleContainer inv = entity.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == Items.LADDER) {
                stack.shrink(1);
                break;
            }
        }
        entity.swing(InteractionHand.MAIN_HAND);
    }

    private int findCurrentLayer(ServerLevel level) {
        for (int y = surfaceY; y >= minY; y--) {
            for (BlockPos col : footprint) {
                if (isLadderColumn(col)) continue;
                if (isMinable(level, new BlockPos(col.getX(), y, col.getZ()))) {
                    return y;
                }
            }
        }
        return minY - 1;
    }

    private boolean isFlooded(ServerLevel level) {
        int from = Math.max(workingY, minY);
        for (int y = surfaceY; y >= from; y--) {
            for (BlockPos col : footprint) {
                if (!level.getBlockState(new BlockPos(col.getX(), y, col.getZ()))
                        .getFluidState().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private BlockPos nextMinable(ServerLevel level, int y) {
        for (BlockPos col : footprint) {
            if (isLadderColumn(col)) continue;
            BlockPos cell = new BlockPos(col.getX(), y, col.getZ());
            if (isMinable(level, cell)) return cell;
        }
        return null;
    }

    private boolean isLadderColumn(BlockPos col) {
        return col.getX() == ladderColumn.getX() && col.getZ() == ladderColumn.getZ();
    }

    private boolean isMinable(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir()) return false;
        if (!st.getFluidState().isEmpty()) return false;
        if (st.getBlock() instanceof LadderBlock) return false;
        return st.getDestroySpeed(level, pos) >= 0;
    }

    private void pickLadderColumn(ServerLevel level) {
        Set<Long> columns = new HashSet<>();
        for (BlockPos p : footprint) {
            columns.add(columnKey(p.getX(), p.getZ()));
        }
        BlockPos best = null;
        Direction bestFacing = null;
        int bestScore = Integer.MAX_VALUE;
        for (BlockPos col : footprint) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = col.relative(dir);
                if (columns.contains(columnKey(neighbor.getX(), neighbor.getZ()))) continue;
                BlockPos wall = new BlockPos(neighbor.getX(), surfaceY, neighbor.getZ());
                if (level.getBlockState(wall).isAir()) continue;
                int score = neighbor.distManhattan(new BlockPos(structureKey.getX(), surfaceY, structureKey.getZ()));
                if (score < bestScore) {
                    bestScore = score;
                    best = new BlockPos(col.getX(), surfaceY, col.getZ());
                    bestFacing = dir.getOpposite();
                }
            }
        }
        if (best == null && !footprint.isEmpty()) {
            BlockPos p = footprint.get(0);
            best = new BlockPos(p.getX(), surfaceY, p.getZ());
            bestFacing = Direction.NORTH;
        }
        ladderColumn = best;
        ladderFacing = bestFacing;
    }

    private BlockPos rimStandPos(ServerLevel level) {
        BlockPos wallCol = ladderColumn.relative(ladderFacing.getOpposite());
        for (int y = surfaceY + 3; y >= surfaceY; y--) {
            BlockPos below = new BlockPos(wallCol.getX(), y - 1, wallCol.getZ());
            BlockPos at = new BlockPos(wallCol.getX(), y, wallCol.getZ());
            if (!level.getBlockState(below).isAir() && level.getBlockState(at).isAir()) {
                return at;
            }
        }
        return new BlockPos(wallCol.getX(), surfaceY + 2, wallCol.getZ());
    }

    private boolean isInventoryFull(XoonglinEntity entity) {
        SimpleContainer inv = entity.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    private boolean hasMinedItems(XoonglinEntity entity) {
        SimpleContainer inv = entity.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() != Items.LADDER) return true;
        }
        return false;
    }

    private void depositLoad(XoonglinEntity entity, List<Container> containers) {
        SimpleContainer inv = entity.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || stack.getItem() == Items.LADDER) continue;
            inv.setItem(i, ContainerUtil.insertIntoContainers(containers, stack));
        }
    }

    private void takeLadders(XoonglinEntity entity, QuarryMinerJob job, List<Container> containers) {
        int wanted = LADDERS_PER_FETCH - job.countLadders(entity);
        if (wanted <= 0) return;
        SimpleContainer inv = entity.getInventory();
        for (Container container : containers) {
            for (int i = 0; i < container.getContainerSize() && wanted > 0; i++) {
                ItemStack stack = container.getItem(i);
                if (stack.getItem() != Items.LADDER) continue;
                int take = Math.min(wanted, stack.getCount());
                ItemStack leftover = inv.addItem(stack.copyWithCount(take));
                int moved = take - leftover.getCount();
                stack.shrink(moved);
                wanted -= moved;
            }
            container.setChanged();
        }
    }

    /**
     * Retires a depleted quarry: removes it from StructuresData so no miner claims it
     * again, and unassigns it from this miner so the job loop looks for another one.
     */
    private void retireQuarry(ServerLevel level, XoonglinEntity entity, QuarryMinerJob job) {
        Structure structure = findStructure(level, entity, job);
        if (structure != null) {
            StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
            data.removeStructure(structure);
        }
        entity.unassignStructure(job.getRequiredStructureType());
        LOGGER.info("[Quarry] {} depleted the quarry at {}; retiring it",
                entity.getName().getString(), structureKey);
    }

    private boolean ladderInContainer(ServerLevel level, XoonglinEntity entity) {
        for (Container container : containers(level, entity, getJob(entity))) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).getItem() == Items.LADDER) return true;
            }
        }
        return false;
    }

    private Structure findStructure(ServerLevel level, XoonglinEntity entity, QuarryMinerJob job) {
        BlockPos assigned = entity.getAssignedStructurePos(job.getRequiredStructureType());
        if (assigned == null) return null;
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        return data.getStructures().stream()
                .filter(s -> s.getKeyBlockPos().equals(assigned))
                .filter(s -> s.getStructureTypeId().equals(job.getRequiredStructureType()))
                .findFirst().orElse(null);
    }

    private List<Container> containers(ServerLevel level, XoonglinEntity entity, QuarryMinerJob job) {
        Structure structure = findStructure(level, entity, job);
        return structure == null ? List.of() : ContainerUtil.findContainers(level, structure);
    }

    private BlockPos containerPos(ServerLevel level, XoonglinEntity entity, QuarryMinerJob job) {
        Structure structure = findStructure(level, entity, job);
        return structure == null ? null
                : ContainerUtil.findContainerPositions(level, structure).stream().findFirst().orElse(null);
    }

    private long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private void approach(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private QuarryMinerJob getJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof QuarryMinerJob q ? q : null;
    }
}
