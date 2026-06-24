package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.HomeValidBlock;
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
            HomeValidBlock.HOME_VALID_BLOCK_CODEC.listOf().fieldOf("floorBlocks").forGetter(EnclosedBuildingStructureType::getFloorBlocks),
            HomeValidBlock.HOME_VALID_BLOCK_CODEC.listOf().fieldOf("wallBlocks").forGetter(EnclosedBuildingStructureType::getWallBlocks),
            HomeValidBlock.HOME_VALID_BLOCK_CODEC.listOf().fieldOf("interiorBlocks").forGetter(EnclosedBuildingStructureType::getInteriorBlocks),
            HomeValidBlock.HOME_VALID_BLOCK_CODEC.listOf().fieldOf("roofBlocks").forGetter(EnclosedBuildingStructureType::getRoofBlocks)
    ).apply(inst, EnclosedBuildingStructureType::new));

    private final List<HomeValidBlock> floorBlocks;
    private final List<HomeValidBlock> wallBlocks;
    private final List<HomeValidBlock> interiorBlocks;
    private final List<HomeValidBlock> roofBlocks;

    public EnclosedBuildingStructureType(String id, Optional<Block> keyBlock, Optional<TagKey<Block>> keyBlockTag,
                                         int maxUsers, boolean requiresContainer, int priority,
                                         List<HomeValidBlock> floorBlocks, List<HomeValidBlock> wallBlocks,
                                         List<HomeValidBlock> interiorBlocks, List<HomeValidBlock> roofBlocks) {
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

    public List<HomeValidBlock> getFloorBlocks() {
        return floorBlocks;
    }

    public List<HomeValidBlock> getWallBlocks() {
        return wallBlocks;
    }

    public List<HomeValidBlock> getInteriorBlocks() {
        return interiorBlocks;
    }

    public List<HomeValidBlock> getRoofBlocks() {
        return roofBlocks;
    }
}
