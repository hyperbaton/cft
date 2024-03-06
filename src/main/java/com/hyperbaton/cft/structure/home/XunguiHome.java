package com.hyperbaton.cft.structure.home;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.util.UUID;

public class XunguiHome {


    private static final String TAG_ENTRANCE = "entrance";
    private static final String TAG_CONTAINER = "container";

    private static final String TAG_SIZE = "leader";

    private static final String TAG_LEADER = "leader";
    private static final String TAG_OWNER = "owner";

    private static final String TAG_HOME_CLASS = "homeClass";

    // The (lower) block of the door to the house
    private BlockPos entrance;

    // The position at which there is a container (a chest) for the owner to get their items
    private BlockPos containerPos;
    private int size;

    // Id of the player that is leader of this home
    private UUID leaderId;

    // Id of the entity that is owner of the home (lives here)
    private UUID ownerId;

    // The class of this home (habitable by Xunguis of the same class)
    private String homeClass;

    public BlockPos getEntrance() {
        return entrance;
    }

    public void setEntrance(BlockPos entrance) {
        this.entrance = entrance;
    }

    public BlockPos getContainerPos() {
        return containerPos;
    }

    public void setContainerPos(BlockPos containerPos) {
        this.containerPos = containerPos;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getHomeClass() {
        return homeClass;
    }

    public void setHomeClass(String homeClass) {
        this.homeClass = homeClass;
    }

    public XunguiHome(BlockPos entrance, BlockPos containerPos, int size, UUID leaderId, UUID ownerId, String homeClass) {
        this.entrance = entrance;
        this.containerPos = containerPos;
        this.size = size;
        this.leaderId = leaderId;
        this.ownerId = ownerId;
        this.homeClass = homeClass;
    }

    public static XunguiHome fromTag(CompoundTag homeTag) {
        BlockPos entrance = NbtUtils.readBlockPos(homeTag.getCompound(TAG_ENTRANCE));
        BlockPos container = NbtUtils.readBlockPos(homeTag.getCompound(TAG_CONTAINER));
        int size = homeTag.getInt(TAG_SIZE);
        UUID leaderId = homeTag.getUUID(TAG_LEADER);
        UUID owner = homeTag.getUUID(TAG_OWNER);
        String homeClass = homeTag.getString(TAG_HOME_CLASS);
        return new XunguiHome(entrance, container, size, leaderId, owner, homeClass);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(TAG_ENTRANCE, NbtUtils.writeBlockPos(entrance));
        tag.put(TAG_CONTAINER, NbtUtils.writeBlockPos(containerPos));
        tag.putInt(TAG_SIZE, size);
        tag.putUUID(TAG_LEADER, leaderId);
        tag.putUUID(TAG_OWNER, ownerId);
        tag.putString(TAG_HOME_CLASS, homeClass);
        return tag;
    }
}
