package com.hyperbaton.cft.structure;

import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.CftRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public abstract class StructureType {

    public static final Codec<StructureType> STRUCTURE_TYPE_CODEC = Codec.lazyInitialized(
            () -> CftRegistry.STRUCTURE_TYPE_CODEC_REGISTRY.byNameCodec()
                    .dispatch("type", StructureType::structureTypeCodec, codec -> MapCodec.assumeMapUnsafe(codec))
    );

    private final RegistryEntries<Block> keyBlock;
    private final int maxUsers;
    private final boolean requiresContainer;
    private final int priority;

    protected StructureType(RegistryEntries<Block> keyBlock, int maxUsers, boolean requiresContainer, int priority) {
        this.keyBlock = keyBlock;
        this.maxUsers = maxUsers;
        this.requiresContainer = requiresContainer;
        this.priority = priority;
    }

    /**
     * Runs this type's detector at the key block. Implemented per type so it can pass
     * {@code this} (the concrete type) to a stateless detector dedicated for each type
     */
    public abstract StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId);

    public abstract Codec<? extends StructureType> structureTypeCodec();

    public boolean matchesKeyBlock(BlockState state) {
        return keyBlock.contains(state.getBlockHolder());
    }

    public boolean isRequiresContainer() {
        return requiresContainer;
    }

    public RegistryEntries<Block> getKeyBlock() {
        return keyBlock;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public int getPriority() {
        return priority;
    }
}
