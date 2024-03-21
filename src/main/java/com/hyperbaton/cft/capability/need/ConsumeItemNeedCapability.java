package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.CftRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class ConsumeItemNeedCapability extends NeedCapability<GoodsNeed> {
    public ConsumeItemNeedCapability(double satisfaction, boolean isSatisfied, GoodsNeed need) {
        super(satisfaction, isSatisfied, need);
    }

}
