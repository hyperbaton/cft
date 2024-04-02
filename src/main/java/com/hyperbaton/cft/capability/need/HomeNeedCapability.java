package com.hyperbaton.cft.capability.need;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class HomeNeedCapability extends NeedCapability<HomeNeed> {
    public HomeNeedCapability(double satisfaction, boolean isSatisfied, HomeNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    public static NeedCapability<HomeNeed> fromTag(CompoundTag tag) {
        return new HomeNeedCapability(
                tag.getInt(TAG_SATISFACTION),
                tag.getBoolean(TAG_IS_SATISFIED),
                (HomeNeed) Need.NEED_CODEC.parse(NbtOps.INSTANCE, tag.getCompound(TAG_NEED)).result().orElse(null)
        );
    }

}
