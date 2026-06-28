package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.network.JobInfoData;
import com.hyperbaton.cft.util.JobUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class HaulerJob extends Job {

    public static final Codec<HaulerJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.optionalFieldOf("radius", 64).forGetter(j -> j.radius),
            HaulerErrand.CODEC.listOf().fieldOf("errands").forGetter(j -> j.errands),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds)
    ).apply(inst, HaulerJob::new));

    private final double hoursPerDay;
    private final int radius;
    private final List<HaulerErrand> errands;

    public HaulerJob(double hoursPerDay, int radius, List<HaulerErrand> errands, List<String> requiredNeeds) {
        super(requiredNeeds);
        this.hoursPerDay = hoursPerDay;
        this.radius = radius;
        this.errands = List.copyOf(errands);
    }

    public double getHoursPerDay() {
        return hoursPerDay;
    }

    public int getRadius() {
        return radius;
    }

    public List<HaulerErrand> getErrands() {
        return errands;
    }

    @Override
    public void tick(XoonglinEntity xoonglin, JobState state) {
        Level level = xoonglin.level();
        if (level.isClientSide) return;

        long dayIndex = Math.floorDiv(level.getDayTime(), 24000L);
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);

        if (state.lastDayIndex == Long.MIN_VALUE) {
            state.lastDayIndex = dayIndex;
        } else if (dayIndex != state.lastDayIndex) {
            boolean metQuota = state.workedTicksToday >= neededTicks;
            if (metQuota) {
                state.consecutiveDaysWorked++;
            } else {
                state.consecutiveDaysWorked = 0;
            }
            state.workedTicksToday = 0;
            state.lastDayIndex = dayIndex;
        }

        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        if (state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_HAUL.get(), Boolean.TRUE);
            state.workedTicksToday++;
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_HAUL.get());
        }
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        boolean doneForDay = state.workedTicksToday >= neededTicks;
        boolean canDoWork = canWork(xoonglin);

        String statusKey;
        int statusColor;
        if (!canDoWork) {
            statusKey = "gui.cft.job_status.cant_work";
            statusColor = 0xDD4040;
        } else if (doneForDay) {
            statusKey = "gui.cft.job_status.resting";
            statusColor = 0xDDAA00;
        } else {
            statusKey = "gui.cft.job_status.working";
            statusColor = 0x40AA40;
        }

        List<JobDisplayEntry> entries = new ArrayList<>();
        entries.add(JobDisplayEntry.progress("gui.cft.job_today", state.workedTicksToday, neededTicks,
                JobUtil.formatWorkTime(state.workedTicksToday, hoursPerDay)));

        SimpleContainer inventory = xoonglin.getInventory();
        for (HaulerErrand errand : errands) {
            for (HaulerErrand.HaulerItem haulerItem : errand.items()) {
                int count = countIngredient(inventory, haulerItem.ingredient());
                if (count > 0) {
                    entries.add(JobDisplayEntry.item("gui.cft.job_carrying",
                            getIngredientIcon(haulerItem.ingredient()), count));
                }
            }
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    private int countIngredient(SimpleContainer inventory, Ingredient ingredient) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private net.minecraft.resources.ResourceLocation getIngredientIcon(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return net.minecraft.resources.ResourceLocation.withDefaultNamespace("air");
        return BuiltInRegistries.ITEM.getKey(items[0].getItem());
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.HAULER_JOB.get();
    }
}
