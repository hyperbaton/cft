package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.EntityTypeMatcher;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.detector.PastureDetector;
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
 * An open-air platform that additionally requires a minimum (and optionally maximum)
 * number of specific animals to be present inside its footprint, e.g. a pasture pen.
 */
public class PastureStructureType extends OpenAirPlatformStructureType {

    public static final Codec<PastureStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("id").forGetter(StructureType::getId),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("key_block").forGetter(s -> Optional.ofNullable(s.getKeyBlock())),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("key_block_tag").forGetter(s -> Optional.ofNullable(s.getKeyBlockTag())),
            Codec.INT.optionalFieldOf("max_users", 1).forGetter(StructureType::getMaxUsers),
            Codec.BOOL.optionalFieldOf("requires_container", false).forGetter(StructureType::isRequiresContainer),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StructureType::getPriority),
            Codec.INT.optionalFieldOf("wall_height", 1).forGetter(OpenAirPlatformStructureType::getWallHeight),
            ValidBlock.CODEC.listOf().fieldOf("borderBlocks").forGetter(OpenAirPlatformStructureType::getBorderBlocks),
            ValidBlock.CODEC.listOf().fieldOf("groundPerimeterBlocks").forGetter(OpenAirPlatformStructureType::getGroundPerimeterBlocks),
            ValidBlock.CODEC.listOf().fieldOf("surfaceBlocks").forGetter(OpenAirPlatformStructureType::getSurfaceBlocks),
            EntityTypeMatcher.CODEC.listOf().fieldOf("eligible_mobs").forGetter(PastureStructureType::getEligibleMobs),
            Codec.INT.optionalFieldOf("min_mob_count", 1).forGetter(PastureStructureType::getMinMobCount),
            Codec.INT.optionalFieldOf("max_mob_count", Integer.MAX_VALUE).forGetter(PastureStructureType::getMaxMobCount)
    ).apply(inst, PastureStructureType::new));

    private final List<EntityTypeMatcher> eligibleMobs;
    private final int minMobCount;
    private final int maxMobCount;

    public PastureStructureType(String id, Optional<Block> keyBlock, Optional<TagKey<Block>> keyBlockTag,
                                int maxUsers, boolean requiresContainer, int priority,
                                int wallHeight,
                                List<ValidBlock> borderBlocks, List<ValidBlock> groundPerimeterBlocks,
                                List<ValidBlock> surfaceBlocks,
                                List<EntityTypeMatcher> eligibleMobs, int minMobCount, int maxMobCount) {
        super(id, keyBlock, keyBlockTag, maxUsers, requiresContainer, priority,
                wallHeight, borderBlocks, groundPerimeterBlocks, surfaceBlocks);
        this.eligibleMobs = List.copyOf(eligibleMobs);
        this.minMobCount = minMobCount;
        this.maxMobCount = maxMobCount;
    }

    public List<EntityTypeMatcher> getEligibleMobs() {
        return eligibleMobs;
    }

    public int getMinMobCount() {
        return minMobCount;
    }

    public int getMaxMobCount() {
        return maxMobCount;
    }

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId) {
        return new PastureDetector().detect(keyBlockPos, level, leaderId, this);
    }

    @Override
    public Codec<? extends StructureType> structureTypeCodec() {
        return CftRegistry.PASTURE_STRUCTURE_TYPE.get();
    }
}
