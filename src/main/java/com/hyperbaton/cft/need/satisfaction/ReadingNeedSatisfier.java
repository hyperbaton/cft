package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.CftDataComponents;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.item.ManuscriptData;
import com.hyperbaton.cft.need.ReadingNeed;
import com.hyperbaton.cft.world.BookEntry;
import com.hyperbaton.cft.world.RostersData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Works exactly like GoodsNeed/ConsumeItemNeedSatisfier: the Xoonglin fetches a matching
 * book from its home container into its own inventory (via SUPPLIES_NEEDED/HOME_CONTAINER
 * and GetSuppliesBehavior), then consumes it from there to satisfy the need. Which book
 * is "wanted" is picked deterministically from the world roster (stable for a given day,
 * shifting over time) rather than assigned once and remembered, so it's always computed
 * fresh against whatever the roster currently contains.
 *
 * Matching is by roster id + leader id only (via the MANUSCRIPT data component) — not by
 * title, which is already carried reliably by vanilla's own WrittenBookContent and would
 * only make matching more brittle to duplicate here. A random book a player writes from
 * scratch has no MANUSCRIPT component at all, so it can never satisfy this need.
 */
public class ReadingNeedSatisfier extends NeedSatisfier<ReadingNeed> {

    public ReadingNeedSatisfier(double satisfaction, boolean isSatisfied, ReadingNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        Ingredient wanted = wantedIngredient(mob);
        if (wanted != null) {
            for (int i = 0; i < mob.getInventory().getContainerSize(); i++) {
                ItemStack stack = mob.getInventory().getItem(i);
                if (!stack.isEmpty() && wanted.test(stack)) {
                    mob.getInventory().removeItem(i, 1);
                    super.satisfy(mob);
                    return true;
                }
            }
        }

        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
        addMemoriesForSatisfaction(mob);
        return false;
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        Ingredient wanted = wantedIngredient(mob);
        if (wanted == null) return;

        mob.getBrain().getMemory(suppliesNeededMemoryType()).ifPresentOrElse(
                memory -> {
                    List<Ingredient> mutableMemory = new ArrayList<>(memory);
                    mutableMemory.add(wanted);
                    mob.getBrain().setMemory(suppliesNeededMemoryType(), mutableMemory);
                },
                () -> mob.getBrain().setMemory(suppliesNeededMemoryType(), new ArrayList<>(List.of(wanted)))
        );

        findContainerWithBook(mob, wanted).ifPresent(pos ->
                mob.getBrain().setMemory(CftMemoryModuleType.HOME_CONTAINER.get(), pos)
        );
    }

    /** Whichever roster entry is currently "wanted" by this Xoonglin, or empty if none. */
    public Optional<BookEntry> wantedBookEntry(XoonglinEntity mob) {
        UUID leaderId = mob.getLeaderId();
        if (leaderId == null) return Optional.empty();

        RostersData rosters = ((ServerLevel) mob.level()).getDataStorage()
                .computeIfAbsent(RostersData.factory(), "rostersData");
        List<BookEntry> entries = rosters.findByLeader(leaderId);
        if (entries.isEmpty()) return Optional.empty();

        long dayIndex = Math.floorDiv(mob.level().getDayTime(), 24000L);
        int index = Math.floorMod(mob.getUUID().hashCode() + (int) dayIndex, entries.size());
        return Optional.of(entries.get(index));
    }

    /** The Ingredient matching whichever roster entry is currently "wanted", or null if none. */
    private Ingredient wantedIngredient(XoonglinEntity mob) {
        UUID leaderId = mob.getLeaderId();
        if (leaderId == null) return null;

        return wantedBookEntry(mob)
                .map(entry -> DataComponentIngredient.of(false, CftDataComponents.MANUSCRIPT.get(),
                        new ManuscriptData(entry.id(), leaderId), Items.WRITTEN_BOOK))
                .orElse(null);
    }

    private Optional<BlockPos> findContainerWithBook(XoonglinEntity mob, Ingredient ingredient) {
        if (mob.getHome() == null) return Optional.empty();
        return mob.getHome().getInteriorBlocks().stream()
                .filter(pos -> {
                    if (!(mob.level().getBlockEntity(pos) instanceof Container container)) return false;
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        if (!container.getItem(i).isEmpty() && ingredient.test(container.getItem(i))) {
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst();
    }

    private MemoryModuleType<List<Ingredient>> suppliesNeededMemoryType() {
        return CftMemoryModuleType.SUPPLIES_NEEDED.get();
    }
}
