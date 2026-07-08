package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.RequiredStructure;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.detector.CompoundDetector;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A structure composed of other structures: an open surface (a plaza, a square, a
 * courtyard...) that is only valid if enough already detected structures of the
 * required types stand close to it. The surrounding buildings act as its "border".
 */
public class CompoundStructureType extends StructureType {

    public static final Codec<CompoundStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("id").forGetter(StructureType::getId),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("key_block").forGetter(s -> Optional.ofNullable(s.getKeyBlock())),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("key_block_tag").forGetter(s -> Optional.ofNullable(s.getKeyBlockTag())),
            Codec.INT.optionalFieldOf("max_users", 0).forGetter(StructureType::getMaxUsers),
            Codec.BOOL.optionalFieldOf("requires_container", false).forGetter(StructureType::isRequiresContainer),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StructureType::getPriority),
            ValidBlock.CODEC.listOf().fieldOf("surfaceBlocks").forGetter(CompoundStructureType::getSurfaceBlocks),
            RequiredStructure.CODEC.listOf().fieldOf("requiredStructures").forGetter(CompoundStructureType::getRequiredStructures),
            Codec.BOOL.optionalFieldOf("requires_sky_access", true).forGetter(CompoundStructureType::isRequiresSkyAccess)
    ).apply(inst, CompoundStructureType::new));

    private final List<ValidBlock> surfaceBlocks;
    private final List<RequiredStructure> requiredStructures;
    private final boolean requiresSkyAccess;

    public CompoundStructureType(String id, Optional<Block> keyBlock, Optional<TagKey<Block>> keyBlockTag,
                                 int maxUsers, boolean requiresContainer, int priority,
                                 List<ValidBlock> surfaceBlocks, List<RequiredStructure> requiredStructures,
                                 boolean requiresSkyAccess) {
        super(id, keyBlock, keyBlockTag, maxUsers, requiresContainer, priority);
        this.surfaceBlocks = surfaceBlocks;
        this.requiredStructures = requiredStructures;
        this.requiresSkyAccess = requiresSkyAccess;
    }

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId) {
        return new CompoundDetector().detect(keyBlockPos, level, leaderId, this);
    }

    @Override
    public Codec<? extends StructureType> structureTypeCodec() {
        return CftRegistry.COMPOUND_STRUCTURE_TYPE.get();
    }

    public List<ValidBlock> getSurfaceBlocks() {
        return surfaceBlocks;
    }

    public List<RequiredStructure> getRequiredStructures() {
        return requiredStructures;
    }

    public boolean isRequiresSkyAccess() {
        return requiresSkyAccess;
    }
}
