package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.StructureNeed;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.util.JobUtil;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class StructureNeedSatisfier extends NeedSatisfier<StructureNeed> {

    public StructureNeedSatisfier(double satisfaction, boolean isSatisfied, StructureNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.level().isClientSide) {
            return isSatisfied();
        }

        StructureNeed need = getNeed();
        ServerLevel level = (ServerLevel) mob.level();
        StructuresData data = level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData");

        if (need.isRequiresUsage()) {
            if (mob.getAssignedStructurePos(need.getRequiredStructure()) != null) {
                Optional<Structure> assigned = JobUtil.checkAssignedStructure(mob, need.getRequiredStructure())
                        .filter(s -> s.isUser(mob.getUUID()));
                if (assigned.isPresent()) {
                    if (isRunningIfRequired(level, assigned.get())) {
                        super.satisfy(mob);
                        return true;
                    }
                    // It has its structure, but nobody is working there now: nothing to look for
                    return fail(mob);
                }
            }
            // Not assigned yet — trigger behavior to find and claim
            return failAndSeek(mob);
        } else {
            // Only requires presence within search radius
            BlockPos homePos = mob.getHome() != null ? mob.getHome().getEntrance() : mob.blockPosition();
            boolean found = data.getStructures().stream()
                    .filter(s -> s.getStructureTypeId().equals(need.getRequiredStructure()))
                    .filter(s -> s.getLeaderId().equals(mob.getLeaderId()))
                    .filter(s -> s.getKeyBlockPos().distManhattan(homePos) <= need.getSearchRadius())
                    .anyMatch(s -> isRunningIfRequired(level, s));

            if (found) {
                super.satisfy(mob);
                return true;
            }

            return fail(mob);
        }
    }

    private boolean isRunningIfRequired(ServerLevel level, Structure structure) {
        return !need.isRequiresRunning() || JobUtil.isStructureRunning(level, structure, need.getRunningWorkSteps());
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        mob.getBrain().setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), need.getRequiredStructure());
    }
}
