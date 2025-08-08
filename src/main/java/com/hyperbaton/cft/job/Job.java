package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;

public abstract class Job {

    public static final Codec<Job> JOB_CODEC = ExtraCodecs.lazyInitializedCodec(() -> CftRegistry.JOBS_CODEC_SUPPLIER.get().getCodec()
            .dispatch(Job::jobType, Function.identity()));
    // Called from the entity’s server tick
    public abstract void tick(XoonglinEntity xoonglin, JobState state);

    // Optional: small title used for UI/debug
    String idHint() { return getClass().getSimpleName(); }

    public abstract Codec<? extends Job> jobType();

    // Per-entity save/load helpers for job-specific state if needed
    void saveExtra(CompoundTag tag) {}
    void loadExtra(CompoundTag tag) {}
}
