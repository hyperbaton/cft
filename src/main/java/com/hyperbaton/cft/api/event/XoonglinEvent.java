package com.hyperbaton.cft.api.event;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.neoforged.bus.api.Event;

/**
 * Something that happens to a Xoonglin, posted on the NeoForge event bus ({@code NeoForge.EVENT_BUS})
 * on the server, as vanilla's {@code LivingEvent} is for living entities.
 */
public abstract class XoonglinEvent extends Event {
    private final XoonglinEntity xoonglin;

    protected XoonglinEvent(XoonglinEntity xoonglin) {
        this.xoonglin = xoonglin;
    }

    public XoonglinEntity getXoonglin() {
        return xoonglin;
    }
}
