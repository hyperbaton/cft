package com.hyperbaton.cft.util;

import com.hyperbaton.cft.event.CftDatapackRegistryEvents;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Names of the game content defined in datapacks, translated like vanilla content: the lang key is
 * {@code <registry>.<namespace>.<path>} of the content's id (see {@link Util#makeDescriptionId}),
 * e.g. {@code need.cft.music_need}, {@code social_class.cft.citizen}, {@code structure.cft.smithy}
 * or {@code job.cft.baker_job}.
 */
public final class LangUtil {

    private LangUtil() {
    }

    public static MutableComponent needName(ResourceLocation needId) {
        return Component.translatable(needKey(needId));
    }

    /** The lang key of a need's description, shown in its tooltip. */
    public static String needDescriptionKey(ResourceLocation needId) {
        return needKey(needId) + ".tooltip";
    }

    public static MutableComponent socialClassName(ResourceLocation socialClassId) {
        return Component.translatable(key(CftDatapackRegistryEvents.SOCIAL_CLASS_KEY, socialClassId));
    }

    public static MutableComponent structureName(ResourceLocation structureTypeId) {
        return Component.translatable(key(CftDatapackRegistryEvents.STRUCTURE_TYPE_KEY, structureTypeId));
    }

    public static MutableComponent jobName(ResourceLocation jobId) {
        return Component.translatable(key(CftDatapackRegistryEvents.JOB_KEY, jobId));
    }

    private static String needKey(ResourceLocation needId) {
        return key(CftDatapackRegistryEvents.NEED_KEY, needId);
    }

    private static String key(ResourceKey<? extends Registry<?>> registry, ResourceLocation id) {
        return Util.makeDescriptionId(registry.location().getPath(), id);
    }
}
