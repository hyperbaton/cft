package com.hyperbaton.cft.api.event;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * A Xoonglin is promoted or demoted to another social class, because its happiness, needs and
 * the social structure call for it.
 */
public abstract class SocialClassChangeEvent extends XoonglinEvent {
    private final ResourceLocation previousClass;
    private final boolean upgrade;

    protected SocialClassChangeEvent(XoonglinEntity xoonglin, ResourceLocation previousClass, boolean upgrade) {
        super(xoonglin);
        this.previousClass = previousClass;
        this.upgrade = upgrade;
    }

    /** The class it had. */
    public ResourceLocation getPreviousClass() {
        return previousClass;
    }

    /** Whether it's a promotion (one of the class's upgrades) rather than a demotion. */
    public boolean isUpgrade() {
        return upgrade;
    }

    /**
     * Before the change. Cancel it to keep the Xoonglin in its class, or send it to another class
     * with {@link #setNextClass}. While the change still applies, it's tried again, and this event
     * posted again, on every needs check (once per second).
     */
    public static class Pre extends SocialClassChangeEvent implements ICancellableEvent {
        private ResourceLocation nextClass;

        public Pre(XoonglinEntity xoonglin, ResourceLocation previousClass, ResourceLocation nextClass, boolean upgrade) {
            super(xoonglin, previousClass, upgrade);
            this.nextClass = nextClass;
        }

        /** The class it's moving to. */
        public ResourceLocation getNextClass() {
            return nextClass;
        }

        /** Moves it to another class instead; it must be a registered social class. */
        public void setNextClass(ResourceLocation nextClass) {
            this.nextClass = nextClass;
        }
    }

    /**
     * After the change: the Xoonglin has the new class, with its needs, job and health, and has
     * left the structures it used.
     */
    public static class Post extends SocialClassChangeEvent {
        private final ResourceLocation newClass;

        public Post(XoonglinEntity xoonglin, ResourceLocation previousClass, ResourceLocation newClass, boolean upgrade) {
            super(xoonglin, previousClass, upgrade);
            this.newClass = newClass;
        }

        /** The class it has now. */
        public ResourceLocation getNewClass() {
            return newClass;
        }
    }
}
