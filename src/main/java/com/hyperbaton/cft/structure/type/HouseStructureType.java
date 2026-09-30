package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.structure.LightingRequirement;
import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.StructureType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

public class HouseStructureType extends EnclosedBuildingStructureType {

    public static final Codec<HouseStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            RegistryEntries.codec(Registries.BLOCK).fieldOf("key_block").forGetter(StructureType::getKeyBlock),
            Codec.INT.optionalFieldOf("max_users", 1).forGetter(StructureType::getMaxUsers),
            Codec.BOOL.optionalFieldOf("requires_container", false).forGetter(StructureType::isRequiresContainer),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StructureType::getPriority),
            ValidBlock.CODEC.listOf().fieldOf("floorBlocks").forGetter(EnclosedBuildingStructureType::getFloorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("wallBlocks").forGetter(EnclosedBuildingStructureType::getWallBlocks),
            ValidBlock.CODEC.listOf().fieldOf("interiorBlocks").forGetter(EnclosedBuildingStructureType::getInteriorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("roofBlocks").forGetter(EnclosedBuildingStructureType::getRoofBlocks),
            LightingRequirement.CODEC.optionalFieldOf("lighting").forGetter(EnclosedBuildingStructureType::getLighting)
    ).apply(inst, HouseStructureType::new));

    public HouseStructureType(RegistryEntries<Block> keyBlock,
                              int maxUsers, boolean requiresContainer, int priority,
                              List<ValidBlock> floorBlocks, List<ValidBlock> wallBlocks,
                              List<ValidBlock> interiorBlocks, List<ValidBlock> roofBlocks,
                              Optional<LightingRequirement> lighting) {
        super(keyBlock, maxUsers, requiresContainer, priority,
                floorBlocks, wallBlocks, interiorBlocks, roofBlocks, lighting);
    }

    @Override
    public Codec<? extends StructureType> structureTypeCodec() {
        return CftRegistry.HOUSE_STRUCTURE_TYPE.get();
    }
}
