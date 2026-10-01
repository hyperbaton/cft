package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.VisitNeed;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureUtils;
import com.hyperbaton.cft.util.ContainerUtil;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Satisfied as soon as the Xoonglin is at a structure of the required type, taking the goods the
 * visit consumes from its containers. Until then, the MUST_VISIT memory sends it to the nearest
 * one; once there, the VISITING memory keeps it around for the stay duration.
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

        // Checked once it gets there: if it was torn down, it's unregistered and it looks for another one
        Optional<Structure> visited = candidateStructures(mob)
                .filter(structure -> isAt(mob, structure))
                .findFirst()
                .filter(structure -> StructureUtils.recheck((ServerLevel) mob.level(), structure).success());
        if (visited.isPresent()) {
            if (!need.getConsumes().isEmpty()) {
                ContainerUtil.consumeIngredients(ContainerUtil.findContainers((ServerLevel) mob.level(), visited.get()),
                        need.getConsumes());
            }
            super.satisfy(mob);
            mob.getBrain().eraseMemory(CftMemoryModuleType.MUST_VISIT.get());
            mob.getBrain().setMemoryWithExpiry(CftMemoryModuleType.VISITING.get(),
                    visited.get().getKeyBlockPos(), need.getStayDuration());
            return true;
        }

        return failAndSeek(mob);
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

    /**
     * Structures of the required type, of the Xoonglin's leader, within reach of its home, running
     * if the need requires it, and holding the goods the visit consumes, if any.
     */
    private Stream<Structure> candidateStructures(XoonglinEntity mob) {
        ServerLevel level = (ServerLevel) mob.level();
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");
        BlockPos homePos = mob.getHome() != null ? mob.getHome().getEntrance() : mob.blockPosition();
        return data.getStructures().stream()
                .filter(structure -> structure.getStructureTypeId().equals(need.getRequiredStructure()))
                .filter(structure -> structure.getLeaderId().equals(mob.getLeaderId()))
                .filter(structure -> structure.getKeyBlockPos().distManhattan(homePos) <= need.getSearchRadius())
                .filter(structure -> !need.isRequiresRunning()
                        || JobUtil.isStructureRunning(level, structure, need.getRunningWorkSteps()))
                .filter(structure -> need.getConsumes().isEmpty()
                        || ContainerUtil.hasAllIngredients(ContainerUtil.findContainers(level, structure), need.getConsumes()));
    }

    /** Inside the structure, or right next to it (e.g. standing on a plaza). */
    private static boolean isAt(XoonglinEntity mob, Structure structure) {
        return structure.getBounds()
                .map(bounds -> bounds.inflatedBy(1).isInside(mob.blockPosition()))
                .orElse(false);
    }
}
