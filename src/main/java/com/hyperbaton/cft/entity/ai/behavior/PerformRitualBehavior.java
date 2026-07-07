package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.AttendanceRule;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.job.OfficiantJob;
import com.hyperbaton.cft.need.RitualNeed;
import com.hyperbaton.cft.need.satisfaction.RitualNeedSatisfier;
import com.hyperbaton.cft.ritual.Ritual;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.world.RitualsData;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Drives the officiant's ritual from start to finish. The Ritual itself is persisted
 * in RitualsData, so if the behavior is interrupted (world reload, behavior timeout)
 * it re-attaches in start() and resumes where it left off. The ritual is only
 * cancelled when the officiant no longer wants to perform it (memory gone) or dies.
 */
public class PerformRitualBehavior extends Behavior<XoonglinEntity> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REPATH_INTERVAL = 40;
    // Ticks to wait before re-checking the container when ingredients are missing
    private static final int INGREDIENT_RETRY_COOLDOWN = 600;
    // How long to wait before retrying after a postponed ritual (attendance not met)
    private static final int POSTPONE_DELAY = 6000;
    // How often to re-check attendance and summon candidates while gathering
    private static final int SUMMON_INTERVAL = 20;
    // How often to verify attendees are still within the ritual radius
    private static final int PRESENCE_CHECK_INTERVAL = 40;
    // Radius within which completed rituals look for xoonglins to satisfy
    private static final int SATISFACTION_SCAN_RADIUS = 128;

    private enum State {
        TRAVELING, CHECKING_INGREDIENTS, SUMMONING, PERFORMING
    }

    private State state;
    private BlockPos templeKeyBlock;
    private int repathTimer;
    private int waitTicks;
    private int intervalTimer;

    public PerformRitualBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 6000);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        return getOfficiantJob(entity) != null
                && entity.getAssignedStructurePos(getOfficiantJob(entity).getRequiredStructureType()) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        OfficiantJob job = getOfficiantJob(entity);
        templeKeyBlock = entity.getAssignedStructurePos(job.getRequiredStructureType());
        repathTimer = 0;
        waitTicks = 0;
        intervalTimer = 0;

        // Re-attach to a persisted ritual if one exists (resume after reload/interruption)
        Optional<Ritual> existing = ritualsData(level).findByOfficiant(entity.getUUID());
        if (existing.isPresent()) {
            state = existing.get().getState() == Ritual.State.GATHERING
                    ? State.SUMMONING : State.PERFORMING;
        } else {
            state = State.TRAVELING;
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_PERFORM_RITUAL.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        OfficiantJob job = getOfficiantJob(entity);
        if (job == null || templeKeyBlock == null) return;

        switch (state) {
            case TRAVELING -> tickTraveling(entity);
            case CHECKING_INGREDIENTS -> tickCheckingIngredients(level, entity, job);
            case SUMMONING -> tickSummoning(level, entity, job);
            case PERFORMING -> tickPerforming(level, entity, job);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        // Only cancel the ritual if the officiant no longer wants to perform it.
        // On a plain behavior timeout the memory is still present and the persisted
        // ritual will be resumed by the next start().
        if (!entity.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_PERFORM_RITUAL.get())) {
            RitualsData data = ritualsData(level);
            data.findByOfficiant(entity.getUUID()).ifPresent(ritual -> {
                boolean completed = ritual.getState() == Ritual.State.IN_PROGRESS
                        && ritual.getTicksRemaining() <= 0;
                if (!completed) {
                    cancelRitual(level, entity, ritual);
                }
            });
        }
    }

    private void tickTraveling(XoonglinEntity entity) {
        if (entity.position().distanceTo(Vec3.atCenterOf(templeKeyBlock))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            entity.getNavigation().stop();
            state = State.CHECKING_INGREDIENTS;
            waitTicks = 0;
            return;
        }
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, templeKeyBlock);
        }
    }

    private void tickCheckingIngredients(ServerLevel level, XoonglinEntity entity, OfficiantJob job) {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        List<Container> containers = findTempleContainers(level, job);
        if (!ContainerUtil.hasAllIngredients(containers, job.getIngredients())) {
            LOGGER.warn("[Ritual] {} cannot start ritual {}: missing ingredients at {}, waiting",
                    entity.getName().getString(), job.getRitualId(), templeKeyBlock);
            waitTicks = INGREDIENT_RETRY_COOLDOWN;
            return;
        }

        Ritual ritual = new Ritual(entity.getUUID(), entity.getLeaderId(), job.getRitualId(),
                templeKeyBlock, job.getRitualRadius(), job.getGatheringTimeout());
        ritualsData(level).addRitual(ritual);
        state = State.SUMMONING;
        intervalTimer = 0;
    }

    private void tickSummoning(ServerLevel level, XoonglinEntity entity, OfficiantJob job) {
        RitualsData data = ritualsData(level);
        Optional<Ritual> maybeRitual = data.findByOfficiant(entity.getUUID());
        if (maybeRitual.isEmpty()) {
            state = State.TRAVELING;
            return;
        }
        Ritual ritual = maybeRitual.get();

        if (ritual.countDown() <= 0) {
            // Attendance minimum was never met; postpone and retry later
            LOGGER.warn("[Ritual] {} postponing ritual {}: attendance not met",
                    entity.getName().getString(), job.getRitualId());
            cancelRitual(level, entity, ritual);
            postpone(level, entity, job);
            return;
        }
        data.setDirty();

        if (++intervalTimer < SUMMON_INTERVAL) {
            return;
        }
        intervalTimer = 0;

        List<XoonglinEntity> present = xoonglinsWithin(level, entity, ritual.getCenter(), ritual.getRadius());
        boolean minimumsMet = attendanceMinimumsMet(job, present);

        if (minimumsMet) {
            // Give latecomers a short grace window to approach the maximum, then start
            if (ritual.getGraceTicks() < 0) {
                ritual.setGraceTicks(job.getGracePeriod());
            } else if (present.size() >= job.getMaxAttendees()
                    || ritual.getGraceTicks() - SUMMON_INTERVAL <= 0) {
                startRitual(level, entity, job, ritual, present);
                return;
            } else {
                ritual.setGraceTicks(ritual.getGraceTicks() - SUMMON_INTERVAL);
            }
        } else {
            ritual.setGraceTicks(-1);
            summonCandidates(level, entity, job, ritual, present);
        }
        data.setDirty();
    }

    private void startRitual(ServerLevel level, XoonglinEntity entity, OfficiantJob job,
                             Ritual ritual, List<XoonglinEntity> present) {
        List<Container> containers = findTempleContainers(level, job);
        if (!ContainerUtil.hasAllIngredients(containers, job.getIngredients())) {
            // Ingredients vanished while gathering; postpone
            LOGGER.warn("[Ritual] {} postponing ritual {}: ingredients disappeared",
                    entity.getName().getString(), job.getRitualId());
            cancelRitual(level, entity, ritual);
            postpone(level, entity, job);
            return;
        }
        ContainerUtil.consumeIngredients(containers, job.getIngredients());

        for (XoonglinEntity attendee : present) {
            if (ritual.getAttendees().size() >= job.getMaxAttendees()) break;
            ritual.addAttendee(attendee.getUUID());
        }
        ritual.begin(job.getDuration());
        ritualsData(level).setDirty();
        state = State.PERFORMING;
        intervalTimer = 0;
    }

    private void tickPerforming(ServerLevel level, XoonglinEntity entity, OfficiantJob job) {
        RitualsData data = ritualsData(level);
        Optional<Ritual> maybeRitual = data.findByOfficiant(entity.getUUID());
        if (maybeRitual.isEmpty()) {
            state = State.TRAVELING;
            return;
        }
        Ritual ritual = maybeRitual.get();

        if (ritual.countDown() <= 0) {
            completeRitual(level, entity, ritual);
            return;
        }
        data.setDirty();

        intervalTimer++;

        // Presence is required from beginning to end: prune attendees who left
        if (intervalTimer % PRESENCE_CHECK_INTERVAL == 0) {
            List<XoonglinEntity> present = xoonglinsWithin(level, entity, ritual.getCenter(), ritual.getRadius());
            ritual.getAttendees().removeIf(uuid ->
                    present.stream().noneMatch(x -> x.getUUID().equals(uuid)));
        }

        // Small ceremonial movements next to the key block
        if (intervalTimer % 60 == 0 && entity.getNavigation().isDone()) {
            BlockPos jitter = templeKeyBlock.offset(
                    entity.getRandom().nextInt(3) - 1, 0, entity.getRandom().nextInt(3) - 1);
            navigateTo(entity, jitter);
        }

        if (intervalTimer % 10 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    entity.getX(), entity.getY() + 1.5, entity.getZ(),
                    5, 0.5, 0.5, 0.5, 0.5);
        }
    }

    private void completeRitual(ServerLevel level, XoonglinEntity entity, Ritual ritual) {
        AABB scanArea = new AABB(ritual.getCenter()).inflate(SATISFACTION_SCAN_RADIUS);
        for (XoonglinEntity xoonglin : level.getEntitiesOfClass(XoonglinEntity.class, scanArea)) {
            if (xoonglin.getLeaderId() == null || !xoonglin.getLeaderId().equals(ritual.getLeaderId())) {
                continue;
            }
            satisfyRitualNeeds(xoonglin, ritual);
        }

        clearAttendMemories(level, ritual);
        entity.getJobState().lastActionGameTime = level.getGameTime();
        ritualsData(level).removeRitual(ritual);
        LOGGER.debug("[Ritual] {} completed ritual {} at {} with {} attendees",
                entity.getName().getString(), ritual.getRitualId(), ritual.getCenter(),
                ritual.getAttendees().size());
    }

    private void satisfyRitualNeeds(XoonglinEntity xoonglin, Ritual ritual) {
        if (xoonglin.getNeeds() == null) return;
        for (Object satisfierObj : xoonglin.getNeeds()) {
            if (!(satisfierObj instanceof RitualNeedSatisfier satisfier)) continue;
            RitualNeed need = satisfier.getNeed();
            if (!need.getRitualId().equals(ritual.getRitualId())) continue;

            boolean qualifies;
            if (need.isRequiresPresence()) {
                // Attendee set was pruned during the ritual, so membership means
                // the xoonglin was present from beginning to end
                qualifies = ritual.isAttendee(xoonglin.getUUID());
            } else {
                qualifies = xoonglin.blockPosition().distManhattan(ritual.getCenter()) <= need.getRadius();
            }
            if (qualifies) {
                satisfier.onRitualCompleted(xoonglin);
            }
        }
    }

    private void cancelRitual(ServerLevel level, XoonglinEntity entity, Ritual ritual) {
        clearAttendMemories(level, ritual);
        ritualsData(level).removeRitual(ritual);
    }

    private void clearAttendMemories(ServerLevel level, Ritual ritual) {
        AABB scanArea = new AABB(ritual.getCenter()).inflate(SATISFACTION_SCAN_RADIUS);
        for (XoonglinEntity xoonglin : level.getEntitiesOfClass(XoonglinEntity.class, scanArea)) {
            xoonglin.getBrain().getMemory(CftMemoryModuleType.MUST_ATTEND_RITUAL.get())
                    .filter(center -> center.equals(ritual.getCenter()))
                    .ifPresent(center ->
                            xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_ATTEND_RITUAL.get()));
        }
    }

    /**
     * Postpones the next attempt by rewinding the last-action time so the ritual
     * becomes due again after POSTPONE_DELAY instead of a full frequency period.
     */
    private void postpone(ServerLevel level, XoonglinEntity entity, OfficiantJob job) {
        entity.getJobState().lastActionGameTime =
                level.getGameTime() - job.getIntervalTicks() + POSTPONE_DELAY;
    }

    private boolean attendanceMinimumsMet(OfficiantJob job, List<XoonglinEntity> present) {
        for (AttendanceRule rule : job.getAttendanceRules()) {
            long count = present.stream()
                    .filter(x -> x.getSocialClass() != null && rule.appliesTo(x.getSocialClass().getId()))
                    .count();
            if (count < rule.min() || count > rule.max()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Summons xoonglins of classes whose attendance minimum is not yet met.
     * Xoonglins with a presence-requiring ritual need volunteer on their own
     * through RitualNeedSatisfier.
     */
    private void summonCandidates(ServerLevel level, XoonglinEntity entity, OfficiantJob job,
                                  Ritual ritual, List<XoonglinEntity> present) {
        AABB summonArea = new AABB(ritual.getCenter()).inflate(job.getSummonRadius());
        List<XoonglinEntity> candidates = level.getEntitiesOfClass(XoonglinEntity.class, summonArea,
                x -> !x.getUUID().equals(entity.getUUID())
                        && x.getLeaderId() != null && x.getLeaderId().equals(ritual.getLeaderId())
                        && x.getSocialClass() != null
                        && !x.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_PERFORM_RITUAL.get()));

        int totalCommitted = (int) (present.size() + candidates.stream()
                .filter(x -> x.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_ATTEND_RITUAL.get()))
                .count());

        for (AttendanceRule rule : job.getAttendanceRules()) {
            long presentCount = present.stream()
                    .filter(x -> rule.appliesTo(x.getSocialClass().getId()))
                    .count();
            long summonedCount = candidates.stream()
                    .filter(x -> rule.appliesTo(x.getSocialClass().getId()))
                    .filter(x -> x.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_ATTEND_RITUAL.get()))
                    .count();
            long shortfall = rule.min() - presentCount - summonedCount;

            for (XoonglinEntity candidate : candidates) {
                if (shortfall <= 0 || totalCommitted >= job.getMaxAttendees()) break;
                if (!rule.appliesTo(candidate.getSocialClass().getId())) continue;
                if (candidate.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_ATTEND_RITUAL.get())) continue;
                candidate.getBrain().setMemory(CftMemoryModuleType.MUST_ATTEND_RITUAL.get(), ritual.getCenter());
                shortfall--;
                totalCommitted++;
            }
        }
    }

    private List<XoonglinEntity> xoonglinsWithin(ServerLevel level, XoonglinEntity officiant,
                                                 BlockPos center, int radius) {
        AABB area = new AABB(center).inflate(radius);
        return level.getEntitiesOfClass(XoonglinEntity.class, area,
                x -> !x.getUUID().equals(officiant.getUUID())
                        && x.getLeaderId() != null && x.getLeaderId().equals(officiant.getLeaderId()));
    }

    private List<Container> findTempleContainers(ServerLevel level, OfficiantJob job) {
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        Structure temple = data.getStructures().stream()
                .filter(s -> s.getKeyBlockPos().equals(templeKeyBlock))
                .filter(s -> s.getStructureTypeId().equals(job.getRequiredStructureType()))
                .findFirst().orElse(null);
        if (temple == null) return List.of();
        return ContainerUtil.findContainers(level, temple);
    }

    private RitualsData ritualsData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RitualsData.factory(), "ritualsData");
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private OfficiantJob getOfficiantJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof OfficiantJob o ? o : null;
    }
}
