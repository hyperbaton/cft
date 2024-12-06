package com.hyperbaton.cft.world;

import com.hyperbaton.cft.structure.home.XoonglinHome;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HomesData extends SavedData {
    private static final String TAG_HOMES = "homes";

    List<XoonglinHome> homes = new ArrayList<>();

    public static HomesData load(CompoundTag compoundTag){
        HomesData data = new HomesData();
        data.homes = new ArrayList<>();
        ListTag homeTags = compoundTag.getList(TAG_HOMES, Tag.TAG_COMPOUND);
        for(Tag homeTag : homeTags){
            XoonglinHome home = XoonglinHome.fromTag((CompoundTag) homeTag);
            data.homes.add(home);
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {

        ListTag homesTags = new ListTag();
        if (!this.homes.isEmpty()) {
            for (XoonglinHome home : this.homes) {
                homesTags.add(home.toTag());
            }
        }
        compoundTag.put(TAG_HOMES, homesTags);
        return compoundTag;
    }

    public List<XoonglinHome> getHomes() {
        return homes;
    }

    public void addHome(XoonglinHome newHome) {
        this.homes.add(newHome);
        this.setDirty();
    }

    public void removeHome(XoonglinHome home) {
        this.homes.remove(home);
        this.setDirty();
    }
}
