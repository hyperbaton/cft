package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.StructureNeed;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

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
            BlockPos assignedPos = mob.getAssignedStructurePos(need.getStructureType());
            if (assignedPos != null) {
                boolean structureExists = data.getStructures().stream()
                        .anyMatch(s -> s.getKeyBlockPos().equals(assignedPos)
                                && s.getStructureTypeId().equals(need.getStructureType())
                                && s.isUser(mob.getUUID()));
                if (structureExists) {
                    super.satisfy(mob);
                    return true;
                }
            }
            // Not assigned yet — trigger behavior to find and claim
            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            addMemoriesForSatisfaction(mob);
            return false;
        } else {
            // Only requires presence within search radius
            BlockPos homePos = mob.getHome() != null ? mob.getHome().getEntrance() : mob.blockPosition();
            boolean found = data.getStructures().stream()
                    .filter(s -> s.getStructureTypeId().equals(need.getStructureType()))
                    .filter(s -> s.getLeaderId().equals(mob.getLeaderId()))
                    .anyMatch(s -> s.getKeyBlockPos().distManhattan(homePos) <= need.getSearchRadius());

            if (found) {
                super.satisfy(mob);
                return true;
            }

            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            return false;
        }
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        mob.getBrain().setMemory(CftMemoryModuleType.STRUCTURE_NEEDED.get(), true);
    }
}
