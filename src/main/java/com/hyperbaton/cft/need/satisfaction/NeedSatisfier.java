package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.Need;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class NeedSatisfier<T extends Need> {

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

    public NeedSatisfier(double satisfaction, boolean isSatisfied, T need) {
        this.satisfaction = satisfaction;
        this.isSatisfied = isSatisfied;
        this.need = need;
    }

    public boolean satisfy(XoonglinEntity mob) {
        satisfaction = 1.0;
        mob.increaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        return true;
    }

    public void unsatisfy(double frequency, XoonglinEntity mob) {
        satisfaction = Math.max(
                satisfaction -
                        BigDecimal.valueOf(20)
                                .setScale(8, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(24000 * frequency),
                                RoundingMode.HALF_UP)
                        .doubleValue(),
                0);
        if(need.getDamage() != 0.0 && satisfaction < need.getDamageThreshold()) {
            mob.hurt(mob.level().damageSources().generic(), (float) need.getDamage());
        }
    }

    public abstract void addMemoriesForSatisfaction(XoonglinEntity mob);

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
        tag.put(TAG_NEED, Need.NEED_CODEC.encodeStart(NbtOps.INSTANCE, need).result().get());
        return tag;
    }

}
