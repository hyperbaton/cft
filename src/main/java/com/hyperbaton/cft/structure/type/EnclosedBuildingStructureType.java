package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.structure.LightingRequirement;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.detector.EnclosedBuildingDetector;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EnclosedBuildingStructureType extends StructureType {

    public static final Codec<EnclosedBuildingStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            propertiesCodec(),
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

    public EnclosedBuildingStructureType(Properties properties, List<ValidBlock> floorBlocks, List<ValidBlock> wallBlocks,
                                         List<ValidBlock> interiorBlocks, List<ValidBlock> roofBlocks,
                                         Optional<LightingRequirement> lighting) {
        super(properties);
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
