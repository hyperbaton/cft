package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.SocializeNeed;
import com.hyperbaton.cft.need.satisfaction.SocializeNeedSatisfier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

/**
 * Free time behavior for socialize needs: having a conversation. A Xoonglin with MUST_SOCIALIZE
 * picks the nearest free Xoonglin its needs accept and both get each other in
 * CONVERSATION_PARTNER. They walk towards each other and talk; when the one who started it
 * finishes, the socialize needs of both are satisfied (as long as each accepts the other's class).
 * <p>
 * Both Xoonglins run this behavior. If either stops (e.g. it has to go to work), it forgets
 * its partner, and the other one stops too, since the partnership is no longer mutual.
 */
public class ConverseBehavior extends Behavior<XoonglinEntity> {
    /** Enough to walk to the partner and talk; a conversation that takes longer is given up. */
    private static final int MAX_DURATION = 1200;
    private static final double WALK_SPEED = 0.8;
    private static final double TALK_DISTANCE_SQR = 2.5 * 2.5;
    private static final int REPATH_INTERVAL = 20;
    private static final int SEARCH_INTERVAL = 40;
    private static final int SOUND_INTERVAL = 60;

    @Nullable
    private XoonglinEntity pendingPartner;
    private boolean initiator;
    private int duration;
    private int chatTicks;
    private int ticksUntilRepath;
    private int ticksUntilSearch;

    public ConverseBehavior() {
        super(Map.of(), MAX_DURATION);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity xoonglin) {
        if (xoonglin.isSleeping()) return false;
        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        // Someone else started a conversation with this Xoonglin
        if (brain.hasMemoryValue(CftMemoryModuleType.CONVERSATION_PARTNER.get())) {
            return partnerOf(level, xoonglin).isPresent();
        }

        if (!brain.hasMemoryValue(CftMemoryModuleType.MUST_SOCIALIZE.get()) || --ticksUntilSearch > 0) return false;
        ticksUntilSearch = SEARCH_INTERVAL;
        pendingPartner = findPartner(level, xoonglin).orElse(null);
        return pendingPartner != null;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        // Activity changes don't stop running behaviors, so leave as soon as IDLE isn't active
        return !xoonglin.isSleeping()
                && xoonglin.getBrain().isActive(Activity.IDLE)
                && partnerOf(level, xoonglin).filter(ConverseBehavior::isAvailable).isPresent();
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        initiator = pendingPartner != null;
        if (initiator) {
            xoonglin.getBrain().setMemory(CftMemoryModuleType.CONVERSATION_PARTNER.get(), pendingPartner.getStringUUID());
            pendingPartner.getBrain().setMemory(CftMemoryModuleType.CONVERSATION_PARTNER.get(), xoonglin.getStringUUID());
            duration = socializeNeeds(xoonglin).mapToInt(SocializeNeed::getDuration).max().orElse(0);
            pendingPartner = null;
        }
        chatTicks = 0;
        ticksUntilRepath = 0;
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        Optional<XoonglinEntity> partnerOptional = partnerOf(level, xoonglin);
        if (partnerOptional.isEmpty()) return;
        XoonglinEntity partner = partnerOptional.get();

        if (xoonglin.distanceToSqr(partner) > TALK_DISTANCE_SQR) {
            if (--ticksUntilRepath <= 0) {
                xoonglin.getNavigation().moveTo(partner, WALK_SPEED);
                ticksUntilRepath = REPATH_INTERVAL;
            }
            return;
        }

        xoonglin.getNavigation().stop();
        xoonglin.getLookControl().setLookAt(partner, 30.0F, 30.0F);
        if (chatTicks++ % SOUND_INTERVAL == 0) {
            xoonglin.playAmbientSound();
        }
        if (initiator && chatTicks >= duration) {
            finishConversation(level, xoonglin, partner);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity xoonglin, long gameTime) {
        xoonglin.getBrain().eraseMemory(CftMemoryModuleType.CONVERSATION_PARTNER.get());
        xoonglin.getNavigation().stop();
        ticksUntilSearch = SEARCH_INTERVAL;
    }

    private static void finishConversation(ServerLevel level, XoonglinEntity xoonglin, XoonglinEntity partner) {
        satisfySocializeNeeds(xoonglin, partner);
        satisfySocializeNeeds(partner, xoonglin);
        for (XoonglinEntity participant : List.of(xoonglin, partner)) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, participant.getX(),
                    participant.getY() + participant.getBbHeight() + 0.3, participant.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
            participant.getBrain().eraseMemory(CftMemoryModuleType.CONVERSATION_PARTNER.get());
        }
    }

    private static void satisfySocializeNeeds(XoonglinEntity xoonglin, XoonglinEntity partner) {
        if (xoonglin.getNeeds() == null) return;
        xoonglin.getNeeds().stream()
                .filter(SocializeNeedSatisfier.class::isInstance)
                .map(SocializeNeedSatisfier.class::cast)
                .filter(satisfier -> satisfier.getNeed().acceptsClass(partner.getSocialClass().getId()))
                .forEach(satisfier -> satisfier.onSocialized(xoonglin));
    }

    /** The partner in CONVERSATION_PARTNER, as long as it also has this Xoonglin as its partner. */
    private static Optional<XoonglinEntity> partnerOf(ServerLevel level, XoonglinEntity xoonglin) {
        String ownId = xoonglin.getStringUUID();
        return xoonglin.getBrain().getMemory(CftMemoryModuleType.CONVERSATION_PARTNER.get())
                .map(id -> level.getEntity(UUID.fromString(id)))
                .filter(XoonglinEntity.class::isInstance)
                .map(XoonglinEntity.class::cast)
                .filter(partner -> partner.isAlive() && partner.getBrain()
                        .getMemory(CftMemoryModuleType.CONVERSATION_PARTNER.get())
                        .map(ownId::equals)
                        .orElse(false));
    }

    /** The nearest Xoonglin that is free to talk and that one of the socialize needs accepts. */
    private static Optional<XoonglinEntity> findPartner(ServerLevel level, XoonglinEntity xoonglin) {
        List<SocializeNeed> needs = socializeNeeds(xoonglin).toList();
        int radius = needs.stream().mapToInt(SocializeNeed::getRadius).max().orElse(0);
        if (radius <= 0) return Optional.empty();

        return level.getEntitiesOfClass(XoonglinEntity.class, xoonglin.getBoundingBox().inflate(radius),
                        candidate -> candidate != xoonglin
                                && candidate.getSocialClass() != null
                                && Objects.equals(candidate.getLeaderId(), xoonglin.getLeaderId())
                                && isAvailable(candidate)
                                && !candidate.getBrain().hasMemoryValue(CftMemoryModuleType.CONVERSATION_PARTNER.get())
                                && !candidate.getBrain().hasMemoryValue(CftMemoryModuleType.MUST_VISIT.get())
                                && needs.stream().anyMatch(need -> need.acceptsClass(candidate.getSocialClass().getId())))
                .stream()
                .min(Comparator.comparingDouble(xoonglin::distanceToSqr));
    }

    /** Awake and in its free time, rather than working, resting or busy with its needs. */
    private static boolean isAvailable(XoonglinEntity xoonglin) {
        return xoonglin.isAlive() && !xoonglin.isSleeping() && xoonglin.getBrain().isActive(Activity.IDLE);
    }

    private static Stream<SocializeNeed> socializeNeeds(XoonglinEntity xoonglin) {
        if (xoonglin.getNeeds() == null) return Stream.empty();
        return xoonglin.getNeeds().stream()
                .map(satisfier -> satisfier.getNeed())
                .filter(SocializeNeed.class::isInstance)
                .map(SocializeNeed.class::cast);
    }
}
