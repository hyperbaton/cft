package com.hyperbaton.cft.capability.need;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class NeedCapabilityMapper {
    public static NeedCapability<? extends Need> mapNeedCapability(CompoundTag needCapabilityTag) {
        CompoundTag needTag = needCapabilityTag.getCompound(NeedCapability.TAG_NEED);
        Need need = Need.NEED_CODEC.parse(NbtOps.INSTANCE, needTag).result().get();
        return need.createCapability(needCapabilityTag.getDouble(NeedCapability.TAG_SATISFACTION),
                needCapabilityTag.getBoolean(NeedCapability.TAG_IS_SATISFIED));
    }
}
