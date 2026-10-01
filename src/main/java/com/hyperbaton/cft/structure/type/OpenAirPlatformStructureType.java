package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.detector.OpenAirPlatformDetector;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

public class OpenAirPlatformStructureType extends StructureType {

    public static final Codec<OpenAirPlatformStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            propertiesCodec(),
            Codec.INT.optionalFieldOf("wall_height", 1).forGetter(OpenAirPlatformStructureType::getWallHeight),
            ValidBlock.CODEC.listOf().fieldOf("border_blocks").forGetter(OpenAirPlatformStructureType::getBorderBlocks),
            ValidBlock.CODEC.listOf().fieldOf("ground_perimeter_blocks").forGetter(OpenAirPlatformStructureType::getGroundPerimeterBlocks),
            ValidBlock.CODEC.listOf().fieldOf("surface_blocks").forGetter(OpenAirPlatformStructureType::getSurfaceBlocks)
    ).apply(inst, OpenAirPlatformStructureType::new));

    private final int wallHeight;
    private final List<ValidBlock> borderBlocks;
    private final List<ValidBlock> groundPerimeterBlocks;
    private final List<ValidBlock> surfaceBlocks;

    public OpenAirPlatformStructureType(Properties properties, int wallHeight, List<ValidBlock> borderBlocks,
                                        List<ValidBlock> groundPerimeterBlocks, List<ValidBlock> surfaceBlocks) {
        super(properties);
        this.wallHeight = wallHeight;
        this.borderBlocks = borderBlocks;
        this.groundPerimeterBlocks = groundPerimeterBlocks;
        this.surfaceBlocks = surfaceBlocks;
    }

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId) {
        return new OpenAirPlatformDetector().detect(keyBlockPos, level, leaderId, this);
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
