package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.structure.LightingRequirement;
import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.detector.EnclosedBuildingDetector;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EnclosedBuildingStructureType extends StructureType {

    public static final Codec<EnclosedBuildingStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            RegistryEntries.codec(Registries.BLOCK).fieldOf("key_block").forGetter(StructureType::getKeyBlock),
            Codec.INT.optionalFieldOf("max_users", 1).forGetter(StructureType::getMaxUsers),
            Codec.BOOL.optionalFieldOf("requires_container", false).forGetter(StructureType::isRequiresContainer),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StructureType::getPriority),
            ValidBlock.CODEC.listOf().fieldOf("floor_blocks").forGetter(EnclosedBuildingStructureType::getFloorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("wall_blocks").forGetter(EnclosedBuildingStructureType::getWallBlocks),
            ValidBlock.CODEC.listOf().fieldOf("interior_blocks").forGetter(EnclosedBuildingStructureType::getInteriorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("roof_blocks").forGetter(EnclosedBuildingStructureType::getRoofBlocks),
            LightingRequirement.CODEC.optionalFieldOf("lighting").forGetter(EnclosedBuildingStructureType::getLighting)
    ).apply(inst, EnclosedBuildingStructureType::new));

    private final List<ValidBlock> floorBlocks;
    private final List<ValidBlock> wallBlocks;
    private final List<ValidBlock> interiorBlocks;
    private final List<ValidBlock> roofBlocks;
    /** How well lit the inside must be; null if it doesn't matter. */
    private final LightingRequirement lighting;

    public EnclosedBuildingStructureType(RegistryEntries<Block> keyBlock,
                                         int maxUsers, boolean requiresContainer, int priority,
                                         List<ValidBlock> floorBlocks, List<ValidBlock> wallBlocks,
                                         List<ValidBlock> interiorBlocks, List<ValidBlock> roofBlocks,
                                         Optional<LightingRequirement> lighting) {
        super(keyBlock, maxUsers, requiresContainer, priority);
        this.floorBlocks = floorBlocks;
        this.wallBlocks = wallBlocks;
        this.interiorBlocks = interiorBlocks;
        this.roofBlocks = roofBlocks;
        this.lighting = lighting.orElse(null);
    }

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId) {
        return new EnclosedBuildingDetector().detect(keyBlockPos, level, leaderId, this);
    }

    @Override
    public Codec<? extends StructureType> structureTypeCodec() {
        return CftRegistry.ENCLOSED_BUILDING_STRUCTURE_TYPE.get();
    }

    public List<ValidBlock> getFloorBlocks() {
        return floorBlocks;
    }

    public List<ValidBlock> getWallBlocks() {
        return wallBlocks;
    }

    public List<ValidBlock> getInteriorBlocks() {
        return interiorBlocks;
    }

    public List<ValidBlock> getRoofBlocks() {
        return roofBlocks;
    }

    public Optional<LightingRequirement> getLighting() {
        return Optional.ofNullable(lighting);
    }
}
