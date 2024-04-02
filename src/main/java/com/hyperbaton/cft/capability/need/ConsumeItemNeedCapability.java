package com.hyperbaton.cft.capability.need;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class ConsumeItemNeedCapability extends NeedCapability<GoodsNeed> {
    public ConsumeItemNeedCapability(double satisfaction, boolean isSatisfied, GoodsNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    public static NeedCapability<GoodsNeed> fromTag(CompoundTag tag) {
        return new ConsumeItemNeedCapability(
                tag.getInt(TAG_SATISFACTION),
                tag.getBoolean(TAG_IS_SATISFIED),
                (GoodsNeed) Need.NEED_CODEC.parse(NbtOps.INSTANCE, tag.getCompound(TAG_NEED)).result().orElse(null)
        );
    }

}
