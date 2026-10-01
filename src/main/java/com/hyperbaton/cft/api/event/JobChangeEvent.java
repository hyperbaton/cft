package com.hyperbaton.cft.api.event;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A Xoonglin changes its job. Not posted for the first job of a new Xoonglin.
 */
public abstract class JobChangeEvent extends XoonglinEvent {

    /** Why the job changes. */
    public enum Cause {
        /** CFT picked a new one: the Xoonglin's job didn't fit its new class, or its age. */
        AUTOMATIC,
        /** Its leader picked it, in the job tab. */
        PLAYER
    }

    @Nullable
    private final ResourceLocation previousJob;
    @Nullable
    private final ResourceLocation newJob;
    private final Cause cause;

    protected JobChangeEvent(XoonglinEntity xoonglin, @Nullable ResourceLocation previousJob,
                             @Nullable ResourceLocation newJob, Cause cause) {
        super(xoonglin);
        this.previousJob = previousJob;
        this.newJob = newJob;
        this.cause = cause;
    }

    /** The job it had; null if it had none. */
    @Nullable
    public ResourceLocation getPreviousJob() {
        return previousJob;
    }

    /** The job it gets; null if it's left without one (no job of its class fits it). */
    @Nullable
    public ResourceLocation getNewJob() {
        return newJob;
    }

    public Cause getCause() {
        return cause;
    }

    /**
     * Before the change. Cancel it to keep the Xoonglin's job. An automatic change is tried again
     * the next time the Xoonglin changes class or grows up.
     */
    public static class Pre extends JobChangeEvent implements ICancellableEvent {
        public Pre(XoonglinEntity xoonglin, @Nullable ResourceLocation previousJob, @Nullable ResourceLocation newJob,
                   Cause cause) {
            super(xoonglin, previousJob, newJob, cause);
        }
    }

    /** After the change: the Xoonglin has left the structures of its old job and starts the new one afresh. */
    public static class Post extends JobChangeEvent {
        public Post(XoonglinEntity xoonglin, @Nullable ResourceLocation previousJob, @Nullable ResourceLocation newJob,
                    Cause cause) {
            super(xoonglin, previousJob, newJob, cause);
        }
    }
}
