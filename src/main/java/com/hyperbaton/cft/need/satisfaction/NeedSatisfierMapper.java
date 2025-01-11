package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.need.Need;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class NeedSatisfierMapper {
    public static NeedSatisfier<? extends Need> mapNeedSatisfier(CompoundTag needSatisfactionTag) {
        CompoundTag needTag = needSatisfactionTag.getCompound(NeedSatisfier.TAG_NEED);
        Need need = Need.NEED_CODEC.parse(NbtOps.INSTANCE, needTag).result().get();
        return need.createSatisfier(needSatisfactionTag.getDouble(NeedSatisfier.TAG_SATISFACTION),
                needSatisfactionTag.getBoolean(NeedSatisfier.TAG_IS_SATISFIED));
    }
}
