package com.hyperbaton.cft.job;

import com.hyperbaton.cft.CftConfig;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.network.JobDisplayEntry;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.network.JobInfoData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

public class FarmerJob extends Job {

    public static final Codec<FarmerJob> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("hours_per_day").forGetter(j -> j.hoursPerDay),
            Codec.STRING.fieldOf("structure_type").forGetter(j -> j.structureType),
            INGREDIENT_CODEC.fieldOf("seed").forGetter(j -> j.seed),
            INGREDIENT_CODEC.fieldOf("product").forGetter(j -> j.product),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("crop_block").forGetter(j -> j.cropBlock),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("ripe_state").forGetter(j -> j.ripeState),
            Codec.STRING.listOf().optionalFieldOf("required_needs", List.of()).forGetter(Job::getRequiredNeeds)
    ).apply(inst, FarmerJob::new));

    private final double hoursPerDay;
    private final String structureType;
    private final Ingredient seed;
    private final Ingredient product;
    private final Block cropBlock;
    private final Map<String, String> ripeState;

    public FarmerJob(double hoursPerDay, String structureType, Ingredient seed, Ingredient product,
                     Block cropBlock, Map<String, String> ripeState, List<String> requiredNeeds) {
        super(requiredNeeds);
        this.hoursPerDay = hoursPerDay;
        this.structureType = structureType;
        this.seed = seed;
        this.product = product;
        this.cropBlock = cropBlock;
        this.ripeState = ripeState;
    }

    public String getStructureType() {
        return structureType;
    }

    public Ingredient getSeed() {
        return seed;
    }

    public Block getCropBlock() {
        return cropBlock;
    }

    public boolean isRipeCrop(BlockState state) {
        if (!state.is(cropBlock)) return false;
        for (Map.Entry<String, String> entry : ripeState.entrySet()) {
            Property<?> prop = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (prop == null) return false;
            if (!state.getValue(prop).toString().equals(entry.getValue())) return false;
        }
        return true;
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

        BlockPos structurePos = xoonglin.getAssignedStructurePos(structureType);

        if (structurePos != null && isAtStructure(xoonglin, structurePos) && canWork(xoonglin)) {
            state.workedTicksToday++;
        }

        Brain<XoonglinEntity> brain = xoonglin.getBrain();

        if (structurePos == null) {
            brain.setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), structureType);
            brain.eraseMemory(CftMemoryModuleType.MUST_FARM.get());
        } else if (state.workedTicksToday < neededTicks && canWork(xoonglin)) {
            brain.setMemory(CftMemoryModuleType.MUST_FARM.get(), Boolean.TRUE);
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        } else {
            brain.eraseMemory(CftMemoryModuleType.MUST_FARM.get());
            brain.eraseMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get());
        }
    }

    private boolean isAtStructure(XoonglinEntity xoonglin, BlockPos structurePos) {
        return structurePos.closerToCenterThan(xoonglin.position(), CftConfig.HOME_WORK_RADIUS.get());
    }

    @Override
    public JobInfoData getDisplayInfo(XoonglinEntity xoonglin, JobState state) {
        int neededTicks = (int) Math.round(hoursPerDay * JobUtil.TICKS_PER_MC_HOUR);
        BlockPos structurePos = xoonglin.getAssignedStructurePos(structureType);
        boolean atStructure = structurePos != null && isAtStructure(xoonglin, structurePos);
        boolean canDoWork = canWork(xoonglin);
        boolean doneForDay = state.workedTicksToday >= neededTicks;

        String statusKey;
        int statusColor;
        if (structurePos == null) {
            statusKey = "gui.cft.job_status.no_structure";
            statusColor = 0xDD4040;
        } else if (!canDoWork) {
            statusKey = "gui.cft.job_status.cant_work";
            statusColor = 0xDD4040;
        } else if (doneForDay) {
            statusKey = "gui.cft.job_status.resting";
            statusColor = 0xDDAA00;
        } else if (atStructure) {
            statusKey = "gui.cft.job_status.working";
            statusColor = 0x40AA40;
        } else {
            statusKey = "gui.cft.job_status.traveling";
            statusColor = 0x4080DD;
        }

        List<JobDisplayEntry> entries = new ArrayList<>();
        entries.add(JobDisplayEntry.progress("gui.cft.job_today", state.workedTicksToday, neededTicks,
                JobUtil.formatWorkTime(state.workedTicksToday, hoursPerDay)));
        entries.add(JobDisplayEntry.progress("gui.cft.job_streak", state.consecutiveDaysWorked, 1));

        boolean seedAndProductSame = isSameItem(seed, product);
        if (seedAndProductSame) {
            int count = countIngredient(xoonglin, seed);
            entries.add(JobDisplayEntry.item("gui.cft.job_inventory",
                    getIngredientIcon(seed), count));
        } else {
            int seedCount = countIngredient(xoonglin, seed);
            entries.add(JobDisplayEntry.item("gui.cft.job_seeds",
                    getIngredientIcon(seed), seedCount));
            int productCount = countIngredient(xoonglin, product);
            entries.add(JobDisplayEntry.item("gui.cft.job_product",
                    getIngredientIcon(product), productCount));
        }

        return new JobInfoData(statusKey, statusColor, entries);
    }

    private int countIngredient(XoonglinEntity xoonglin, Ingredient ingredient) {
        int count = 0;
        for (int i = 0; i < xoonglin.getInventory().getContainerSize(); i++) {
            ItemStack stack = xoonglin.getInventory().getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean isSameItem(Ingredient a, Ingredient b) {
        ItemStack[] aItems = a.getItems();
        ItemStack[] bItems = b.getItems();
        if (aItems.length == 0 || bItems.length == 0) return false;
        return ItemStack.isSameItem(aItems[0], bItems[0]);
    }

    private ResourceLocation getIngredientIcon(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return ResourceLocation.withDefaultNamespace("air");
        return BuiltInRegistries.ITEM.getKey(items[0].getItem());
    }

    @Override
    public Codec<? extends Job> jobType() {
        return CftRegistry.FARMER_JOB.get();
    }
}
