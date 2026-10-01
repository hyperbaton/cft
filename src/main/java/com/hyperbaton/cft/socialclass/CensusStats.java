package com.hyperbaton.cft.socialclass;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.util.SocialStructureUtil;
import com.hyperbaton.cft.world.PopulationHistoryData;
import com.hyperbaton.cft.world.PopulationSnapshot;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

/**
 * Aggregated view of a leader's Xoonglins, as shown in the census.
 *
 * @param population       Xoonglins per social class id
 * @param averageHappiness average happiness per social class id
 * @param needIssues       visible, non-bonus needs currently unsatisfied by at least one Xoonglin
 * @param jobs             Xoonglins per job id; {@link #NO_JOB} counts those without a job
 * @param history          daily snapshots, oldest first
 */
public record CensusStats(
        Map<ResourceLocation, Integer> population,
        Map<ResourceLocation, Double> averageHappiness,
        List<NeedIssue> needIssues,
        Map<String, Integer> jobs,
        List<PopulationSnapshot> history
) {
    public static final String NO_JOB = "";
    private static final int SNAPSHOT_STARTUP_GRACE_TICKS = 1200;

    public static final StreamCodec<ByteBuf, CensusStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), CensusStats::population,
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.DOUBLE), CensusStats::averageHappiness,
            NeedIssue.STREAM_CODEC.apply(ByteBufCodecs.list()), CensusStats::needIssues,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), CensusStats::jobs,
            PopulationSnapshot.STREAM_CODEC.apply(ByteBufCodecs.list()), CensusStats::history,
            CensusStats::new
    );

    /**
     * @param unsatisfied Xoonglins with this need below its satisfaction threshold
     * @param critical    of those, the ones below the damage threshold of a harmful need
     */
    public record NeedIssue(ResourceLocation needId, int unsatisfied, int critical) {
        public static final StreamCodec<ByteBuf, NeedIssue> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, NeedIssue::needId,
                ByteBufCodecs.VAR_INT, NeedIssue::unsatisfied,
                ByteBufCodecs.VAR_INT, NeedIssue::critical,
                NeedIssue::new
        );
    }

    public int totalPopulation() {
        return population.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static CensusStats build(ServerLevel level, UUID leaderId) {
        List<XoonglinEntity> xoonglins = SocialStructureUtil.getLeaderXoonglins(level, leaderId);

        Map<ResourceLocation, Integer> population = new HashMap<>();
        Map<ResourceLocation, Double> happinessSums = new HashMap<>();
        Map<ResourceLocation, int[]> issueCounts = new HashMap<>();
        Map<String, Integer> jobs = new HashMap<>();

        for (XoonglinEntity xoonglin : xoonglins) {
            ResourceLocation classId = xoonglin.getSocialClassId();
            population.merge(classId, 1, Integer::sum);
            happinessSums.merge(classId, xoonglin.getHappiness(), Double::sum);
            jobs.merge(xoonglin.getJob() != null ? xoonglin.getJob().toString() : NO_JOB, 1, Integer::sum);

            for (NeedSatisfier<? extends Need> satisfier : xoonglin.getUnsatisfiedVisibleNeeds()) {
                int[] counts = issueCounts.computeIfAbsent(satisfier.getNeedId(), id -> new int[2]);
                counts[0]++;
                if (XoonglinEntity.isCritical(satisfier)) {
                    counts[1]++;
                }
            }
        }

        Map<ResourceLocation, Double> averageHappiness = new HashMap<>();
        happinessSums.forEach((classId, sum) -> averageHappiness.put(classId, sum / population.get(classId)));

        List<NeedIssue> needIssues = new ArrayList<>();
        issueCounts.forEach((needId, counts) -> needIssues.add(new NeedIssue(needId, counts[0], counts[1])));
        needIssues.sort(Comparator.comparingInt(NeedIssue::critical)
                .thenComparingInt(NeedIssue::unsatisfied)
                .reversed());

        List<PopulationSnapshot> history = getHistoryData(level).getHistory(leaderId);

        return new CensusStats(population, averageHappiness, needIssues, jobs, history);
    }

    /**
     * Records one snapshot per leader with loaded Xoonglins in this level, once per in-game day.
     * Skipped right after startup, while Xoonglins are still being loaded, to avoid recording
     * a partial population.
     */
    public static void recordDailySnapshots(ServerLevel level) {
        if (level.getServer().getTickCount() < SNAPSHOT_STARTUP_GRACE_TICKS) return;
        long day = level.getGameTime() / 24000L;
        PopulationHistoryData historyData = getHistoryData(level);
        if (historyData.getLastRecordedDay() == day) return;

        Map<UUID, List<XoonglinEntity>> xoonglinsByLeader = new HashMap<>();
        for (XoonglinEntity xoonglin : SocialStructureUtil.getAllXoonglins(level)) {
            if (xoonglin.getLeaderId() != null && xoonglin.getSocialClass() != null) {
                xoonglinsByLeader.computeIfAbsent(xoonglin.getLeaderId(), id -> new ArrayList<>()).add(xoonglin);
            }
        }
        xoonglinsByLeader.forEach((leaderId, xoonglins) -> historyData.record(leaderId, snapshotOf(day, xoonglins)));
        historyData.setLastRecordedDay(day);
    }

    private static PopulationSnapshot snapshotOf(long day, List<XoonglinEntity> xoonglins) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        double happinessSum = 0;
        for (XoonglinEntity xoonglin : xoonglins) {
            counts.merge(xoonglin.getSocialClassId(), 1, Integer::sum);
            happinessSum += xoonglin.getHappiness();
        }
        return new PopulationSnapshot(day, counts, xoonglins.isEmpty() ? 0 : happinessSum / xoonglins.size());
    }

    private static PopulationHistoryData getHistoryData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PopulationHistoryData.factory(), PopulationHistoryData.DATA_NAME);
    }
}
