package com.hyperbaton.cft.api.event;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import net.minecraft.resources.ResourceLocation;

/**
 * One of a Xoonglin's needs becomes satisfied, or stops being satisfied. Needs are checked once
 * per second; the need's satisfier posts this event right after its check, only when its state
 * changed. The rest of the Xoonglin's needs are still being checked, so don't change its class or
 * needs from a listener.
 */
public class NeedStateChangeEvent extends XoonglinEvent {
    private final NeedSatisfier<? extends Need> satisfier;

    public NeedStateChangeEvent(XoonglinEntity xoonglin, NeedSatisfier<? extends Need> satisfier) {
        super(xoonglin);
        this.satisfier = satisfier;
    }

    /** The need's state in this Xoonglin: its satisfaction, and the need itself with {@code getNeed()}. */
    public NeedSatisfier<? extends Need> getSatisfier() {
        return satisfier;
    }

    public ResourceLocation getNeedId() {
        return satisfier.getNeedId();
    }

    /** Whether the need is now satisfied; if not, it just stopped being so. */
    public boolean isSatisfied() {
        return satisfier.isSatisfied();
    }
}
