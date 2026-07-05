package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.network.JobInfoData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public abstract class Job {

    public static final Codec<Job> JOB_CODEC = Codec.lazyInitialized(() -> CftRegistry.JOBS_CODEC_REGISTRY.byNameCodec()
            .dispatch("type", Job::jobType, codec -> MapCodec.assumeMapUnsafe(codec)));

    private final List<String> requiredNeeds;

    protected Job(List<String> requiredNeeds) {
        this.requiredNeeds = requiredNeeds != null ? List.copyOf(requiredNeeds) : List.of();
    }

    public abstract void tick(XoonglinEntity xoonglin, JobState state);

    /**
     * Erases the brain memories this job sets to trigger its behaviors. Called instead
     * of tick() while the job is preempted (e.g. the xoonglin was summoned to attend a
     * ritual), so the job's behaviors stop cleanly until the job resumes ticking.
     */
    public abstract void eraseMemories(XoonglinEntity xoonglin);

    public abstract JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state);

    String idHint() { return getClass().getSimpleName(); }

    public abstract Codec<? extends Job> jobType();

    public List<String> getRequiredNeeds() {
        return requiredNeeds;
    }

    public boolean canWork(XoonglinEntity xoonglin) {
        if (!xoonglin.allDamagingNeedsSatisfied()) return false;

        if (!requiredNeeds.isEmpty() && xoonglin.getNeeds() != null) {
            for (String needId : requiredNeeds) {
                boolean satisfied = xoonglin.getNeeds().stream()
                        .filter(ns -> ns.getNeed().getId().equals(needId))
                        .anyMatch(NeedSatisfier::isSatisfied);
                if (!satisfied) return false;
            }
        }

        return true;
    }

    public String getRequiredStructureType() {
        return null;
    }

}
