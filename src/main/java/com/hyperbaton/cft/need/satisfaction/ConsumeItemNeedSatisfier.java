package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.GoodsNeed;
import com.hyperbaton.cft.need.Need;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class ConsumeItemNeedSatisfier extends NeedSatisfier<GoodsNeed> {
    public ConsumeItemNeedSatisfier(double satisfaction, boolean isSatisfied, GoodsNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        // Consume matching item and satisfy the need
        for (int i = 0; i < mob.getInventory().getContainerSize(); i++) {
            ItemStack stack = mob.getInventory().getItem(i);
            if (need.getIngredient().test(stack) && stack.getCount() >= need.getQuantity()) {
                mob.getInventory().removeItem(i, need.getQuantity());
                super.satisfy(mob);
                return true;
            }
        }
        // If it didn't stop after finding something, unsatisfy the need
        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        addMemoriesForSatisfaction(mob);
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        mob.getBrain().getMemory(suppliesNeededMemoryType()).ifPresentOrElse(
                memory -> {
                    // Ensure the Ingredient isn't already in memory before adding
                    if (memory.stream().noneMatch(ingredient -> ingredient.equals(need.getIngredient()))) {
                        List<Ingredient> mutableMemory = new ArrayList<>(memory);
                        mutableMemory.add(need.getIngredient());
                        mob.getBrain().setMemory(suppliesNeededMemoryType(), mutableMemory);
                    }
                },
                () -> mob.getBrain().setMemory(suppliesNeededMemoryType(),
                        new ArrayList<>(List.of(need.getIngredient())))
        );
    }

    public static NeedSatisfier<GoodsNeed> fromTag(CompoundTag tag) {
        return new ConsumeItemNeedSatisfier(
                tag.getInt(TAG_SATISFACTION),
                tag.getBoolean(TAG_IS_SATISFIED),
                (GoodsNeed) Need.NEED_CODEC.parse(NbtOps.INSTANCE, tag.getCompound(TAG_NEED)).result().orElse(null)
        );
    }

    private MemoryModuleType<List<Ingredient>> suppliesNeededMemoryType() {
        return CftMemoryModuleType.SUPPLIES_NEEDED.get();
    }

}
