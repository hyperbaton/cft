package com.hyperbaton.cft.world;

import com.hyperbaton.cft.structure.Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StructuresData extends SavedData {
    private static final String TAG_STRUCTURES = "structures";

    List<Structure> structures = new ArrayList<>();

    public static StructuresData load(CompoundTag compoundTag, HolderLookup.Provider registries) {
        StructuresData data = new StructuresData();
        data.structures = new ArrayList<>();
        ListTag structureTags = compoundTag.getList(TAG_STRUCTURES, Tag.TAG_COMPOUND);
        for (Tag structureTag : structureTags) {
            Structure structure = Structure.fromTag((CompoundTag) structureTag);
            data.structures.add(structure);
        }
        return data;
    }

    public static SavedData.Factory<StructuresData> factory() {
        return new SavedData.Factory<>(StructuresData::new, StructuresData::load);
    }

    /** The structures registered in a level. */
    public static StructuresData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), "structuresData");
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider registries) {
        ListTag structuresTags = new ListTag();
        if (!this.structures.isEmpty()) {
            for (Structure structure : this.structures) {
                structuresTags.add(structure.toTag());
            }
        }
        compoundTag.put(TAG_STRUCTURES, structuresTags);
        return compoundTag;
    }

    public List<Structure> getStructures() {
        return structures;
    }

    /** The registered structure whose key block is at the given position, if any. */
    public Optional<Structure> findByKeyBlock(BlockPos keyBlockPos) {
        return structures.stream()
                .filter(structure -> structure.getKeyBlockPos().equals(keyBlockPos))
                .findFirst();
    }

    public void addStructure(Structure structure) {
        this.structures.add(structure);
        this.setDirty();
    }

    public void removeStructure(Structure structure) {
        this.structures.remove(structure);
        this.setDirty();
    }
}
