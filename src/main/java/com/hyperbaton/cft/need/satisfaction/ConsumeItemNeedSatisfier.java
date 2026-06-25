package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.GoodsNeed;
import com.hyperbaton.cft.need.Need;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConsumeItemNeedSatisfier extends NeedSatisfier<GoodsNeed> {
    public ConsumeItemNeedSatisfier(double satisfaction, boolean isSatisfied, GoodsNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        for (int i = 0; i < mob.getInventory().getContainerSize(); i++) {
            ItemStack stack = mob.getInventory().getItem(i);
            if (need.getIngredient().test(stack) && stack.getCount() >= need.getQuantity()) {
                mob.getInventory().removeItem(i, need.getQuantity());
                super.satisfy(mob);
                return true;
            }
        }
        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        addMemoriesForSatisfaction(mob);
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        mob.getBrain().getMemory(suppliesNeededMemoryType()).ifPresentOrElse(
                memory -> {
                    if (memory.stream().noneMatch(ingredient -> ingredient.equals(need.getIngredient()))) {
                        List<Ingredient> mutableMemory = new ArrayList<>(memory);
                        mutableMemory.add(need.getIngredient());
                        mob.getBrain().setMemory(suppliesNeededMemoryType(), mutableMemory);
                    }
                },
                () -> mob.getBrain().setMemory(suppliesNeededMemoryType(),
                        new ArrayList<>(List.of(need.getIngredient())))
        );

        findContainerWithSupplies(mob, need.getIngredient()).ifPresent(pos ->
                mob.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER.get(), pos)
        );
    }

    private Optional<BlockPos> findContainerWithSupplies(XoonglinEntity mob, Ingredient ingredient) {
        return Optional.ofNullable(mob.getHome())
                .flatMap(home -> home.getInteriorBlocks().stream()
                        .filter(pos -> {
                            if (!(mob.level().getBlockEntity(pos) instanceof Container container)) return false;
                            for (int i = 0; i < container.getContainerSize(); i++) {
                                if (ingredient.test(container.getItem(i)) && !container.getItem(i).isEmpty()) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .findFirst());
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
