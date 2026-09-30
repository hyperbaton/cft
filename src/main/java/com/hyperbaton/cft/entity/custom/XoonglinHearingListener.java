package com.hyperbaton.cft.entity.custom;

import com.hyperbaton.cft.util.HearingUtil;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

/**
 * Hears the game events (vibrations) around a Xoonglin and passes them on to its hearing needs.
 */
public class XoonglinHearingListener implements GameEventListener {

    /**
     * The furthest any vanilla game event reaches (the sculk shrieker's). Every event is only sent
     * as far as its own reach, which is 16 blocks for most and 10 for jukeboxes.
     */
    private static final int LISTENER_RADIUS = 32;

    private final XoonglinEntity xoonglin;
    private final PositionSource listenerSource;

    public XoonglinHearingListener(XoonglinEntity xoonglin) {
        this.xoonglin = xoonglin;
        this.listenerSource = new EntityPositionSource(xoonglin, xoonglin.getEyeHeight());
    }

    @Override
    public PositionSource getListenerSource() {
        return listenerSource;
    }

    @Override
    public int getListenerRadius() {
        return LISTENER_RADIUS;
    }

    @Override
    public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> gameEvent, GameEvent.Context context, Vec3 position) {
        if (context.sourceEntity() == xoonglin) return false;
        HearingUtil.forEachHearingSatisfier(xoonglin,
                satisfier -> satisfier.onGameEventHeard(xoonglin, gameEvent, position, context));
        return true;
    }
}
