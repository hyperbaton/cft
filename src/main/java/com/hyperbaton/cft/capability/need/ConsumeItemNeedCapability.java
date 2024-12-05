package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.entity.custom.XunguiEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ConsumeItemNeedCapability extends NeedCapability<GoodsNeed> {
    public ConsumeItemNeedCapability(double satisfaction, boolean isSatisfied, GoodsNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XunguiEntity mob) {
        if (mob.getInventory().hasAnyMatching(itemStack -> itemStack.is(need.getItem()) &&
                itemStack.getCount() >= need.getQuantity())) {
            // Consume item and satisfy the need
            mob.getInventory().removeItemType(need.getItem(), need.getQuantity());
            super.satisfy(mob);
        } else {
            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            addMemoriesForSatisfaction(mob);
            return false;
        }
        return true;
    }

    @Override
    public void addMemoriesForSatisfaction(XunguiEntity mob) {
        ItemStack neededStack = new ItemStack(need.getItem(), need.getQuantity());
        mob.getBrain().getMemory(suppliesNeededMemoryType()).ifPresentOrElse(
                memory -> {
                    // Create a mutable copy of the memory list
                    if (memory.stream().noneMatch(item -> item.is(neededStack.getItem()))) {
                        List<ItemStack> mutableMemory = new ArrayList<>(memory);
                        mutableMemory.add(new ItemStack(need.getItem(), need.getQuantity()));
                        mob.getBrain().setMemory(suppliesNeededMemoryType(), mutableMemory);
                    }
                },
                () -> mob.getBrain().setMemory(suppliesNeededMemoryType(),
                        new ArrayList<>(List.of(new ItemStack(need.getItem(), need.getQuantity()))))
        );
    }

    public static NeedCapability<GoodsNeed> fromTag(CompoundTag tag) {
        return new ConsumeItemNeedCapability(
                tag.getInt(TAG_SATISFACTION),
                tag.getBoolean(TAG_IS_SATISFIED),
                (GoodsNeed) Need.NEED_CODEC.parse(NbtOps.INSTANCE, tag.getCompound(TAG_NEED)).result().orElse(null)
        );
    }

    private MemoryModuleType<List<ItemStack>> suppliesNeededMemoryType() {
        return CftMemoryModuleType.SUPPLIES_NEEDED.get();
    }

}
