package com.hyperbaton.cft.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/**
 * Matches an entity type either by its exact ID (e.g. "minecraft:cow") or by an entity
 * type tag (e.g. "#minecraft:skeletons"), mirroring how Ingredient entries can be either
 * an item or a tag, so a list of these can mix both freely.
 */
public record EntityTypeMatcher(Optional<EntityType<?>> type, Optional<TagKey<EntityType<?>>> tag) {

    public static final Codec<EntityTypeMatcher> CODEC = Codec.STRING.comapFlatMap(
            EntityTypeMatcher::parse, EntityTypeMatcher::serialize);

    private static DataResult<EntityTypeMatcher> parse(String value) {
        if (value.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(value.substring(1));
            if (id == null) return DataResult.error(() -> "Invalid entity type tag: " + value);
            return DataResult.success(new EntityTypeMatcher(Optional.empty(),
                    Optional.of(TagKey.create(Registries.ENTITY_TYPE, id))));
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) return DataResult.error(() -> "Invalid entity type: " + value);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) return DataResult.error(() -> "Unknown entity type: " + value);
        return DataResult.success(new EntityTypeMatcher(Optional.of(type), Optional.empty()));
    }

    private static String serialize(EntityTypeMatcher matcher) {
        return matcher.tag.map(t -> "#" + t.location())
                .orElseGet(() -> BuiltInRegistries.ENTITY_TYPE.getKey(matcher.type.orElseThrow()).toString());
    }

    public boolean matches(EntityType<?> candidate) {
        if (type.isPresent()) return type.get() == candidate;
        return tag.map(candidate::is).orElse(false);
    }
}
