package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.BuilderJob;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
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

    private static final int REPATH_INTERVAL = 40;
    private static final double REACH = 3.0;
    private static final int BLOCKS_PER_SITE_SCAN = 4096;

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
    private int repathTimer;
    private int placeCooldown;
    private int navFailures;

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
        repathTimer = 0;
        placeCooldown = 0;
        navFailures = 0;
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
            state = State.FINDING_SITE;
            buildSiteKeyBlock = null;
            return;
        }

        buildPlan = computeBuildPlan(level, template, buildSiteKeyBlock);
        buildPlanIndex = 0;

        if (buildPlan.isEmpty()) {
            state = State.FINDING_SITE;
            buildSiteKeyBlock = null;
            return;
        }

        storageContainerPos = findStorageContainer(level, data, entity, job);
        if (storageContainerPos == null) {
            state = State.FINDING_SITE;
            buildSiteKeyBlock = null;
            return;
        }

        state = State.FETCHING_RESOURCES;
        repathTimer = 0;
        navigateTo(entity, storageContainerPos);
    }

    private void tickFetchingResources(ServerLevel level, XoonglinEntity entity, BuilderJob job) {
        if (storageContainerPos == null) {
            state = State.FINDING_SITE;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(storageContainerPos)) < REACH) {
            entity.getNavigation().stop();

            if (!(level.getBlockEntity(storageContainerPos) instanceof Container container)) {
                state = State.FINDING_SITE;
                return;
            }

            takeNeededBlocks(entity, container);
            container.setChanged();

            boolean hasBlocks = hasAnyBuildBlocks(entity);
            if (hasBlocks) {
                state = State.TRAVELING_TO_SITE;
                repathTimer = 0;
                navigateTo(entity, buildSiteKeyBlock);
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
                BlockPos safePos = findSafePosition(level, entity, placement.target);
                if (safePos != null) {
                    if (entity.getNavigation().isDone()) {
                        navigateTo(entity, safePos);
                    }
                    return;
                }
            }

            entity.getNavigation().stop();
            level.setBlock(placement.target, placement.state, 3);
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

        for (String group : buildOrder) {
            List<BlockPos> positions = template.getBlockPositions().getOrDefault(group, List.of());
            for (BlockPos templatePos : positions) {
                BlockState templateState = level.getBlockState(templatePos);
                if (templateState.isAir() || templateState.liquid()) continue;
                if (templateState.getBlock().asItem() == net.minecraft.world.item.Items.AIR) continue;

                BlockPos targetPos = templatePos.offset(dx, dy, dz);
                if (targetPos.equals(siteKeyBlock)) continue;

                if (!level.getBlockState(targetPos).equals(templateState)) {
                    plan.add(new BuildPlacement(targetPos, templateState));
                }
            }
        }

        for (String group : template.getBlockPositions().keySet()) {
            if (buildOrder.contains(group)) continue;
            List<BlockPos> positions = template.getBlockPositions().get(group);
            for (BlockPos templatePos : positions) {
                BlockState templateState = level.getBlockState(templatePos);
                if (templateState.isAir() || templateState.liquid()) continue;
                if (templateState.getBlock().asItem() == net.minecraft.world.item.Items.AIR) continue;

                BlockPos targetPos = templatePos.offset(dx, dy, dz);
                if (targetPos.equals(siteKeyBlock)) continue;

                if (!level.getBlockState(targetPos).equals(templateState)) {
                    plan.add(new BuildPlacement(targetPos, templateState));
                }
            }
        }

        return plan;
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
                    return findContainerInStructure(level, assigned);
                }
            }
            return null;
        }

        BlockPos entityPos = entity.blockPosition();
        return data.getStructures().stream()
                .filter(s -> s.getStructureTypeId().equals(storageTypeId))
                .filter(s -> s.getLeaderId().equals(entity.getLeaderId()))
                .sorted(Comparator.comparingInt(s -> s.getKeyBlockPos().distManhattan(entityPos)))
                .map(s -> findContainerInStructure(level, s))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private BlockPos findContainerInStructure(ServerLevel level, Structure structure) {
        for (List<BlockPos> blocks : structure.getBlockPositions().values()) {
            for (BlockPos pos : blocks) {
                if (level.getBlockEntity(pos) instanceof Container) {
                    return pos;
                }
            }
        }
        return null;
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

    private boolean hasAnyBuildBlocks(XoonglinEntity entity) {
        SimpleContainer inventory = entity.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
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

    private BlockPos findSafePosition(ServerLevel level, XoonglinEntity entity, BlockPos avoid) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos candidate = avoid.relative(dir);
            if (level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()
                    && !level.getBlockState(candidate.below()).isAir()) {
                return candidate;
            }
        }
        return null;
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
