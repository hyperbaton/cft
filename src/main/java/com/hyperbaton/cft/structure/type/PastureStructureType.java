package com.hyperbaton.cft.structure.type;

import net.minecraft.world.entity.EntityType;
import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.detector.PastureDetector;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

/**
 * An open-air platform that additionally requires a minimum (and optionally maximum)
 * number of specific animals to be present inside its footprint, e.g. a pasture pen.
 */
public class PastureStructureType extends OpenAirPlatformStructureType {

    public static final Codec<PastureStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            propertiesCodec(),
            Codec.INT.optionalFieldOf("wall_height", 1).forGetter(OpenAirPlatformStructureType::getWallHeight),
            ValidBlock.CODEC.listOf().fieldOf("border_blocks").forGetter(OpenAirPlatformStructureType::getBorderBlocks),
            ValidBlock.CODEC.listOf().fieldOf("ground_perimeter_blocks").forGetter(OpenAirPlatformStructureType::getGroundPerimeterBlocks),
            ValidBlock.CODEC.listOf().fieldOf("surface_blocks").forGetter(OpenAirPlatformStructureType::getSurfaceBlocks),
            RegistryEntries.codec(Registries.ENTITY_TYPE).fieldOf("eligible_mobs").forGetter(PastureStructureType::getEligibleMobs),
            Codec.INT.optionalFieldOf("min_mob_count", 1).forGetter(PastureStructureType::getMinMobCount),
            Codec.INT.optionalFieldOf("max_mob_count", Integer.MAX_VALUE).forGetter(PastureStructureType::getMaxMobCount)
    ).apply(inst, PastureStructureType::new));

    private final RegistryEntries<EntityType<?>> eligibleMobs;
    private final int minMobCount;
    private final int maxMobCount;

    public PastureStructureType(Properties properties, int wallHeight, List<ValidBlock> borderBlocks,
                                List<ValidBlock> groundPerimeterBlocks, List<ValidBlock> surfaceBlocks,
                                RegistryEntries<EntityType<?>> eligibleMobs, int minMobCount, int maxMobCount) {
        super(properties, wallHeight, borderBlocks, groundPerimeterBlocks, surfaceBlocks);
        this.eligibleMobs = eligibleMobs;
        this.minMobCount = minMobCount;
        this.maxMobCount = maxMobCount;
    }

    public RegistryEntries<EntityType<?>> getEligibleMobs() {
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
