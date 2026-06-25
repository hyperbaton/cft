package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.detector.OpenAirPlatformDetector;
import com.hyperbaton.cft.structure.detector.StructureDetector;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

public class OpenAirPlatformStructureType extends StructureType {

    public static final Codec<OpenAirPlatformStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("id").forGetter(StructureType::getId),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("key_block").forGetter(s -> Optional.ofNullable(s.getKeyBlock())),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("key_block_tag").forGetter(s -> Optional.ofNullable(s.getKeyBlockTag())),
            Codec.INT.optionalFieldOf("max_users", 1).forGetter(StructureType::getMaxUsers),
            Codec.BOOL.optionalFieldOf("requires_container", false).forGetter(StructureType::isRequiresContainer),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StructureType::getPriority),
            Codec.INT.optionalFieldOf("wall_height", 1).forGetter(OpenAirPlatformStructureType::getWallHeight),
            ValidBlock.CODEC.listOf().fieldOf("borderBlocks").forGetter(OpenAirPlatformStructureType::getBorderBlocks),
            ValidBlock.CODEC.listOf().fieldOf("groundPerimeterBlocks").forGetter(OpenAirPlatformStructureType::getGroundPerimeterBlocks),
            ValidBlock.CODEC.listOf().fieldOf("surfaceBlocks").forGetter(OpenAirPlatformStructureType::getSurfaceBlocks)
    ).apply(inst, OpenAirPlatformStructureType::new));

    private final int wallHeight;
    private final List<ValidBlock> borderBlocks;
    private final List<ValidBlock> groundPerimeterBlocks;
    private final List<ValidBlock> surfaceBlocks;

    public OpenAirPlatformStructureType(String id, Optional<Block> keyBlock, Optional<TagKey<Block>> keyBlockTag,
                                         int maxUsers, boolean requiresContainer, int priority,
                                         int wallHeight,
                                         List<ValidBlock> borderBlocks, List<ValidBlock> groundPerimeterBlocks,
                                         List<ValidBlock> surfaceBlocks) {
        super(id, keyBlock, keyBlockTag, maxUsers, requiresContainer, priority);
        this.wallHeight = wallHeight;
        this.borderBlocks = borderBlocks;
        this.groundPerimeterBlocks = groundPerimeterBlocks;
        this.surfaceBlocks = surfaceBlocks;
    }

    @Override
    public StructureDetector createDetector() {
        return new OpenAirPlatformDetector();
    }

    @Override
    public Codec<? extends StructureType> structureTypeCodec() {
        return CftRegistry.OPEN_AIR_PLATFORM_STRUCTURE_TYPE.get();
    }

    public int getWallHeight() {
        return wallHeight;
    }

    public List<ValidBlock> getBorderBlocks() {
        return borderBlocks;
    }

    public List<ValidBlock> getGroundPerimeterBlocks() {
        return groundPerimeterBlocks;
    }

    public List<ValidBlock> getSurfaceBlocks() {
        return surfaceBlocks;
    }
}
