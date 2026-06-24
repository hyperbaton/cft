package com.hyperbaton.cft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.*;

public class Structure {

    private static final String TAG_KEY_BLOCK_POS = "keyBlockPos";
    private static final String TAG_CONTAINER_POS = "containerPos";
    private static final String TAG_SIZE = "size";
    private static final String TAG_LEADER_ID = "leaderId";
    private static final String TAG_STRUCTURE_TYPE_ID = "structureTypeId";
    private static final String TAG_MAX_USERS = "maxUsers";
    private static final String TAG_USER_IDS = "userIds";
    private static final String TAG_BLOCK_GROUPS = "blockGroups";

    private final BlockPos keyBlockPos;
    private BlockPos containerPos;
    private int size;
    private UUID leaderId;
    private final String structureTypeId;
    private final int maxUsers;
    private final List<UUID> userIds;
    private final Map<String, List<BlockPos>> blockPositions;

    public Structure(BlockPos keyBlockPos, BlockPos containerPos, int size, UUID leaderId,
                     String structureTypeId, int maxUsers,
                     Map<String, List<BlockPos>> blockPositions) {
        this.keyBlockPos = keyBlockPos;
        this.containerPos = containerPos;
        this.size = size;
        this.leaderId = leaderId;
        this.structureTypeId = structureTypeId;
        this.maxUsers = maxUsers;
        this.userIds = new ArrayList<>();
        this.blockPositions = blockPositions;
    }

    protected Structure(BlockPos keyBlockPos, BlockPos containerPos, int size, UUID leaderId,
                        String structureTypeId, int maxUsers, List<UUID> userIds,
                        Map<String, List<BlockPos>> blockPositions) {
        this.keyBlockPos = keyBlockPos;
        this.containerPos = containerPos;
        this.size = size;
        this.leaderId = leaderId;
        this.structureTypeId = structureTypeId;
        this.maxUsers = maxUsers;
        this.userIds = new ArrayList<>(userIds);
        this.blockPositions = blockPositions;
    }

    public boolean hasCapacity() {
        return userIds.size() < maxUsers;
    }

    public boolean addUser(UUID userId) {
        if (!hasCapacity() || userIds.contains(userId)) return false;
        userIds.add(userId);
        return true;
    }

    public boolean removeUser(UUID userId) {
        return userIds.remove(userId);
    }

    public boolean isUser(UUID userId) {
        return userIds.contains(userId);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(TAG_KEY_BLOCK_POS, NbtUtils.writeBlockPos(keyBlockPos));
        if (containerPos != null) {
            tag.put(TAG_CONTAINER_POS, NbtUtils.writeBlockPos(containerPos));
        }
        tag.putInt(TAG_SIZE, size);
        tag.putUUID(TAG_LEADER_ID, leaderId);
        tag.putString(TAG_STRUCTURE_TYPE_ID, structureTypeId);
        tag.putInt(TAG_MAX_USERS, maxUsers);

        ListTag userIdTags = new ListTag();
        for (UUID userId : userIds) {
            CompoundTag userTag = new CompoundTag();
            userTag.putUUID("id", userId);
            userIdTags.add(userTag);
        }
        tag.put(TAG_USER_IDS, userIdTags);

        CompoundTag groupsTag = new CompoundTag();
        for (Map.Entry<String, List<BlockPos>> entry : blockPositions.entrySet()) {
            groupsTag.put(entry.getKey(), writeBlockPosList(entry.getValue()));
        }
        tag.put(TAG_BLOCK_GROUPS, groupsTag);

        return tag;
    }

    public static Structure fromTag(CompoundTag tag) {
        BlockPos keyBlockPos = NbtUtils.readBlockPos(tag, TAG_KEY_BLOCK_POS).orElse(BlockPos.ZERO);
        BlockPos containerPos = tag.contains(TAG_CONTAINER_POS)
                ? NbtUtils.readBlockPos(tag, TAG_CONTAINER_POS).orElse(null)
                : null;
        int size = tag.getInt(TAG_SIZE);
        UUID leaderId = tag.getUUID(TAG_LEADER_ID);
        String structureTypeId = tag.getString(TAG_STRUCTURE_TYPE_ID);
        int maxUsers = tag.getInt(TAG_MAX_USERS);

        List<UUID> userIds = new ArrayList<>();
        ListTag userIdTags = tag.getList(TAG_USER_IDS, Tag.TAG_COMPOUND);
        for (Tag userTag : userIdTags) {
            userIds.add(((CompoundTag) userTag).getUUID("id"));
        }

        Map<String, List<BlockPos>> blockPositions = new HashMap<>();
        if (tag.contains(TAG_BLOCK_GROUPS)) {
            CompoundTag groupsTag = tag.getCompound(TAG_BLOCK_GROUPS);
            for (String key : groupsTag.getAllKeys()) {
                blockPositions.put(key, readBlockPosList(groupsTag, key));
            }
        }

        return new Structure(keyBlockPos, containerPos, size, leaderId,
                structureTypeId, maxUsers, userIds, blockPositions);
    }

    private static ListTag writeBlockPosList(List<BlockPos> blocks) {
        ListTag listTag = new ListTag();
        for (BlockPos pos : blocks) {
            listTag.add(NbtUtils.writeBlockPos(pos));
        }
        return listTag;
    }

    private static List<BlockPos> readBlockPosList(CompoundTag tag, String key) {
        List<BlockPos> blocks = new ArrayList<>();
        for (Tag blockPosTag : tag.getList(key, Tag.TAG_COMPOUND)) {
            CompoundTag posTag = (CompoundTag) blockPosTag;
            blocks.add(new BlockPos(posTag.getInt("X"), posTag.getInt("Y"), posTag.getInt("Z")));
        }
        return blocks;
    }

    public BlockPos getKeyBlockPos() {
        return keyBlockPos;
    }

    public BlockPos getContainerPos() {
        return containerPos;
    }

    public int getSize() {
        return size;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public String getStructureTypeId() {
        return structureTypeId;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public List<UUID> getUserIds() {
        return Collections.unmodifiableList(userIds);
    }

    public Map<String, List<BlockPos>> getBlockPositions() {
        return blockPositions;
    }
}
