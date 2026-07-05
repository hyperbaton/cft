package com.hyperbaton.cft.world;

import com.hyperbaton.cft.ritual.Ritual;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RitualsData extends SavedData {
    private static final String TAG_RITUALS = "rituals";

    List<Ritual> rituals = new ArrayList<>();

    public static RitualsData load(CompoundTag compoundTag, HolderLookup.Provider registries) {
        RitualsData data = new RitualsData();
        data.rituals = new ArrayList<>();
        ListTag ritualTags = compoundTag.getList(TAG_RITUALS, Tag.TAG_COMPOUND);
        for (Tag ritualTag : ritualTags) {
            data.rituals.add(Ritual.fromTag((CompoundTag) ritualTag));
        }
        return data;
    }

    public static SavedData.Factory<RitualsData> factory() {
        return new SavedData.Factory<>(RitualsData::new, RitualsData::load);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider registries) {
        ListTag ritualTags = new ListTag();
        for (Ritual ritual : this.rituals) {
            ritualTags.add(ritual.toTag());
        }
        compoundTag.put(TAG_RITUALS, ritualTags);
        return compoundTag;
    }

    public List<Ritual> getRituals() {
        return rituals;
    }

    public void addRitual(Ritual ritual) {
        this.rituals.add(ritual);
        this.setDirty();
    }

    public void removeRitual(Ritual ritual) {
        this.rituals.remove(ritual);
        this.setDirty();
    }

    public Optional<Ritual> findByOfficiant(UUID officiantId) {
        return rituals.stream()
                .filter(r -> r.getOfficiantId().equals(officiantId))
                .findFirst();
    }

    public Optional<Ritual> findByCenter(BlockPos center) {
        return rituals.stream()
                .filter(r -> r.getCenter().equals(center))
                .findFirst();
    }

    public Optional<Ritual> findNearby(String ritualId, UUID leaderId, BlockPos pos, int radius) {
        return rituals.stream()
                .filter(r -> r.getRitualId().equals(ritualId))
                .filter(r -> r.getLeaderId().equals(leaderId))
                .filter(r -> r.getCenter().distManhattan(pos) <= radius)
                .findFirst();
    }
}
