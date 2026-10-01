package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.structure.LightingRequirement;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.StructureType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public class HouseStructureType extends EnclosedBuildingStructureType {

    public static final Codec<HouseStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            propertiesCodec(),
            ValidBlock.CODEC.listOf().fieldOf("floor_blocks").forGetter(EnclosedBuildingStructureType::getFloorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("wall_blocks").forGetter(EnclosedBuildingStructureType::getWallBlocks),
            ValidBlock.CODEC.listOf().fieldOf("interior_blocks").forGetter(EnclosedBuildingStructureType::getInteriorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("roof_blocks").forGetter(EnclosedBuildingStructureType::getRoofBlocks),
            LightingRequirement.CODEC.optionalFieldOf("lighting").forGetter(EnclosedBuildingStructureType::getLighting)
    ).apply(inst, HouseStructureType::new));

    public HouseStructureType(Properties properties, List<ValidBlock> floorBlocks, List<ValidBlock> wallBlocks,
                              List<ValidBlock> interiorBlocks, List<ValidBlock> roofBlocks,
                              Optional<LightingRequirement> lighting) {
        super(properties, floorBlocks, wallBlocks, interiorBlocks, roofBlocks, lighting);
    }

    @Override
    public Codec<? extends StructureType> structureTypeCodec() {
        return CftRegistry.HOUSE_STRUCTURE_TYPE.get();
    }
}
