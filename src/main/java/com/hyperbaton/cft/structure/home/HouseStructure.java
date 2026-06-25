package com.hyperbaton.cft.structure.home;

import com.hyperbaton.cft.structure.EnclosedBuildingBlockGroup;
import com.hyperbaton.cft.structure.Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class HouseStructure extends Structure {

    private HouseStructure(BlockPos keyBlockPos, int size, UUID leaderId,
                           String structureTypeId, int maxUsers, List<UUID> userIds,
                           Map<String, List<BlockPos>> blockPositions) {
        super(keyBlockPos, size, leaderId, structureTypeId, maxUsers, userIds, blockPositions);
    }

    public static HouseStructure of(Structure structure) {
        return new HouseStructure(
                structure.getKeyBlockPos(), structure.getSize(),
                structure.getLeaderId(), structure.getStructureTypeId(), structure.getMaxUsers(),
                structure.getUserIds(), structure.getBlockPositions()
        );
    }

    public BlockPos getEntrance() {
        return getKeyBlockPos();
    }

    public List<BlockPos> getFloorBlocks() {
        return getBlockPositions().getOrDefault(EnclosedBuildingBlockGroup.FLOOR.getKey(), Collections.emptyList());
    }

    public List<BlockPos> getWallBlocks() {
        return getBlockPositions().getOrDefault(EnclosedBuildingBlockGroup.WALL.getKey(), Collections.emptyList());
    }

    public List<BlockPos> getInteriorBlocks() {
        return getBlockPositions().getOrDefault(EnclosedBuildingBlockGroup.INTERIOR.getKey(), Collections.emptyList());
    }

    public List<BlockPos> getRoofBlocks() {
        return getBlockPositions().getOrDefault(EnclosedBuildingBlockGroup.ROOF.getKey(), Collections.emptyList());
    }

    public static HouseStructure fromTag(CompoundTag tag) {
        Structure structure = Structure.fromTag(tag);
        return HouseStructure.of(structure);
    }

}
