package com.hyperbaton.cft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.List;

/**
 * A list of entries of a registry, given the way vanilla data lists them: by id, or by tag with a
 * leading '#'. Unlike vanilla's holder set codecs, it's read without registry access, so needs
 * holding one can still be saved with the copy of the need each Xoonglin keeps.
 */
public record RegistryEntries<T>(List<Either<TagKey<T>, ResourceLocation>> entries) {

    public static <T> Codec<RegistryEntries<T>> codec(ResourceKey<? extends Registry<T>> registry) {
        return Codec.either(TagKey.hashedCodec(registry), ResourceLocation.CODEC).listOf()
                .xmap(RegistryEntries::new, RegistryEntries::entries);
    }

    public static <T> RegistryEntries<T> empty() {
        return new RegistryEntries<>(List.of());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean contains(Holder<T> holder) {
        return entries.stream().anyMatch(entry -> entry.map(holder::is, holder::is));
    }
}
