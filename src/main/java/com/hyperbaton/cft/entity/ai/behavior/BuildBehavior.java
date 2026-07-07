package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.BuilderJob;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BuildBehavior extends Behavior<XoonglinEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    private static final double REACH = 3.0;
    private static final int BLOCKS_PER_SITE_SCAN = 4096;
    // Ticks to spend trying to step off a placement target before deferring it
    private static final int MAX_OCCUPY_TICKS = 60;
    // Ticks to wait before re-checking storage when it lacks needed materials
    private static final int FETCH_RETRY_COOLDOWN = 600;
    // Consecutive plan rebuilds without progress before abandoning the site
    private static final int MAX_STALLED_REBUILDS = 2;

    private enum State {
        FINDING_SITE, PLANNING, FETCHING_RESOURCES,
        TRAVELING_TO_SITE, PLACING_BLOCKS
    }

    private record BuildPlacement(BlockPos target, BlockState state) {}

    private static final int MAX_NAV_FAILURES = 3;

    private State state;
    private BlockPos buildSiteKeyBlock;
    private String buildStructureTypeId;
    private List<BuildPlacement> buildPlan;
    private int buildPlanIndex;
    private BlockPos storageContainerPos;
    private BlockPos storageStructureKeyPos;
    private int repathTimer;
    private int placeCooldown;
    private int navFailures;
    private int occupyTicks;
    private int fetchWait;
    private int lastRemainingCount;
    private int stalledRebuilds;
    private final Set<BlockPos> abandonedSites = new HashSet<>();

    public BuildBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getBuilderJob(entity) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        state = State.FINDING_SITE;
        buildSiteKeyBlock = null;
        buildStructureTypeId = null;
        buildPlan = null;
        buildPlanIndex = 0;
        storageContainerPos = null;
        storageStructureKeyPos = null;
        repathTimer = 0;
        placeCooldown = 0;
        navFailures = 0;
        occupyTicks = 0;
        fetchWait = 0;
        lastRemainingCount = Integer.MAX_VALUE;
        stalledRebuilds = 0;
        abandonedSites.clear();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_BUILD.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        BuilderJob job = getBuilderJob(entity);
        if (job == null) return;

        switch (state) {
            case FINDING_SITE -> tickFindingSite(level, entity, job);
            case PLANNING -> tickPlanning(level, entity, job);
            case FETCHING_RESOURCES -> tickFetchingResources(level, entity, job);
            case TRAVELING_TO_SITE -> tickTravelingToSite(entity);
            case PLACING_BLOCKS -> tickPlacingBlocks(level, entity);
        }
    }

    private void tickFindingSite(ServerLevel level, XoonglinEntity entity, BuilderJob job) {
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        Set<BlockPos> existingBlocks = new HashSet<>();
        for (Structure s : data.getStructures()) {
            existingBlocks.add(s.getKeyBlockPos());
            for (List<BlockPos> group : s.getBlockPositions().values()) {
                existingBlocks.addAll(group);
            }
        }

        BlockPos entityPos = entity.blockPosition();
        int radius = job.getBuildRadius();
        BlockPos bestPos = null;
        String bestTypeId = null;
        int bestDist = Integer.MAX_VALUE;

        int minY = entityPos.getY() - 10;
        int maxY = entityPos.getY() + 10;

        for (int x = entityPos.getX() - radius; x <= entityPos.getX() + radius; x++) {
            for (int z = entityPos.getZ() - radius; z <= entityPos.getZ() + radius; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    BlockState blockState = level.getBlockState(pos);
                    if (blockState.isAir()) continue;

                    BlockPos normalizedPos = pos;
                    if (blockState.getBlock() instanceof DoorBlock
                            && blockState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                        normalizedPos = pos.below();
                    }

                    if (existingBlocks.contains(normalizedPos)) continue;
                    if (!normalizedPos.equals(pos) && existingBlocks.contains(pos)) continue;
                    if (abandonedSites.contains(normalizedPos)) continue;

                    for (String typeId : job.getBuildableStructures()) {
                        StructureType structureType = findStructureType(typeId);
                        if (structureType == null) continue;
                        if (!structureType.matchesKeyBlock(blockState)) continue;

                        int dist = normalizedPos.distManhattan(entityPos);
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestPos = normalizedPos;
                            bestTypeId = typeId;
                        }
                        break;
                    }
                }
            }
        }

        if (bestPos != null) {
            buildSiteKeyBlock = bestPos;
            buildStructureTypeId = bestTypeId;
            state = State.PLANNING;
        }
    }

    private void tickPlanning(ServerLevel level, XoonglinEntity entity, BuilderJob job) {
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");

        Structure template = findClosestTemplate(data, entity, buildStructureTypeId);
        if (template == null) {
            abandonedSites.add(buildSiteKeyBlock);
            state = State.FINDING_SITE;
            buildSiteKeyBlock = null;
            return;
        }

        buildPlan = computeBuildPlan(level, template, buildSiteKeyBlock);
        buildPlanIndex = 0;

        if (buildPlan.isEmpty()) {
            // Structure is already fully built (even if the player hasn't detected it yet)
            abandonedSites.add(buildSiteKeyBlock);
            state = State.FINDING_SITE;
            buildSiteKeyBlock = null;
            buildPlan = null;
            return;
        }

        storageContainerPos = findStorageContainer(level, data, entity, job);
        if (storageContainerPos == null) {
            abandonedSites.add(buildSiteKeyBlock);
            state = State.FINDING_SITE;
            buildSiteKeyBlock = null;
            buildPlan = null;
            return;
        }

        lastRemainingCount = buildPlan.size();
        stalledRebuilds = 0;
        state = State.FETCHING_RESOURCES;
        repathTimer = 0;
        navigateTo(entity, storageContainerPos);
    }

    private void tickFetchingResources(ServerLevel level, XoonglinEntity entity, BuilderJob job) {
        if (storageContainerPos == null) {
            state = State.FINDING_SITE;
            return;
        }

        if (fetchWait > 0) {
            fetchWait--;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(storageContainerPos))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            entity.getNavigation().stop();

            List<Container> containers = findStorageContainers(level);
            if (containers.isEmpty()) {
                state = State.FINDING_SITE;
                return;
            }

            for (Container container : containers) {
                takeNeededBlocks(entity, container);
                container.setChanged();
            }

            boolean hasBlocks = hasUsableBuildBlocks(level, entity);
            if (hasBlocks) {
                state = State.TRAVELING_TO_SITE;
                repathTimer = 0;
                navigateTo(entity, buildSiteKeyBlock);
            } else {
                fetchWait = FETCH_RETRY_COOLDOWN;
            }
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, storageContainerPos);
        }
    }

    private void tickTravelingToSite(XoonglinEntity entity) {
        if (buildSiteKeyBlock == null) {
            state = State.FINDING_SITE;
            return;
        }

        BlockPos target = getNextPlacementPos();
        if (target == null) target = buildSiteKeyBlock;

        if (entity.position().distanceTo(Vec3.atCenterOf(target)) < REACH) {
            entity.getNavigation().stop();
            state = State.PLACING_BLOCKS;
            repathTimer = 0;
            return;
        }

        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, target);
        }
    }

    private BlockPos getNextPlacementPos() {
        if (buildPlan == null) return null;
        for (int i = buildPlanIndex; i < buildPlan.size(); i++) {
            return buildPlan.get(i).target;
        }
        return null;
    }

    private void tickPlacingBlocks(ServerLevel level, XoonglinEntity entity) {
        if (buildPlan == null) {
            state = State.FINDING_SITE;
            buildSiteKeyBlock = null;
            return;
        }

        if (buildPlanIndex >= buildPlan.size()) {
            List<BuildPlacement> remaining = new ArrayList<>();
            for (BuildPlacement p : buildPlan) {
                if (!level.getBlockState(p.target).equals(p.state)) {
                    remaining.add(p);
                }
            }
            if (remaining.isEmpty()) {
                state = State.FINDING_SITE;
                buildSiteKeyBlock = null;
                buildPlan = null;
                return;
            }
            if (remaining.size() >= lastRemainingCount) {
                stalledRebuilds++;
            } else {
                stalledRebuilds = 0;
            }
            lastRemainingCount = remaining.size();
            if (stalledRebuilds > MAX_STALLED_REBUILDS) {
                LOGGER.warn("[Build] {} abandoning site {}: {} blocks unplaceable after {} stalled rebuilds",
                        entity.getName().getString(), buildSiteKeyBlock, remaining.size(), stalledRebuilds);
                abandonedSites.add(buildSiteKeyBlock);
                state = State.FINDING_SITE;
                buildSiteKeyBlock = null;
                buildPlan = null;
                return;
            }
            buildPlan = remaining;
            buildPlanIndex = 0;
            return;
        }

        BuilderJob job = getBuilderJob(entity);
        if (job == null) return;

        if (placeCooldown > 0) {
            placeCooldown--;
            return;
        }

        while (buildPlanIndex < buildPlan.size()) {
            BuildPlacement placement = buildPlan.get(buildPlanIndex);

            if (level.getBlockState(placement.target).equals(placement.state)) {
                buildPlanIndex++;
                continue;
            }

            Item neededItem = placement.state.getBlock().asItem();
            int invSlot = findItemInInventory(entity, neededItem);

            if (invSlot < 0) {
                if (hasUsableBuildBlocks(level, entity)) {
                    // Missing this item but can still place others; defer it for later
                    buildPlanIndex++;
                    continue;
                }
                if (hasRemainingWork(level)) {
                    state = State.FETCHING_RESOURCES;
                    repathTimer = 0;
                    navigateTo(entity, storageContainerPos);
                } else {
                    state = State.FINDING_SITE;
                    buildSiteKeyBlock = null;
                    buildPlan = null;
                }
                return;
            }

            if (entity.position().distanceTo(Vec3.atCenterOf(placement.target)) > REACH) {
                occupyTicks = 0;
                if (entity.getNavigation().isDone()) {
                    navFailures++;
                    if (navFailures >= MAX_NAV_FAILURES) {
                        navFailures = 0;
                        buildPlanIndex++;
                        continue;
                    }
                    repathTimer = 0;
                    navigateTo(entity, placement.target);
                } else if (++repathTimer >= REPATH_INTERVAL) {
                    repathTimer = 0;
                    navigateTo(entity, placement.target);
                }
                return;
            }

            navFailures = 0;

            if (entityOccupies(entity, placement.target)) {
                occupyTicks++;
                if (occupyTicks > MAX_OCCUPY_TICKS) {
                    // Couldn't step aside in time; defer this block and continue with the rest
                    occupyTicks = 0;
                    buildPlanIndex++;
                    continue;
                }
                BlockPos safePos = findSafePosition(level, entity, placement.target);
                if (safePos == null) {
                    // Nowhere to step; defer immediately rather than suffocating ourselves
                    occupyTicks = 0;
                    buildPlanIndex++;
                    continue;
                }
                if (entity.getNavigation().isDone()) {
                    navigateTo(entity, safePos);
                }
                return;
            }

            occupyTicks = 0;
            entity.getNavigation().stop();
            level.setBlock(placement.target, placement.state, 3);
            if (placement.state.getBlock() instanceof DoorBlock) {
                level.setBlock(placement.target.above(),
                        placement.state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
            }
            entity.getInventory().removeItem(invSlot, 1);
            buildPlanIndex++;
            placeCooldown = job.getBuildSpeed();
            return;
        }

        state = State.FINDING_SITE;
        buildSiteKeyBlock = null;
        buildPlan = null;
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        buildSiteKeyBlock = null;
        buildStructureTypeId = null;
        buildPlan = null;
        storageContainerPos = null;
        storageStructureKeyPos = null;
    }

    private Structure findClosestTemplate(StructuresData data, XoonglinEntity entity, String structureTypeId) {
        BlockPos entityPos = entity.blockPosition();
        return data.getStructures().stream()
                .filter(s -> s.getStructureTypeId().equals(structureTypeId))
                .filter(s -> s.getLeaderId().equals(entity.getLeaderId()))
                .min(Comparator.comparingInt(s -> s.getKeyBlockPos().distManhattan(entityPos)))
                .orElse(null);
    }

    private List<BuildPlacement> computeBuildPlan(ServerLevel level, Structure template, BlockPos siteKeyBlock) {
        BlockPos templateKey = template.getKeyBlockPos();
        int dx = siteKeyBlock.getX() - templateKey.getX();
        int dy = siteKeyBlock.getY() - templateKey.getY();
        int dz = siteKeyBlock.getZ() - templateKey.getZ();

        List<BuildPlacement> plan = new ArrayList<>();

        List<String> buildOrder = List.of("floor", "ground_perimeter", "wall", "border",
                "surface", "interior", "roof");

        boolean siteKeyIsDoor = level.getBlockState(siteKeyBlock).getBlock() instanceof DoorBlock;

        for (String group : buildOrder) {
            for (BlockPos templatePos : template.getBlockPositions().getOrDefault(group, List.of())) {
                addPlacement(level, plan, templatePos, siteKeyBlock, siteKeyIsDoor, dx, dy, dz);
            }
        }

        for (String group : template.getBlockPositions().keySet()) {
            if (buildOrder.contains(group)) continue;
            for (BlockPos templatePos : template.getBlockPositions().get(group)) {
                addPlacement(level, plan, templatePos, siteKeyBlock, siteKeyIsDoor, dx, dy, dz);
            }
        }

        return plan;
    }

    private void addPlacement(ServerLevel level, List<BuildPlacement> plan, BlockPos templatePos,
                              BlockPos siteKeyBlock, boolean siteKeyIsDoor, int dx, int dy, int dz) {
        BlockState templateState = level.getBlockState(templatePos);
        if (templateState.isAir() || templateState.liquid()) return;
        if (templateState.getBlock().asItem() == net.minecraft.world.item.Items.AIR) return;
        // Upper door halves are placed together with their lower half
        if (templateState.getBlock() instanceof DoorBlock
                && templateState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) return;

        BlockPos targetPos = templatePos.offset(dx, dy, dz);
        // The key block already exists at the site (both halves, if it is a door),
        // even if its orientation differs from the template's
        if (targetPos.equals(siteKeyBlock)) return;
        if (siteKeyIsDoor && targetPos.equals(siteKeyBlock.above())) return;

        if (!level.getBlockState(targetPos).equals(templateState)) {
            plan.add(new BuildPlacement(targetPos, templateState));
        }
    }

    private BlockPos findStorageContainer(ServerLevel level, StructuresData data,
                                          XoonglinEntity entity, BuilderJob job) {
        String storageTypeId = job.getStorageStructure();
        boolean useAssigned = storageTypeId.equals(
                job.getRequiredStructureType() != null ? job.getRequiredStructureType() : "");

        if (useAssigned) {
            BlockPos assignedPos = entity.getAssignedStructurePos(storageTypeId);
            if (assignedPos != null) {
                Structure assigned = data.getStructures().stream()
                        .filter(s -> s.getKeyBlockPos().equals(assignedPos))
                        .findFirst().orElse(null);
                if (assigned != null) {
                    return rememberStorageStructure(level, assigned);
                }
            }
            return null;
        }

        BlockPos entityPos = entity.blockPosition();
        return data.getStructures().stream()
                .filter(s -> s.getStructureTypeId().equals(storageTypeId))
                .filter(s -> s.getLeaderId().equals(entity.getLeaderId()))
                .filter(s -> s.isUser(entity.getUUID()))
                .sorted(Comparator.comparingInt(s -> s.getKeyBlockPos().distManhattan(entityPos)))
                .map(s -> rememberStorageStructure(level, s))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Records the storage structure's key block (so all its containers can be used
     * when fetching) and returns the preferred container position to navigate to.
     */
    private BlockPos rememberStorageStructure(ServerLevel level, Structure structure) {
        List<BlockPos> containerPositions = ContainerUtil.findContainerPositions(level, structure);
        if (containerPositions.isEmpty()) {
            return null;
        }
        storageStructureKeyPos = structure.getKeyBlockPos();
        return containerPositions.get(0);
    }

    private List<Container> findStorageContainers(ServerLevel level) {
        if (storageStructureKeyPos == null) return List.of();
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        Structure storage = data.getStructures().stream()
                .filter(s -> s.getKeyBlockPos().equals(storageStructureKeyPos))
                .findFirst().orElse(null);
        if (storage == null) return List.of();
        return ContainerUtil.findContainers(level, storage);
    }

    private void takeNeededBlocks(XoonglinEntity entity, Container container) {
        if (buildPlan == null) return;
        SimpleContainer inventory = entity.getInventory();

        Map<Item, Integer> needed = new HashMap<>();
        for (int i = buildPlanIndex; i < buildPlan.size(); i++) {
            Item item = buildPlan.get(i).state.getBlock().asItem();
            if (item == net.minecraft.world.item.Items.AIR) continue;
            needed.merge(item, 1, Integer::sum);
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                needed.merge(stack.getItem(), -stack.getCount(), Integer::sum);
            }
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            Integer count = needed.get(stack.getItem());
            if (count == null || count <= 0) continue;

            int takeAmount = Math.min(count, stack.getCount());
            ItemStack taken = container.removeItem(i, takeAmount);
            ItemStack leftover = inventory.addItem(taken);
            if (!leftover.isEmpty()) {
                container.setItem(i, leftover);
                break;
            }
            needed.put(stack.getItem(), count - takeAmount);
        }
    }

    /**
     * True if the inventory holds an item usable for at least one remaining placement.
     * Holding unrelated blocks does not count — that caused fetch/place ping-pong loops.
     */
    private boolean hasUsableBuildBlocks(ServerLevel level, XoonglinEntity entity) {
        if (buildPlan == null) return false;
        for (int i = buildPlanIndex; i < buildPlan.size(); i++) {
            BuildPlacement p = buildPlan.get(i);
            if (level.getBlockState(p.target).equals(p.state)) continue;
            if (findItemInInventory(entity, p.state.getBlock().asItem()) >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRemainingWork(ServerLevel level) {
        if (buildPlan == null) return false;
        for (int i = buildPlanIndex; i < buildPlan.size(); i++) {
            if (!level.getBlockState(buildPlan.get(i).target).equals(buildPlan.get(i).state)) {
                return true;
            }
        }
        return false;
    }

    private String describeMissingItems(ServerLevel level, XoonglinEntity entity) {
        if (buildPlan == null) return "?";
        Map<Item, Integer> missing = new HashMap<>();
        for (int i = buildPlanIndex; i < buildPlan.size(); i++) {
            BuildPlacement p = buildPlan.get(i);
            if (level.getBlockState(p.target).equals(p.state)) continue;
            missing.merge(p.state.getBlock().asItem(), 1, Integer::sum);
        }
        SimpleContainer inventory = entity.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                missing.merge(stack.getItem(), -stack.getCount(), Integer::sum);
            }
        }
        StringBuilder sb = new StringBuilder();
        missing.forEach((item, count) -> {
            if (count > 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(count).append("x ").append(item.getDescription().getString());
            }
        });
        return sb.length() > 0 ? sb.toString() : "nothing";
    }

    private int findItemInInventory(XoonglinEntity entity, Item item) {
        SimpleContainer inventory = entity.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private StructureType findStructureType(String typeId) {
        if (CftRegistry.STRUCTURES == null) return null;
        return CftRegistry.STRUCTURES.stream()
                .filter(st -> st.getId().equals(typeId))
                .findFirst().orElse(null);
    }

    private boolean entityOccupies(XoonglinEntity entity, BlockPos pos) {
        return entity.getBoundingBox().intersects(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
    }

    /**
     * Finds a standable position near the entity that does not intersect the block
     * about to be placed. Prefers positions that are not themselves pending build
     * targets, to avoid oscillating between two cells that both need blocks.
     */
    private BlockPos findSafePosition(ServerLevel level, XoonglinEntity entity, BlockPos avoid) {
        BlockPos feet = entity.blockPosition();
        Set<BlockPos> pending = new HashSet<>();
        if (buildPlan != null) {
            for (int i = buildPlanIndex; i < buildPlan.size(); i++) {
                pending.add(buildPlan.get(i).target);
            }
        }

        BlockPos bestNonPending = null;
        int bestNonPendingDist = Integer.MAX_VALUE;
        BlockPos bestAny = null;
        int bestAnyDist = Integer.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    BlockPos candidate = feet.offset(dx, dy, dz);
                    // Standing here must not intersect the block we want to place
                    if (candidate.equals(avoid) || candidate.above().equals(avoid)) continue;
                    if (!level.getBlockState(candidate).isAir()
                            || !level.getBlockState(candidate.above()).isAir()
                            || level.getBlockState(candidate.below()).isAir()) continue;

                    int dist = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (!pending.contains(candidate) && !pending.contains(candidate.above())) {
                        if (dist < bestNonPendingDist) {
                            bestNonPendingDist = dist;
                            bestNonPending = candidate;
                        }
                    } else if (dist < bestAnyDist) {
                        bestAnyDist = dist;
                        bestAny = candidate;
                    }
                }
            }
        }

        return bestNonPending != null ? bestNonPending : bestAny;
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private BuilderJob getBuilderJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof BuilderJob b ? b : null;
    }
}
