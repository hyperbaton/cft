package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.util.JobUtil;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

public class HomeArtisanJob extends Job {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<HomeArtisanJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.INT.fieldOf("frequency_days").forGetter(j -> j.frequencyDays),
            INGREDIENT_CODEC.fieldOf("output").forGetter(j -> j.output),
            Codec.INT.fieldOf("output_count").forGetter(j -> j.outputCount)
    ).apply(inst, HomeArtisanJob::new));

    private final double hoursPerDay;
    private final int frequencyDays;
    private final Ingredient output;
    private final int outputCount;

    private static final int TICKS_PER_MC_HOUR = 1000;

    public HomeArtisanJob(double hoursPerDay, int frequencyDays, Ingredient output, int outputCount) {
        this.hoursPerDay = hoursPerDay;
        this.frequencyDays = Math.max(1, frequencyDays);
        this.output = output;
        this.outputCount = outputCount;
    }

    @Override
    public void tick(XoonglinEntity xoonglin, JobState state) {
        Level level = xoonglin.level();
        if (level.isClientSide) return;

        long dayIndex = Math.floorDiv(level.getDayTime(), 24000L);
        int neededTicks = (int) Math.round(hoursPerDay * TICKS_PER_MC_HOUR);
        // Day rollover handling
        if (state.lastDayIndex == Long.MIN_VALUE) { // First tick after spawning
            state.lastDayIndex = dayIndex;
        } else if (dayIndex != state.lastDayIndex) {
            // End-of-day check
            if (state.workedTicksToday >= neededTicks) {
                state.consecutiveDaysWorked++;
            } else {
                state.consecutiveDaysWorked = 0;
            }
            state.workedTicksToday = 0;
            state.lastDayIndex = dayIndex;

            LOGGER.trace(
                    "Day rollover for {}: workedTicks={}, needed={}, metQuota={}, streak {}, (dayIndex={})",
                    xoonglin.getName(), state.workedTicksToday, neededTicks, state.workedTicksToday >= neededTicks, state.consecutiveDaysWorked, dayIndex
            );

            if (state.consecutiveDaysWorked >= frequencyDays) {
                // Try deposit
                ItemStack toDeposit = createOutputStack();
                LOGGER.trace(
                        "Deposit trigger for {} after {} consecutive days (threshold={}): attempting to deposit {}x {}",
                        xoonglin.getName(), state.consecutiveDaysWorked, frequencyDays, toDeposit.getCount(), toDeposit.getItem()
                );
                ItemStack leftover = JobUtil.tryDepositAtHome(xoonglin, toDeposit);
                if (!leftover.isEmpty()) {
                    JobUtil.dropAtHome(xoonglin, leftover);
                }
                state.consecutiveDaysWorked = 0;
            }
        }

        // Count work tick if at home
        if (JobUtil.isAtHome(xoonglin, CftConfig.HOME_WORK_RADIUS.get())) {
            state.workedTicksToday++;
        }

        // Drive behavior via memory: if we still need to work at home today, remember it.
        // Only set this if the Xoonglin has a valid home with an entrance.
        boolean hasHome = xoonglin.getHome() != null && xoonglin.getHome().getEntrance() != null;
        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        if (hasHome && state.workedTicksToday < neededTicks) {
            brain.setMemory(CftMemoryModuleType.MUST_WORK_AT_HOME.get(), Boolean.TRUE);
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_WORK_AT_HOME.get());
        }
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.HOME_ARTISAN_JOB.get();
    }

    private ItemStack createOutputStack() {
        ItemStack[] matches = output.getItems();
        if (matches.length == 0) return ItemStack.EMPTY;
        ItemStack stack = matches[0].copy();
        stack.setCount(outputCount);
        return stack;
    }
}