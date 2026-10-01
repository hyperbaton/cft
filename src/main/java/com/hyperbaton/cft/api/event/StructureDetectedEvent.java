package com.hyperbaton.cft.api.event;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureDetectionReason;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

import java.util.List;
import java.util.Optional;

/**
 * A structure passed the detection of its type, posted on the NeoForge event bus
 * ({@code NeoForge.EVENT_BUS}) on the server. Listeners can add their own checks and
 * {@link #fail} the detection.
 *
 * <p>Posted on every detection: when the leader registers or checks a structure with the staff, and
 * when a registered structure is detected again before it's used, which unregisters it if it fails.
 */
public class StructureDetectedEvent extends Event {
    private final ServerLevel level;
    private final StructureType structureType;
    private final Structure structure;
    private StructureDetectionResult failure;

    public StructureDetectedEvent(ServerLevel level, StructureType structureType, Structure structure) {
        this.level = level;
        this.structureType = structureType;
        this.structure = structure;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public StructureType getStructureType() {
        return structureType;
    }

    public ResourceLocation getStructureTypeId() {
        return CftRegistry.getStructureTypeId(structureType);
    }

    /** The structure found: its key block, leader and blocks. */
    public Structure getStructure() {
        return structure;
    }

    /**
     * Makes the detection fail: the player is told the reason and the details, as with the
     * structure type's own checks.
     */
    public void fail(StructureDetectionReason reason, List<Component> details) {
        this.failure = StructureDetectionResult.failure(reason, details);
    }

    public void fail(StructureDetectionReason reason) {
        fail(reason, List.of());
    }

    /** The failure a listener set, if any. */
    public Optional<StructureDetectionResult> getFailure() {
        return Optional.ofNullable(failure);
    }
}
