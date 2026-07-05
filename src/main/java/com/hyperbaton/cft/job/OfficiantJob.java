package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.ritual.Ritual;
import com.hyperbaton.cft.world.RitualsData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

/**
 * Performs a ritual at the key block of its required structure every `frequency` days,
 * consuming ingredients from the structure's container. The ritual starts once the
 * attendance rules are met and satisfies nearby xoonglins' ritual needs on completion.
 */
public class OfficiantJob extends Job {

    public static final Codec<OfficiantJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("ritual_id").forGetter(OfficiantJob::getRitualId),
            Codec.STRING.fieldOf("required_structure").forGetter(j -> j.requiredStructure),
            Codec.DOUBLE.fieldOf("frequency").forGetter(OfficiantJob::getFrequency),
            Codec.INT.fieldOf("duration").forGetter(OfficiantJob::getDuration),
            RitualIngredient.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(OfficiantJob::getIngredients),
            Codec.INT.optionalFieldOf("summon_radius", 32).forGetter(OfficiantJob::getSummonRadius),
            Codec.INT.optionalFieldOf("ritual_radius", 8).forGetter(OfficiantJob::getRitualRadius),
            AttendanceRule.CODEC.listOf().optionalFieldOf("attendance", List.of()).forGetter(OfficiantJob::getAttendanceRules),
            Codec.INT.optionalFieldOf("max_attendees", Integer.MAX_VALUE).forGetter(OfficiantJob::getMaxAttendees),
            Codec.INT.optionalFieldOf("gathering_timeout", 1200).forGetter(OfficiantJob::getGatheringTimeout),
            Codec.INT.optionalFieldOf("grace_period", 200).forGetter(OfficiantJob::getGracePeriod),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds)
    ).apply(inst, OfficiantJob::new));

    public record RitualIngredient(Ingredient ingredient, int quantity) {
        public static final Codec<RitualIngredient> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                INGREDIENT_CODEC.fieldOf("item").forGetter(RitualIngredient::ingredient),
                Codec.INT.fieldOf("quantity").forGetter(RitualIngredient::quantity)
        ).apply(inst, RitualIngredient::new));
    }

    private final String ritualId;
    private final String requiredStructure;
    private final double frequency;
    private final int duration;
    private final List<RitualIngredient> ingredients;
    private final int summonRadius;
    private final int ritualRadius;
    private final List<AttendanceRule> attendanceRules;
    private final int maxAttendees;
    private final int gatheringTimeout;
    private final int gracePeriod;

    public OfficiantJob(String ritualId, String requiredStructure, double frequency, int duration,
                        List<RitualIngredient> ingredients, int summonRadius, int ritualRadius,
                        List<AttendanceRule> attendanceRules, int maxAttendees,
                        int gatheringTimeout, int gracePeriod, List<String> requiredNeeds) {
        super(requiredNeeds);
        this.ritualId = ritualId;
        this.requiredStructure = requiredStructure;
        this.frequency = frequency;
        this.duration = duration;
        this.ingredients = List.copyOf(ingredients);
        this.summonRadius = summonRadius;
        this.ritualRadius = ritualRadius;
        this.attendanceRules = List.copyOf(attendanceRules);
        this.maxAttendees = maxAttendees;
        this.gatheringTimeout = gatheringTimeout;
        this.gracePeriod = gracePeriod;
    }

    public String getRitualId() {
        return ritualId;
    }

    public double getFrequency() {
        return frequency;
    }

    public int getDuration() {
        return duration;
    }

    public List<RitualIngredient> getIngredients() {
        return ingredients;
    }

    public int getSummonRadius() {
        return summonRadius;
    }

    public int getRitualRadius() {
        return ritualRadius;
    }

    public List<AttendanceRule> getAttendanceRules() {
        return attendanceRules;
    }

    public int getMaxAttendees() {
        return maxAttendees;
    }

    public int getGatheringTimeout() {
        return gatheringTimeout;
    }

    public int getGracePeriod() {
        return gracePeriod;
    }

    @Override
    public String getRequiredStructureType() {
        return requiredStructure;
    }

    public long getIntervalTicks() {
        return Math.round(frequency * 24000);
    }

    public boolean isRitualDue(Level level, JobState state) {
        return level.getGameTime() - state.lastActionGameTime >= getIntervalTicks();
    }

    @Override
    public void tick(XoonglinEntity xoonglin, JobState state) {
        Level level = xoonglin.level();
        if (level.isClientSide) return;

        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        boolean needsStructure = xoonglin.getAssignedStructurePos(requiredStructure) == null;

        if (needsStructure) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), requiredStructure);
            brain.eraseMemory(CftMemoryModuleType.MUST_PERFORM_RITUAL.get());
        } else if (isRitualDue(level, state) && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_PERFORM_RITUAL.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_PERFORM_RITUAL.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    @Override
    public void eraseMemories(XoonglinEntity xoonglin) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.MUST_PERFORM_RITUAL.get());
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        Level level = xoonglin.level();
        boolean needsStructure = xoonglin.getAssignedStructurePos(requiredStructure) == null;
        boolean canDoWork = canWork(xoonglin);

        Optional<Ritual> activeRitual = Optional.empty();
        if (level instanceof ServerLevel serverLevel) {
            RitualsData data = serverLevel.getDataStorage().computeIfAbsent(RitualsData.factory(), "ritualsData");
            activeRitual = data.findByOfficiant(xoonglin.getUUID());
        }

        String statusKey;
        int statusColor;
        if (needsStructure) {
            statusKey = "gui.cft.job_status.no_structure";
            statusColor = 0xDD4040;
        } else if (!canDoWork) {
            statusKey = "gui.cft.job_status.cant_work";
            statusColor = 0xDD4040;
        } else if (activeRitual.isPresent() && activeRitual.get().getState() == Ritual.State.IN_PROGRESS) {
            statusKey = "gui.cft.job_status.performing_ritual";
            statusColor = 0x40AA40;
        } else if (activeRitual.isPresent()) {
            statusKey = "gui.cft.job_status.gathering_attendees";
            statusColor = 0x40AA40;
        } else if (isRitualDue(level, state)) {
            statusKey = "gui.cft.job_status.working";
            statusColor = 0x40AA40;
        } else {
            statusKey = "gui.cft.job_status.resting";
            statusColor = 0xDDAA00;
        }

        List<JobDisplayEntry> entries = new ArrayList<>();

        long interval = getIntervalTicks();
        long elapsed = Math.min(Math.max(level.getGameTime() - state.lastActionGameTime, 0), interval);
        entries.add(JobDisplayEntry.progress("gui.cft.job_next_ritual", (int) elapsed, (int) interval));

        if (activeRitual.isPresent()) {
            Ritual ritual = activeRitual.get();
            if (ritual.getState() == Ritual.State.IN_PROGRESS) {
                entries.add(JobDisplayEntry.progress("gui.cft.job_ritual_progress",
                        duration - ritual.getTicksRemaining(), duration));
            }
            entries.add(JobDisplayEntry.text("gui.cft.job_attendees",
                    String.valueOf(ritual.getAttendees().size()), 0xFFFFFF));
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.OFFICIANT_JOB.get();
    }
}
