package com.hyperbaton.cft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Optional;
import java.util.*;

public class Structure {

    private static final String TAG_KEY_BLOCK_POS = "keyBlockPos";
    private static final String TAG_SIZE = "size";
    private static final String TAG_LEADER_ID = "leaderId";
    private static final String TAG_STRUCTURE_TYPE_ID = "structureTypeId";
    private static final String TAG_MAX_USERS = "maxUsers";
    private static final String TAG_USER_IDS = "userIds";
    private static final String TAG_BLOCK_GROUPS = "blockGroups";

    private final BlockPos keyBlockPos;
    private int size;
    private UUID leaderId;
    private final ResourceLocation structureTypeId;
    private final int maxUsers;
    private final List<UUID> userIds;
    private Map<String, List<BlockPos>> blockPositions;
    private Optional<BoundingBox> bounds;

    public Structure(BlockPos keyBlockPos, int size, UUID leaderId,
                     ResourceLocation structureTypeId, int maxUsers,
                     Map<String, List<BlockPos>> blockPositions) {
        this.keyBlockPos = keyBlockPos;
        this.size = size;
        this.leaderId = leaderId;
        this.structureTypeId = structureTypeId;
        this.maxUsers = maxUsers;
        this.userIds = new ArrayList<>();
        this.blockPositions = blockPositions;
    }

    protected Structure(BlockPos keyBlockPos, int size, UUID leaderId,
                        ResourceLocation structureTypeId, int maxUsers, List<UUID> userIds,
                        Map<String, List<BlockPos>> blockPositions) {
        this.keyBlockPos = keyBlockPos;
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

    /**
     * Takes the blocks of the same structure detected again, which may have changed (e.g. an added
     * room or container), keeping its users.
     */
    public void update(Structure detected) {
        this.size = detected.size;
        this.blockPositions = detected.blockPositions;
        this.bounds = null;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(TAG_KEY_BLOCK_POS, NbtUtils.writeBlockPos(keyBlockPos));
        tag.putInt(TAG_SIZE, size);
        tag.putUUID(TAG_LEADER_ID, leaderId);
        tag.putString(TAG_STRUCTURE_TYPE_ID, structureTypeId.toString());
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
        int size = tag.getInt(TAG_SIZE);
        UUID leaderId = tag.getUUID(TAG_LEADER_ID);
        ResourceLocation structureTypeId = ResourceLocation.parse(tag.getString(TAG_STRUCTURE_TYPE_ID));
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

        return new Structure(keyBlockPos, size, leaderId,
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
        for (Tag blockPosTag : tag.getList(key, Tag.TAG_INT_ARRAY)) {
            int[] arr = ((IntArrayTag) blockPosTag).getAsIntArray();
            if (arr.length == 3) {
                blocks.add(new BlockPos(arr[0], arr[1], arr[2]));
            }
        }
        return blocks;
    }

    public BlockPos getKeyBlockPos() {
        return keyBlockPos;
    }

    public int getSize() {
        return size;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public ResourceLocation getStructureTypeId() {
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

    /** Every block of the structure, from all its block groups. */
    public List<BlockPos> getAllBlockPositions() {
        return blockPositions.values().stream().flatMap(List::stream).toList();
    }

    /** The box enclosing all the blocks of this structure, or empty if it has none. */
    public Optional<BoundingBox> getBounds() {
        if (bounds == null) {
            bounds = BoundingBox.encapsulatingPositions(getAllBlockPositions());
        }
        return bounds;
    }
}
