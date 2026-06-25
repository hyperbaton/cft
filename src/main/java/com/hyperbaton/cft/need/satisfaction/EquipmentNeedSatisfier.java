package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.EquipmentNeed;
import com.hyperbaton.cft.need.Need;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EquipmentNeedSatisfier extends NeedSatisfier<EquipmentNeed> {

    public EquipmentNeedSatisfier(double satisfaction, boolean isSatisfied, EquipmentNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        EquipmentSlot slot = need.getSlot();
        Ingredient ingredient = need.getIngredient();
        ItemStack equipped = mob.getItemBySlot(slot);

        if (ingredient.test(equipped) && !equipped.isEmpty()) {
            super.satisfy(mob);
            return true;
        }

        for (int i = 0; i < mob.getInventory().getContainerSize(); i++) {
            ItemStack stack = mob.getInventory().getItem(i);
            if (ingredient.test(stack) && !stack.isEmpty()) {
                ItemStack taken = mob.getInventory().removeItem(i, 1);
                if (!equipped.isEmpty()) {
                    mob.getInventory().addItem(equipped);
                }
                mob.setItemSlot(slot, taken);
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
    public void unsatisfy(double frequency, XoonglinEntity mob) {
        super.unsatisfy(frequency, mob);

        EquipmentSlot slot = need.getSlot();
        ItemStack equipped = mob.getItemBySlot(slot);
        if (!need.getIngredient().test(equipped) || equipped.isEmpty()) return;

        damageEquipment(equipped, frequency, mob, slot);

        if (satisfaction < need.getSatisfactionThreshold()) {
            mob.setItemSlot(slot, ItemStack.EMPTY);
            if (!equipped.isEmpty()) {
                mob.getInventory().addItem(equipped);
            }
        }
    }

    private void damageEquipment(ItemStack equipped, double frequency, XoonglinEntity mob, EquipmentSlot slot) {
        int maxDamage = equipped.getMaxDamage();
        if (maxDamage <= 0) return;
        int damagePerCheck = Math.max(1, (int) Math.round((double) maxDamage * 20.0 / (24000.0 * frequency)));
        equipped.hurtAndBreak(damagePerCheck, mob, slot);
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        Ingredient ingredient = need.getIngredient();
        mob.getBrain().getMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get()).ifPresentOrElse(
                memory -> {
                    if (memory.stream().noneMatch(ing -> ing.equals(ingredient))) {
                        List<Ingredient> mutableMemory = new ArrayList<>(memory);
                        mutableMemory.add(ingredient);
                        mob.getBrain().setMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get(), mutableMemory);
                    }
                },
                () -> mob.getBrain().setMemory(CftMemoryModuleType.SUPPLIES_NEEDED.get(),
                        new ArrayList<>(List.of(ingredient)))
        );

        findContainerWithEquipment(mob, ingredient).ifPresent(pos ->
                mob.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER.get(), pos)
        );
    }

    private Optional<BlockPos> findContainerWithEquipment(XoonglinEntity mob, Ingredient ingredient) {
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

    public static NeedSatisfier<EquipmentNeed> fromTag(CompoundTag tag) {
        return new EquipmentNeedSatisfier(
                tag.getInt(TAG_SATISFACTION),
                tag.getBoolean(TAG_IS_SATISFIED),
                (EquipmentNeed) Need.NEED_CODEC.parse(NbtOps.INSTANCE, tag.getCompound(TAG_NEED)).result().orElse(null)
        );
    }
}
