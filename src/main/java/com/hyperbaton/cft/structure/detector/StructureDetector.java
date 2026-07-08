package com.hyperbaton.cft.structure.detector;

import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * A stateless computation that checks whether the structure of type {@code T} stands at a
 * given key block. Detectors hold no state: they are created, used once, and discarded.
 * The type parameter makes the pairing type-safe, so no cast is needed inside the detector.
 */
public interface StructureDetector<T extends StructureType> {

    StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId, T structureType);
}
