package com.hyperbaton.cft.capability.need;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class NeedCapabilityMapper {
    public static NeedCapability mapNeedCapability(CompoundTag needCapabilityTag) {
        CompoundTag needTag = needCapabilityTag.getCompound(NeedCapability.TAG_NEED);
        Need need = Need.NEED_CODEC.parse(NbtOps.INSTANCE, needTag).result().get();
        if (need instanceof GoodsNeed) {
            return new ConsumeItemNeedCapability(
                    needCapabilityTag.getDouble(NeedCapability.TAG_SATISFACTION),
                    needCapabilityTag.getBoolean(NeedCapability.TAG_IS_SATISFIED),
                    (GoodsNeed) need
            );
        } else if (need instanceof HomeNeed) {
            return new HomeNeedCapability(
                    needCapabilityTag.getDouble(NeedCapability.TAG_SATISFACTION),
                    needCapabilityTag.getBoolean(NeedCapability.TAG_IS_SATISFIED),
                    (HomeNeed) need
            );
        }
        return null;
    }
}
