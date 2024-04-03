package com.hyperbaton.cft.structure.home;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class XunguiHome {


    private static final String TAG_ENTRANCE = "entrance";
    private static final String TAG_CONTAINER = "container";

    private static final String TAG_SIZE = "leader";

    private static final String TAG_LEADER = "leader";
    private static final String TAG_OWNER = "owner";

    private static final String TAG_SATISFIED_NEED = "satisfiedNeed";

    private static final String TAG_FLOOR_BLOCKS = "floorBlocks";

    private static final String TAG_WALL_BLOCKS = "wallBlocks";

    private static final String TAG_INTERIOR_BLOCKS = "interiorBlocks";

    private static final String TAG_ROOF_BLOCKS = "roofBlocks";

    // The (lower) block of the door to the house
    private BlockPos entrance;

    // The position at which there is a container (a chest) for the owner to get their items
    private BlockPos containerPos;
    private int size;

    // Id of the player that is leader of this home
    private UUID leaderId;

    // Id of the entity that is owner of the home (lives here)
    private UUID ownerId;

    // The need satisfied by this home (habitable by Xunguis of any class that has such need)
    private String satisfiedNeed;

    private List<BlockPos> floorBlocks;
    private List<BlockPos> wallBlocks;
    private List<BlockPos> interiorBlocks;
    private List<BlockPos> roofBlocks;

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

    public String getSatisfiedNeed() {
        return satisfiedNeed;
    }

    public void setSatisfiedNeed(String satisfiedNeed) {
        this.satisfiedNeed = satisfiedNeed;
    }

    public List<BlockPos> getFloorBlocks() {
        return floorBlocks;
    }

    public void setFloorBlocks(List<BlockPos> floorBlocks) {
        this.floorBlocks = floorBlocks;
    }

    public List<BlockPos> getWallBlocks() {
        return wallBlocks;
    }

    public void setWallBlocks(List<BlockPos> wallBlocks) {
        this.wallBlocks = wallBlocks;
    }

    public List<BlockPos> getInteriorBlocks() {
        return interiorBlocks;
    }

    public void setInteriorBlocks(List<BlockPos> interiorBlocks) {
        this.interiorBlocks = interiorBlocks;
    }

    public List<BlockPos> getRoofBlocks() {
        return roofBlocks;
    }

    public void setRoofBlocks(List<BlockPos> roofBlocks) {
        this.roofBlocks = roofBlocks;
    }

    public XunguiHome(BlockPos entrance, BlockPos containerPos, int size, UUID leaderId, UUID ownerId, String satisfiedNeed, List<BlockPos> floorBlocks, List<BlockPos> wallBlocks, List<BlockPos> interiorBlocks, List<BlockPos> roofBlocks) {
        this.entrance = entrance;
        this.containerPos = containerPos;
        this.size = size;
        this.leaderId = leaderId;
        this.ownerId = ownerId;
        this.satisfiedNeed = satisfiedNeed;
        this.floorBlocks = floorBlocks;
        this.wallBlocks = wallBlocks;
        this.interiorBlocks = interiorBlocks;
        this.roofBlocks = roofBlocks;
    }

    public static XunguiHome fromTag(CompoundTag homeTag) {
        BlockPos entrance = NbtUtils.readBlockPos(homeTag.getCompound(TAG_ENTRANCE));
        BlockPos container = NbtUtils.readBlockPos(homeTag.getCompound(TAG_CONTAINER));
        int size = homeTag.getInt(TAG_SIZE);
        UUID leaderId = homeTag.getUUID(TAG_LEADER);
        UUID owner = null;
        if (homeTag.contains(TAG_OWNER)) {
            owner = homeTag.getUUID(TAG_OWNER);
        }
        String homeClass = homeTag.getString(TAG_SATISFIED_NEED);
        List<BlockPos> floorBlocks = new ArrayList<>();
        for(Tag blockPosTag : homeTag.getList(TAG_FLOOR_BLOCKS, Tag.TAG_COMPOUND)){
            floorBlocks.add(NbtUtils.readBlockPos((CompoundTag) blockPosTag));
        }
        List<BlockPos> wallBlocks = new ArrayList<>();
        for(Tag blockPosTag : homeTag.getList(TAG_WALL_BLOCKS, Tag.TAG_COMPOUND)){
            wallBlocks.add(NbtUtils.readBlockPos((CompoundTag) blockPosTag));
        }
        List<BlockPos> interiorBlocks = new ArrayList<>();
        for(Tag blockPosTag : homeTag.getList(TAG_INTERIOR_BLOCKS, Tag.TAG_COMPOUND)){
            interiorBlocks.add(NbtUtils.readBlockPos((CompoundTag) blockPosTag));
        }
        List<BlockPos> roofBlocks = new ArrayList<>();
        for(Tag blockPosTag : homeTag.getList(TAG_ROOF_BLOCKS, Tag.TAG_COMPOUND)){
            roofBlocks.add(NbtUtils.readBlockPos((CompoundTag) blockPosTag));
        }
        return new XunguiHome(entrance, container, size, leaderId, owner, homeClass,
                floorBlocks, wallBlocks, interiorBlocks, roofBlocks);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(TAG_ENTRANCE, NbtUtils.writeBlockPos(entrance));
        tag.put(TAG_CONTAINER, NbtUtils.writeBlockPos(containerPos));
        tag.putInt(TAG_SIZE, size);
        tag.putUUID(TAG_LEADER, leaderId);
        if (ownerId != null) {
            tag.putUUID(TAG_OWNER, ownerId);
        }
        tag.putString(TAG_SATISFIED_NEED, satisfiedNeed);
        tag.put(TAG_FLOOR_BLOCKS, toBlockPosListTag(floorBlocks));
        tag.put(TAG_WALL_BLOCKS, toBlockPosListTag(wallBlocks));
        tag.put(TAG_INTERIOR_BLOCKS, toBlockPosListTag(interiorBlocks));
        tag.put(TAG_ROOF_BLOCKS, toBlockPosListTag(roofBlocks));
        return tag;
    }

    private ListTag toBlockPosListTag(List<BlockPos> blockList) {
        ListTag blocksTags = new ListTag();
        if (!blockList.isEmpty()) {
            for (BlockPos blockPos : blockList) {
                blocksTags.add(NbtUtils.writeBlockPos(blockPos));
            }
        }
        return blocksTags;
    }
}
