package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.CftRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class NeedCapability<T extends Need> {

    /**
     * A value between 0 and 1 about how much this need is currently satisfied
     */
    double satisfaction;

    /**
     * Quick access to know if the need is currently satisfied
     */
    boolean isSatisfied;

    T need;

    public static final String TAG_SATISFACTION = "satisfaction";
    public static final String TAG_IS_SATISFIED = "isSatisfied";
    public static final String TAG_NEED = "need";

    public NeedCapability(double satisfaction, boolean isSatisfied, T need) {
        this.satisfaction = satisfaction;
        this.isSatisfied = isSatisfied;
        this.need = need;
    }

    public void satisfy() {
        satisfaction = 1.0;
    }

    public void unsatisfy(double frequency) {
        satisfaction = Math.max(
                satisfaction -
                        BigDecimal.valueOf(20)
                                .setScale(8, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(24000 * frequency),
                                RoundingMode.HALF_UP)
                        .doubleValue(),
                0);
    }

    public double getSatisfaction() {
        return satisfaction;
    }

    public void setSatisfaction(double satisfaction) {
        this.satisfaction = satisfaction;
    }

    public boolean isSatisfied() {
        return isSatisfied;
    }

    public void setSatisfied(boolean satisfied) {
        isSatisfied = satisfied;
    }

    public T getNeed() {
        return need;
    }

    public void setNeed(T need) {
        this.need = need;
    }

    public CompoundTag toTag(){
        CompoundTag tag = new CompoundTag();
        tag.putDouble(TAG_SATISFACTION, satisfaction);
        tag.putBoolean(TAG_IS_SATISFIED, isSatisfied);
        tag.putString(TAG_NEED, need.getId());
        return tag;
    }

    public static NeedCapability<GoodsNeed> fromTag(CompoundTag tag) {
        return new ConsumeItemNeedCapability(
                tag.getInt(TAG_SATISFACTION),
                tag.getBoolean(TAG_IS_SATISFIED),
                CftRegistry.GOODS_NEEDS.get(new ResourceLocation(tag.getString(TAG_NEED)))
        );
    }
}
