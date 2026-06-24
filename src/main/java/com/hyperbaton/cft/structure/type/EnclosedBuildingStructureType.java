package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.detector.EnclosedBuildingDetector;
import com.hyperbaton.cft.structure.detector.StructureDetector;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

public class EnclosedBuildingStructureType extends StructureType {

    public static final Codec<EnclosedBuildingStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
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
    ).apply(inst, EnclosedBuildingStructureType::new));

    private final List<ValidBlock> floorBlocks;
    private final List<ValidBlock> wallBlocks;
    private final List<ValidBlock> interiorBlocks;
    private final List<ValidBlock> roofBlocks;

    public EnclosedBuildingStructureType(String id, Optional<Block> keyBlock, Optional<TagKey<Block>> keyBlockTag,
                                         int maxUsers, boolean requiresContainer, int priority,
                                         List<ValidBlock> floorBlocks, List<ValidBlock> wallBlocks,
                                         List<ValidBlock> interiorBlocks, List<ValidBlock> roofBlocks) {
        super(id, keyBlock, keyBlockTag, maxUsers, requiresContainer, priority);
        this.floorBlocks = floorBlocks;
        this.wallBlocks = wallBlocks;
        this.interiorBlocks = interiorBlocks;
        this.roofBlocks = roofBlocks;
    }

    @Override
    public StructureDetector createDetector() {
        return new EnclosedBuildingDetector();
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
}
