package com.hyperbaton.cft.entity.ai;

import com.hyperbaton.cft.CftMod;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.HashSet;
import java.util.Set;

/**
 * Errands that pause a Xoonglin's job, kept in the ERRANDS_PAUSING_WORK memory. While it has any,
 * its job doesn't tick and it goes to the INVESTIGATE activity to run them, even in working hours.
 * Each errand has its own id, so one finishing doesn't resume the job while another is pending.
 *
 * <p>Whatever needs the Xoonglin (usually a need that found a container to go to) starts an
 * errand, and the behavior doing it finishes it, whether it succeeded or gave up. Its id also
 * names its lang entry {@code errand.<namespace>.<path>}, shown in the job tab ("Paused: ...").
 */
public final class ErrandUtils {
    private ErrandUtils() {}

    /** Fetching goods or equipment from a container. */
    public static final ResourceLocation SUPPLIES = ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "supplies");
    /** Fetching a fluid from a tank. */
    public static final ResourceLocation FLUID = ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "fluid");
    /** Fetching energy from a battery. */
    public static final ResourceLocation ENERGY = ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, "energy");

    public static void start(XoonglinEntity xoonglin, ResourceLocation errand) {
        Set<ResourceLocation> errands = new HashSet<>(errands(xoonglin));
        if (errands.add(errand)) {
            xoonglin.getBrain().setMemory(CftMemoryModuleType.ERRANDS_PAUSING_WORK.get(), Set.copyOf(errands));
        }
    }

    /**
     * Starts the errand unless the Xoonglin gave up on it a moment ago and is waiting to retry, as
     * the cooldown memory says. Meanwhile it goes on working.
     */
    public static void startUnlessCoolingDown(XoonglinEntity xoonglin, ResourceLocation errand,
                                              MemoryModuleType<?> cooldown) {
        if (!xoonglin.getBrain().hasMemoryValue(cooldown)) {
            start(xoonglin, errand);
        }
    }

    public static void finish(XoonglinEntity xoonglin, ResourceLocation errand) {
        Set<ResourceLocation> errands = new HashSet<>(errands(xoonglin));
        if (!errands.remove(errand)) return;
        if (errands.isEmpty()) {
            xoonglin.getBrain().eraseMemory(CftMemoryModuleType.ERRANDS_PAUSING_WORK.get());
        } else {
            xoonglin.getBrain().setMemory(CftMemoryModuleType.ERRANDS_PAUSING_WORK.get(), Set.copyOf(errands));
        }
    }

    public static Set<ResourceLocation> errands(XoonglinEntity xoonglin) {
        return xoonglin.getBrain().getMemory(CftMemoryModuleType.ERRANDS_PAUSING_WORK.get()).orElse(Set.of());
    }

    public static boolean hasErrands(XoonglinEntity xoonglin) {
        return xoonglin.getBrain().hasMemoryValue(CftMemoryModuleType.ERRANDS_PAUSING_WORK.get());
    }
}
