package com.hyperbaton.cft.structure.detector;

import com.google.common.collect.Sets;
import com.hyperbaton.cft.structure.EntityTypeMatcher;
import com.hyperbaton.cft.structure.OpenAirPlatformBlockGroup;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureDetectionReasons;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.type.PastureStructureType;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Detects the pasture's shape exactly like an open-air platform, then additionally
 * requires a minimum (and optionally maximum) number of eligible animals to be present
 * inside its footprint.
 */
public class PastureDetector implements StructureDetector<PastureStructureType> {

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId,
                                           PastureStructureType structureType) {
        StructureDetectionResult shapeResult = new OpenAirPlatformDetector().detect(keyBlockPos, level, leaderId, structureType);
        if (!shapeResult.success()) {
            return shapeResult;
        }

        List<BlockPos> border = shapeResult.structure().getBlockPositions().get(OpenAirPlatformBlockGroup.BORDER.getKey());
        List<BlockPos> surface = shapeResult.structure().getBlockPositions().get(OpenAirPlatformBlockGroup.SURFACE.getKey());

        // A pasture cannot share blocks with an already registered pasture: two key
        // blocks (e.g. two hay bales) in the same pen would otherwise each detect their
        // own overlapping copy of it
        if (overlapsAnotherPasture(level, keyBlockPos, shapeResult.structure(), structureType.getId())) {
            return StructureDetectionResult.failure(StructureDetectionReasons.OVERLAPPING_STRUCTURE);
        }

        AABB area = boundingBox(border, surface);
        int found = countEligibleMobs(level, area, structureType.getEligibleMobs());

        if (found < structureType.getMinMobCount() || found > structureType.getMaxMobCount()) {
            String needed = structureType.getMaxMobCount() == Integer.MAX_VALUE
                    ? "needs at least " + structureType.getMinMobCount()
                    : "needs between " + structureType.getMinMobCount() + " and " + structureType.getMaxMobCount();
            return StructureDetectionResult.failure(StructureDetectionReasons.NOT_ENOUGH_ANIMALS,
                    List.of("Found " + found + " eligible animals, " + needed));
        }

        return shapeResult;
    }

    private boolean overlapsAnotherPasture(ServerLevel level, BlockPos keyBlockPos, Structure detected, String structureTypeId) {
        StructuresData structuresData = level.getDataStorage()
                .computeIfAbsent(StructuresData.factory(), "structuresData");
        Set<BlockPos> occupiedBlocks = Sets.newHashSet();
        for (Structure existing : structuresData.getStructures()) {
            if (!existing.getStructureTypeId().equals(structureTypeId)) continue;
            // Skip the pasture already registered at this key block: it is "itself"
            // when this detection is a re-validation of an existing pasture
            if (existing.getKeyBlockPos().equals(keyBlockPos)) continue;
            occupiedBlocks.add(existing.getKeyBlockPos());
            existing.getBlockPositions().values().forEach(occupiedBlocks::addAll);
        }
        return detected.getBlockPositions().values().stream()
                .anyMatch(blocks -> blocks.stream().anyMatch(occupiedBlocks::contains));
    }

    /** A box covering the pen's footprint from ground to just above the walls. */
    private AABB boundingBox(List<BlockPos> border, List<BlockPos> surface) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (List<BlockPos> group : List.of(border, surface)) {
            for (BlockPos pos : group) {
                minX = Math.min(minX, pos.getX());
                maxX = Math.max(maxX, pos.getX());
                minY = Math.min(minY, pos.getY());
                maxY = Math.max(maxY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxZ = Math.max(maxZ, pos.getZ());
            }
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 2, maxZ + 1);
    }

    private int countEligibleMobs(ServerLevel level, AABB area, List<EntityTypeMatcher> eligibleMobs) {
        List<Entity> nearby = level.getEntities((Entity) null, area,
                entity -> entity.isAlive() && matchesAny(entity.getType(), eligibleMobs));
        return nearby.size();
    }

    private boolean matchesAny(EntityType<?> type, List<EntityTypeMatcher> eligibleMobs) {
        for (EntityTypeMatcher matcher : eligibleMobs) {
            if (matcher.matches(type)) return true;
        }
        return false;
    }
}
