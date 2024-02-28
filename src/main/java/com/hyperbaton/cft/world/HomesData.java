package com.hyperbaton.cft.world;

import com.hyperbaton.cft.structure.home.XunguiHome;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HomesData extends SavedData {
    private static final String TAG_HOMES = "homes";

    List<XunguiHome> homes = new ArrayList<>();

    public static HomesData load(CompoundTag compoundTag){
        HomesData data = new HomesData();
        data.homes = new ArrayList<>();
        ListTag homeTags = compoundTag.getList(TAG_HOMES, Tag.TAG_COMPOUND);
        for(Tag homeTag : homeTags){
            XunguiHome home = XunguiHome.fromTag((CompoundTag) homeTag);
            data.homes.add(home);
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {

        ListTag homesTags = new ListTag();
        if (!this.homes.isEmpty()) {
            for (XunguiHome home : this.homes) {
                homesTags.add(home.toTag());
            }
        }
        compoundTag.put(TAG_HOMES, homesTags);
        return compoundTag;
    }

    public List<XunguiHome> getHomes() {
        return homes;
    }

    public void addHome(XunguiHome newHome) {
        this.homes.add(newHome);
    }
}
