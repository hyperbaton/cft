package com.hyperbaton.cft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Arrays;
import java.util.List;

/**
 * Entries of a registry, given the way vanilla data gives them: an id ({@code "minecraft:furnace"}),
 * a tag with a leading '#' ({@code "#minecraft:logs"}), or a list mixing both.
 * <p>
 * Unlike vanilla's holder set codecs, it's read without registry access, so needs holding one can
 * still be saved with the copy of the need each Xoonglin keeps. Ids of registries built into the
 * game (blocks, entity types, sounds...) are checked when read, so a typo fails loudly; ids of
 * datapack registries, like biomes, can't be checked then.
 */
public record RegistryEntries<T>(List<Either<TagKey<T>, ResourceLocation>> entries) {

    public static <T> Codec<RegistryEntries<T>> codec(ResourceKey<? extends Registry<T>> registry) {
        Codec<Either<TagKey<T>, ResourceLocation>> entry = Codec.either(TagKey.hashedCodec(registry), idCodec(registry));
        return CodecUtil.singleOrList(entry).xmap(RegistryEntries::new, RegistryEntries::entries);
    }

    private static Codec<ResourceLocation> idCodec(ResourceKey<? extends Registry<?>> registry) {
        return ResourceLocation.CODEC.validate(id -> {
            Registry<?> builtIn = BuiltInRegistries.REGISTRY.get(registry.location());
            return builtIn == null || builtIn.containsKey(id)
                    ? DataResult.success(id)
                    : DataResult.error(() -> "Unknown " + registry.location().getPath() + ": " + id);
        });
    }

    public static <T> RegistryEntries<T> empty() {
        return new RegistryEntries<>(List.of());
    }

    public static <T> RegistryEntries<T> of(ResourceLocation... ids) {
        return new RegistryEntries<>(Arrays.stream(ids).map(Either::<TagKey<T>, ResourceLocation>right).toList());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean contains(Holder<T> holder) {
        return entries.stream().anyMatch(entry -> entry.map(holder::is, holder::is));
    }

    /** The entries given by id, leaving out the tags. */
    public List<ResourceLocation> ids() {
        return entries.stream().flatMap(entry -> entry.right().stream()).toList();
    }
}
