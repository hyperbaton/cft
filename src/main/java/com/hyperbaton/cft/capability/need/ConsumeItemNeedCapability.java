package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.memory.CftMemoryModuleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

public class ConsumeItemNeedCapability extends NeedCapability<GoodsNeed> {
    public ConsumeItemNeedCapability(double satisfaction, boolean isSatisfied, GoodsNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XunguiEntity mob) {
        if (mob.getInventory().hasAnyMatching(itemStack -> itemStack.is(need.getItem()) && itemStack.getCount() >= need.getQuantity())) {
            // Consume item and satisfy the need
            mob.getInventory().removeItemType(need.getItem(), need.getQuantity());
            super.satisfy(mob);
        } else {
            this.unsatisfy(need.getFrequency());
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            addMemoriesForSatisfaction(mob);
            return false;
        }
        return true;
    }

    @Override
    public void addMemoriesForSatisfaction(XunguiEntity mob) {
        if(!mob.getBrain().hasMemoryValue(CftMemoryModuleType.SUPPLIES_NEEDED.get())){
            mob.getBrain().setMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get(), new ItemStack(need.getItem(), need.getQuantity()));
        }
    }

    public static NeedCapability<GoodsNeed> fromTag(CompoundTag tag) {
        return new ConsumeItemNeedCapability(
                tag.getInt(TAG_SATISFACTION),
                tag.getBoolean(TAG_IS_SATISFIED),
                (GoodsNeed) Need.NEED_CODEC.parse(NbtOps.INSTANCE, tag.getCompound(TAG_NEED)).result().orElse(null)
        );
    }

}
