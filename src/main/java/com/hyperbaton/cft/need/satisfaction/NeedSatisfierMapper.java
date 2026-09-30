package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.need.Need;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class NeedSatisfierMapper {
    public static NeedSatisfier<? extends Need> createNeedSatisfier(String needId, Need need) {
        NeedSatisfier<? extends Need> satisfier = need.createSatisfier();
        satisfier.setNeedId(needId);
        return satisfier;
    }

    public static NeedSatisfier<? extends Need> mapNeedSatisfier(CompoundTag needSatisfactionTag) {
        CompoundTag needTag = needSatisfactionTag.getCompound(NeedSatisfier.TAG_NEED);
        Need need = Need.NEED_CODEC.parse(NbtOps.INSTANCE, needTag).result().get();
        NeedSatisfier<? extends Need> satisfier = need.createSatisfier(needSatisfactionTag.getDouble(NeedSatisfier.TAG_SATISFACTION),
                needSatisfactionTag.getBoolean(NeedSatisfier.TAG_IS_SATISFIED));
        satisfier.setNeedId(needSatisfactionTag.getString(NeedSatisfier.TAG_NEED_ID));
        return satisfier;
    }
}
