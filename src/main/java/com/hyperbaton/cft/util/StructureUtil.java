package com.hyperbaton.cft.util;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.api.event.StructureDetectedEvent;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureDetectionReasons;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.world.StructuresData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Keeps the registered structures true to the world. A structure is detected again when it's
 * about to be used (claimed, checked by a need, or when a job starts working there) and when the
 * leader clicks it with the staff; one that no longer passes is unregistered, and the Xoonglins
 * using it give it up the next time they're about to use it.
 */
public final class StructureUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StructureUtil() {
    }

    /**
     * Detects a structure of a type at a key block. Every detection goes through here, so a
     * structure that passes its type's checks is also offered to the {@link StructureDetectedEvent}
     * listeners, which may still fail it.
     */
    public static StructureDetectionResult detect(ServerLevel level, StructureType type, BlockPos keyBlockPos,
                                                  UUID leaderId) {
        StructureDetectionResult result = type.detect(keyBlockPos, level, leaderId);
        if (!result.success()) return result;
        StructureDetectedEvent event = NeoForge.EVENT_BUS.post(new StructureDetectedEvent(level, type, result.structure()));
        return event.getFailure().orElse(result);
    }

    /**
     * Detects a registered structure again, with its own type. If it still passes, it takes the
     * blocks found, which may have changed; if not, it's unregistered.
     */
    public static StructureDetectionResult recheck(ServerLevel level, Structure structure) {
        StructureType type = CftRegistry.getStructureType(structure.getStructureTypeId());
        StructureDetectionResult result = type != null
                ? detect(level, type, structure.getKeyBlockPos(), structure.getLeaderId())
                : StructureDetectionResult.failure(StructureDetectionReasons.NOT_A_KEY_BLOCK);
        if (result.success()) {
            structure.update(result.structure());
            StructuresData.get(level).setDirty();
        } else {
            unregister(level, structure);
        }
        return result;
    }

    /**
     * The structure of a type registered at a key block, detected again first: empty if there's
     * none, or if it no longer passes (then it's unregistered).
     */
    public static Optional<Structure> recheck(ServerLevel level, BlockPos keyBlockPos, ResourceLocation structureTypeId) {
        return StructuresData.get(level).findByKeyBlock(keyBlockPos)
                .filter(structure -> structure.getStructureTypeId().equals(structureTypeId))
                .filter(structure -> recheck(level, structure).success());
    }

    /** Removes a structure from the registered ones; the Xoonglins using it give it up. */
    public static void unregister(ServerLevel level, Structure structure) {
        StructuresData.get(level).removeStructure(structure);
        LOGGER.debug("Unregistered structure {} at {}", structure.getStructureTypeId(), structure.getKeyBlockPos());
    }

    /**
     * The position a structure's key block is registered at, from a position of that block: the
     * lower half of a door or any other two-block-tall block.
     */
    public static BlockPos keyBlockPos(BlockState state, BlockPos pos) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
                ? pos.below() : pos;
    }
}
