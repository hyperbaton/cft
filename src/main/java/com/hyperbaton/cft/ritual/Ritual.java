package com.hyperbaton.cft.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A ritual in progress, created and driven by an officiant's PerformRitualBehavior.
 * Persisted in RitualsData so a ritual survives world reloads: the officiant's
 * behavior re-attaches to it by officiantId and resumes from the saved state.
 * Finished or cancelled rituals are simply removed from RitualsData.
 */
public class Ritual {

    public enum State {
        GATHERING,
        IN_PROGRESS
    }

    private final UUID officiantId;
    private final UUID leaderId;
    private final String ritualId;
    private final BlockPos center;
    private final int radius;

    private State state;
    /**
     * In GATHERING: ticks left before the ritual is postponed for lack of attendance.
     * In IN_PROGRESS: ticks left until the ritual completes.
     */
    private int ticksRemaining;
    /**
     * Countdown started once the minimum attendance is reached, giving latecomers
     * a short window before the ritual starts anyway. -1 means not started.
     */
    private int graceTicks = -1;
    /**
     * Xoonglins registered as attending. During IN_PROGRESS, members that leave
     * the radius are removed and no longer qualify for presence-based satisfaction.
     */
    private final Set<UUID> attendees = new HashSet<>();

    private static final String TAG_OFFICIANT = "officiantId";
    private static final String TAG_LEADER = "leaderId";
    private static final String TAG_RITUAL_ID = "ritualId";
    private static final String TAG_CENTER = "center";
    private static final String TAG_RADIUS = "radius";
    private static final String TAG_STATE = "state";
    private static final String TAG_TICKS_REMAINING = "ticksRemaining";
    private static final String TAG_GRACE_TICKS = "graceTicks";
    private static final String TAG_ATTENDEES = "attendees";

    public Ritual(UUID officiantId, UUID leaderId, String ritualId, BlockPos center, int radius,
                  int gatheringTimeout) {
        this.officiantId = officiantId;
        this.leaderId = leaderId;
        this.ritualId = ritualId;
        this.center = center;
        this.radius = radius;
        this.state = State.GATHERING;
        this.ticksRemaining = gatheringTimeout;
    }

    private Ritual(UUID officiantId, UUID leaderId, String ritualId, BlockPos center, int radius,
                   State state, int ticksRemaining, int graceTicks, Set<UUID> attendees) {
        this.officiantId = officiantId;
        this.leaderId = leaderId;
        this.ritualId = ritualId;
        this.center = center;
        this.radius = radius;
        this.state = state;
        this.ticksRemaining = ticksRemaining;
        this.graceTicks = graceTicks;
        this.attendees.addAll(attendees);
    }

    public UUID getOfficiantId() {
        return officiantId;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public String getRitualId() {
        return ritualId;
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }

    public State getState() {
        return state;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public int getGraceTicks() {
        return graceTicks;
    }

    public void setGraceTicks(int graceTicks) {
        this.graceTicks = graceTicks;
    }

    public int countDown() {
        return --ticksRemaining;
    }

    public int countDownGrace() {
        return --graceTicks;
    }

    public void begin(int duration) {
        this.state = State.IN_PROGRESS;
        this.ticksRemaining = duration;
    }

    public boolean addAttendee(UUID uuid) {
        return attendees.add(uuid);
    }

    public boolean removeAttendee(UUID uuid) {
        return attendees.remove(uuid);
    }

    public boolean isAttendee(UUID uuid) {
        return attendees.contains(uuid);
    }

    public Set<UUID> getAttendees() {
        return attendees;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TAG_OFFICIANT, officiantId);
        tag.putUUID(TAG_LEADER, leaderId);
        tag.putString(TAG_RITUAL_ID, ritualId);
        tag.put(TAG_CENTER, NbtUtils.writeBlockPos(center));
        tag.putInt(TAG_RADIUS, radius);
        tag.putString(TAG_STATE, state.name());
        tag.putInt(TAG_TICKS_REMAINING, ticksRemaining);
        tag.putInt(TAG_GRACE_TICKS, graceTicks);
        ListTag attendeeTags = new ListTag();
        for (UUID attendee : attendees) {
            attendeeTags.add(NbtUtils.createUUID(attendee));
        }
        tag.put(TAG_ATTENDEES, attendeeTags);
        return tag;
    }

    public static Ritual fromTag(CompoundTag tag) {
        Set<UUID> attendees = new HashSet<>();
        for (Tag attendeeTag : tag.getList(TAG_ATTENDEES, Tag.TAG_INT_ARRAY)) {
            attendees.add(NbtUtils.loadUUID(attendeeTag));
        }
        return new Ritual(
                tag.getUUID(TAG_OFFICIANT),
                tag.getUUID(TAG_LEADER),
                tag.getString(TAG_RITUAL_ID),
                NbtUtils.readBlockPos(tag, TAG_CENTER).orElse(BlockPos.ZERO),
                tag.getInt(TAG_RADIUS),
                State.valueOf(tag.getString(TAG_STATE)),
                tag.getInt(TAG_TICKS_REMAINING),
                tag.getInt(TAG_GRACE_TICKS),
                attendees);
    }
}
