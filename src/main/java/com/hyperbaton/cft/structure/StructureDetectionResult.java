package com.hyperbaton.cft.structure;

import net.minecraft.network.chat.Component;
import java.util.List;

public record StructureDetectionResult(
        boolean success,
        StructureDetectionReasons reason,
        List<Component> validationDetails,
        Structure structure
) {
    public static StructureDetectionResult success(Structure structure) {
        return new StructureDetectionResult(true, StructureDetectionReasons.STRUCTURE_DETECTED, List.of(), structure);
    }

    public static StructureDetectionResult failure(StructureDetectionReasons reason, List<Component> details) {
        return new StructureDetectionResult(false, reason, details, null);
    }

    public static StructureDetectionResult failure(StructureDetectionReasons reason) {
        return failure(reason, List.of());
    }
}
