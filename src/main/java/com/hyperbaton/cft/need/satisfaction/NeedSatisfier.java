package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.api.event.NeedStateChangeEvent;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.Need;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;

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

    /**
     * The id of the need, which is the name of its file. Kept here because the Xoonglin keeps its
     * own copy of the need, which isn't in the registry
     */
    ResourceLocation needId;

    public static final String TAG_SATISFACTION = "satisfaction";
    public static final String TAG_IS_SATISFIED = "isSatisfied";
    public static final String TAG_NEED = "need";
    public static final String TAG_NEED_ID = "needId";

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

    /**
     * The need isn't satisfied on this check: it wears off and the Xoonglin loses happiness.
     *
     * @return false, for {@link #satisfy} to return
     */
    protected boolean fail(XoonglinEntity mob) {
        unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need);
        return false;
    }

    /**
     * {@link #fail}, and the Xoonglin goes to satisfy it ({@link #addMemoriesForSatisfaction}).
     *
     * @return false, for {@link #satisfy} to return
     */
    protected boolean failAndSeek(XoonglinEntity mob) {
        fail(mob);
        addMemoriesForSatisfaction(mob);
        return false;
    }

    public abstract void addMemoriesForSatisfaction(XoonglinEntity mob);

    public double getSatisfaction() {
        return satisfaction;
    }

    public boolean isSatisfied() {
        return isSatisfied;
    }

    /**
     * After the need is checked: whether it's satisfied now, from its satisfaction. When that
     * changes, {@link NeedStateChangeEvent} listeners are told.
     */
    public void updateSatisfied(XoonglinEntity mob) {
        boolean satisfied = satisfaction >= need.getSatisfactionThreshold();
        if (satisfied != isSatisfied) {
            isSatisfied = satisfied;
            NeoForge.EVENT_BUS.post(new NeedStateChangeEvent(mob, this));
        }
    }

    public T getNeed() {
        return need;
    }

    public ResourceLocation getNeedId() {
        return needId;
    }

    public void setNeedId(ResourceLocation needId) {
        this.needId = needId;
    }

    public CompoundTag toTag(){
        CompoundTag tag = new CompoundTag();
        tag.putDouble(TAG_SATISFACTION, satisfaction);
        tag.putBoolean(TAG_IS_SATISFIED, isSatisfied);
        tag.putString(TAG_NEED_ID, needId.toString());
        tag.put(TAG_NEED, Need.NEED_CODEC.encodeStart(NbtOps.INSTANCE, need).result().get());
        return tag;
    }

}
