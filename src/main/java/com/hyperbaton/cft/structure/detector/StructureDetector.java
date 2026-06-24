package com.hyperbaton.cft.structure.detector;

import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public interface StructureDetector {
    StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId, StructureType structureType);
}
