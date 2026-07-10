package com.hyperbaton.cft.entity.ai.behavior;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.job.EnchanterJob;
import com.hyperbaton.cft.job.EnchantmentOption;
import com.hyperbaton.cft.job.Job;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Makes an enchanter work at its structure: it stands by the key block and, as long as
 * the container holds an eligible item, picks an enchantment from its repertoire that
 * the item doesn't already have (and is compatible with) and applies it in place. If no
 * eligible item/enchantment combination is found, it waits and rechecks periodically.
 */
public class EnchantBehavior extends Behavior<XoonglinEntity> {

    private static final int REPATH_INTERVAL = 40;
    // Ticks to wait before re-checking the container when there's nothing eligible to enchant
    private static final int NO_TARGET_RETRY_COOLDOWN = 600;

    private enum State {
        TRAVELING, ENCHANTING
    }

    private record Target(Container container, int slot, Holder<Enchantment> enchantment, int level) {}

    private State state;
    private BlockPos structureKeyBlock;
    private int repathTimer;
    private int waitTicks;
    private int enchantProgress;
    private Target target;

    public EnchantBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition) {
        super(pEntryCondition, 2400);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, XoonglinEntity entity) {
        EnchanterJob job = getEnchanterJob(entity);
        return job != null && entity.getAssignedStructurePos(job.getRequiredStructureType()) != null;
    }

    @Override
    protected void start(ServerLevel level, XoonglinEntity entity, long gameTime) {
        EnchanterJob job = getEnchanterJob(entity);
        structureKeyBlock = entity.getAssignedStructurePos(job.getRequiredStructureType());
        state = State.TRAVELING;
        repathTimer = 0;
        waitTicks = 0;
        enchantProgress = 0;
        target = null;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, XoonglinEntity entity, long gameTime) {
        return entity.getBrain().getMemory(CftMemoryModuleType.MUST_ENCHANT.get()).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, XoonglinEntity entity, long gameTime) {
        EnchanterJob job = getEnchanterJob(entity);
        if (job == null || structureKeyBlock == null) return;

        switch (state) {
            case TRAVELING -> tickTraveling(entity);
            case ENCHANTING -> tickEnchanting(level, entity, job);
        }
    }

    @Override
    protected void stop(ServerLevel level, XoonglinEntity entity, long gameTime) {
        entity.getNavigation().stop();
        enchantProgress = 0;
        target = null;
    }

    private void tickTraveling(XoonglinEntity entity) {
        if (entity.position().distanceTo(Vec3.atCenterOf(structureKeyBlock))
                < CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            entity.getNavigation().stop();
            state = State.ENCHANTING;
            return;
        }
        if (++repathTimer >= REPATH_INTERVAL || entity.getNavigation().isDone()) {
            repathTimer = 0;
            navigateTo(entity, structureKeyBlock);
        }
    }

    private void tickEnchanting(ServerLevel level, XoonglinEntity entity, EnchanterJob job) {
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (entity.position().distanceTo(Vec3.atCenterOf(structureKeyBlock))
                > CftConfig.CLOSE_ENOUGH_DISTANCE_TO_CONTAINER.get()) {
            state = State.TRAVELING;
            repathTimer = 0;
            return;
        }

        List<Container> containers = findStructureContainers(level, job);
        if (containers.isEmpty()) {
            waitTicks = NO_TARGET_RETRY_COOLDOWN;
            return;
        }

        if (enchantProgress == 0) {
            target = findTarget(level, entity, job, containers);
            if (target == null) {
                waitTicks = NO_TARGET_RETRY_COOLDOWN;
                return;
            }
        }

        enchantProgress++;
        if (enchantProgress % 20 == 0) {
            entity.swing(InteractionHand.MAIN_HAND);
        }

        if (enchantProgress >= job.getEnchantingTime()) {
            enchantProgress = 0;
            ItemStack stack = target.container().getItem(target.slot());
            // Item may have been taken away, or already changed, while enchanting; re-verify
            if (!stack.isEmpty() && job.getInput().test(stack) && isEligible(stack, target.enchantment())) {
                EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(target.enchantment(), target.level()));
                target.container().setChanged();
            }
            target = null;
        }
    }

    private Target findTarget(ServerLevel level, XoonglinEntity entity, EnchanterJob job, List<Container> containers) {
        Registry<Enchantment> enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for (Container container : containers) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty() || !job.getInput().test(stack)) continue;

                for (EnchantmentOption option : job.getRepertoire()) {
                    ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, option.enchantment());
                    Holder<Enchantment> holder = enchantments.getHolder(key).orElse(null);
                    if (holder == null || !isEligible(stack, holder)) continue;

                    int maxLevel = Math.min(option.maxLevel(), holder.value().getMaxLevel());
                    int minLevel = Math.min(option.minLevel(), maxLevel);
                    if (EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(holder) >= maxLevel) continue;

                    int chosenLevel = minLevel + entity.getRandom().nextInt(maxLevel - minLevel + 1);
                    return new Target(container, slot, holder, chosenLevel);
                }
            }
        }
        return null;
    }

    private boolean isEligible(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.supportsEnchantment(enchantment)
                && EnchantmentHelper.isEnchantmentCompatible(
                        EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet(), enchantment);
    }

    private List<Container> findStructureContainers(ServerLevel level, EnchanterJob job) {
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        Structure structure = data.getStructures().stream()
                .filter(s -> s.getKeyBlockPos().equals(structureKeyBlock))
                .filter(s -> s.getStructureTypeId().equals(job.getRequiredStructureType()))
                .findFirst().orElse(null);
        if (structure == null) return List.of();
        return ContainerUtil.findContainers(level, structure);
    }

    private void navigateTo(XoonglinEntity entity, BlockPos pos) {
        entity.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
    }

    private EnchanterJob getEnchanterJob(XoonglinEntity entity) {
        if (entity.getJob() == null) return null;
        Job job = CftRegistry.JOBS.get(entity.getJob());
        return job instanceof EnchanterJob e ? e : null;
    }
}
