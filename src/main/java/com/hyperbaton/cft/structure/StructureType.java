package com.hyperbaton.cft.structure;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.detector.StructureDetector;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public abstract class StructureType {

    public static final Codec<StructureType> STRUCTURE_TYPE_CODEC = Codec.lazyInitialized(
            () -> CftRegistry.STRUCTURE_TYPE_CODEC_REGISTRY.byNameCodec()
                    .dispatch("type", StructureType::structureTypeCodec, codec -> MapCodec.assumeMapUnsafe(codec))
    );

    private final String id;
    private final Block keyBlock;
    private final TagKey<Block> keyBlockTag;
    private final int maxUsers;
    private final boolean requiresContainer;
    private final int priority;

    protected StructureType(String id, Optional<Block> keyBlock, Optional<TagKey<Block>> keyBlockTag,
                            int maxUsers, boolean requiresContainer, int priority) {
        this.id = id;
        this.keyBlock = keyBlock.orElse(null);
        this.keyBlockTag = keyBlockTag.orElse(null);
        this.maxUsers = maxUsers;
        this.requiresContainer = requiresContainer;
        this.priority = priority;
    }

    public abstract StructureDetector createDetector();

    public abstract Codec<? extends StructureType> structureTypeCodec();

    public boolean matchesKeyBlock(BlockState state) {
        if (keyBlock != null && state.is(keyBlock)) return true;
        return keyBlockTag != null && state.is(keyBlockTag);
    }

    public boolean isKeyBlock(BlockState state) {
        return matchesKeyBlock(state);
    }

    public boolean isRequiresContainer() {
        return requiresContainer;
    }

    public String getId() {
        return id;
    }

    public Block getKeyBlock() {
        return keyBlock;
    }

    public TagKey<Block> getKeyBlockTag() {
        return keyBlockTag;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public int getPriority() {
        return priority;
    }
}
