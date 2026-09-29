package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.VisitNeed;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Satisfied as soon as the Xoonglin is at a structure of the required type. Until then, the
 * MUST_VISIT memory sends it to the nearest one; once there, the VISITING memory keeps it
 * around for the stay duration.
 */
public class VisitNeedSatisfier extends NeedSatisfier<VisitNeed> {

    public VisitNeedSatisfier(double satisfaction, boolean isSatisfied, VisitNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.level().isClientSide) {
            return isSatisfied();
        }

        Optional<Structure> visited = candidateStructures(mob)
                .filter(structure -> isAt(mob, structure))
                .findFirst();
        if (visited.isPresent()) {
            super.satisfy(mob);
            mob.getBrain().eraseMemory(CftMemoryModuleType.MUST_VISIT.get());
            mob.getBrain().setMemoryWithExpiry(CftMemoryModuleType.VISITING.get(),
                    visited.get().getKeyBlockPos(), need.getStayDuration());
            return true;
        }

        this.unsatisfy(need.getFrequency(), mob);
        mob.decreaseHappiness(need);
        addMemoriesForSatisfaction(mob);
        return false;
    }

    /** Points the Xoonglin to the nearest structure to visit, which may change as it moves. */
    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        candidateStructures(mob)
                .min(Comparator.comparingInt(structure -> structure.getKeyBlockPos().distManhattan(mob.blockPosition())))
                .ifPresentOrElse(
                        structure -> mob.getBrain().setMemory(CftMemoryModuleType.MUST_VISIT.get(), structure.getKeyBlockPos()),
                        () -> mob.getBrain().eraseMemory(CftMemoryModuleType.MUST_VISIT.get()));
    }

    /** Structures of the required type, of the Xoonglin's leader, within reach of its home. */
    private Stream<Structure> candidateStructures(XoonglinEntity mob) {
        ServerLevel level = (ServerLevel) mob.level();
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        BlockPos homePos = mob.getHome() != null ? mob.getHome().getEntrance() : mob.blockPosition();
        return data.getStructures().stream()
                .filter(structure -> structure.getStructureTypeId().equals(need.getRequiredStructure()))
                .filter(structure -> structure.getLeaderId().equals(mob.getLeaderId()))
                .filter(structure -> structure.getKeyBlockPos().distManhattan(homePos) <= need.getSearchRadius());
    }

    /** Inside the structure, or right next to it (e.g. standing on a plaza). */
    private static boolean isAt(XoonglinEntity mob, Structure structure) {
        return structure.getBounds()
                .map(bounds -> bounds.inflatedBy(1).isInside(mob.blockPosition()))
                .orElse(false);
    }
}
