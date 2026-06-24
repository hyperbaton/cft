package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.HomeNeed;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;

public class HomeNeedSatisfier extends NeedSatisfier<HomeNeed> {
    public HomeNeedSatisfier(double satisfaction, boolean isSatisfied, HomeNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.getHome() != null) {
            String structureTypeId = mob.getHome().getStructureTypeId();
            StructureType structureType = findStructureType(structureTypeId);

            if (structureType != null) {
                var result = structureType.createDetector().detect(
                        mob.getHome().getEntrance(), (ServerLevel) mob.level(),
                        mob.getLeaderId(), structureType);
                if (result.success()) {
                    super.satisfy(mob);
                    return true;
                }
            }

            if (!mob.level().isClientSide) {
                StructuresData data = ((ServerLevel) mob.level()).getDataStorage()
                        .computeIfAbsent(StructuresData.factory(), "structuresData");
                data.getStructures().removeIf(s -> s.getKeyBlockPos().equals(mob.getHome().getEntrance()));
                data.setDirty();
            }

            mob.setHome(null);
            mob.getBrain().eraseMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get());
            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            addMemoriesForSatisfaction(mob);
            return false;
        } else {
            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            addMemoriesForSatisfaction(mob);
            return false;
        }
    }

    @Override
    public void addMemoriesForSatisfaction(XoonglinEntity mob) {
        mob.getBrain().setMemory(CftMemoryModuleType.HOME_NEEDED.get(), true);
    }

    public static NeedSatisfier<HomeNeed> fromTag(CompoundTag tag) {
        return new HomeNeedSatisfier(
                tag.getInt(TAG_SATISFACTION),
                tag.getBoolean(TAG_IS_SATISFIED),
                (HomeNeed) Need.NEED_CODEC.parse(NbtOps.INSTANCE, tag.getCompound(TAG_NEED)).result().orElse(null)
        );
    }

    private StructureType findStructureType(String structureTypeId) {
        if (CftRegistry.STRUCTURES == null) return null;
        return CftRegistry.STRUCTURES.stream()
                .filter(st -> st.getId().equals(structureTypeId))
                .findFirst()
                .orElse(null);
    }
}
