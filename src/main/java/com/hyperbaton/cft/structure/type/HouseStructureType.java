package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.StructureType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

public class HouseStructureType extends EnclosedBuildingStructureType {

    public static final Codec<HouseStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("id").forGetter(StructureType::getId),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("key_block").forGetter(s -> Optional.ofNullable(s.getKeyBlock())),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("key_block_tag").forGetter(s -> Optional.ofNullable(s.getKeyBlockTag())),
            Codec.INT.optionalFieldOf("max_users", 1).forGetter(StructureType::getMaxUsers),
            Codec.BOOL.optionalFieldOf("requires_container", false).forGetter(StructureType::isRequiresContainer),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StructureType::getPriority),
            ValidBlock.CODEC.listOf().fieldOf("floorBlocks").forGetter(EnclosedBuildingStructureType::getFloorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("wallBlocks").forGetter(EnclosedBuildingStructureType::getWallBlocks),
            ValidBlock.CODEC.listOf().fieldOf("interiorBlocks").forGetter(EnclosedBuildingStructureType::getInteriorBlocks),
            ValidBlock.CODEC.listOf().fieldOf("roofBlocks").forGetter(EnclosedBuildingStructureType::getRoofBlocks)
    ).apply(inst, HouseStructureType::new));

    public HouseStructureType(String id, Optional<Block> keyBlock, Optional<TagKey<Block>> keyBlockTag,
                              int maxUsers, boolean requiresContainer, int priority,
                              List<ValidBlock> floorBlocks, List<ValidBlock> wallBlocks,
                              List<ValidBlock> interiorBlocks, List<ValidBlock> roofBlocks) {
        super(id, keyBlock, keyBlockTag, maxUsers, requiresContainer, priority,
                floorBlocks, wallBlocks, interiorBlocks, roofBlocks);
    }

    @Override
    public Codec<? extends StructureType> structureTypeCodec() {
        return CftRegistry.HOUSE_STRUCTURE_TYPE.get();
    }
}
