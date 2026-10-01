package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.HomeNeed;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.structure.StructureUtils;
import com.hyperbaton.cft.structure.home.HouseStructure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class HomeNeedSatisfier extends NeedSatisfier<HomeNeed> {
    public HomeNeedSatisfier(double satisfaction, boolean isSatisfied, HomeNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    /** Checks the home is still standing; if it isn't (or was unregistered), the Xoonglin loses it. */
    @Override
    public boolean satisfy(XoonglinEntity mob) {
        HouseStructure home = mob.getHome();
        if (home != null) {
            Optional<Structure> registered = StructureUtils.recheck((ServerLevel) mob.level(),
                    home.getEntrance(), home.getStructureTypeId());
            if (registered.isPresent()) {
                if (registered.get().getBlockPositions() != home.getBlockPositions()) {
                    // Its blocks changed (e.g. a new room): its copy of the home catches up
                    mob.setHome(HouseStructure.of(registered.get()));
                }
                super.satisfy(mob);
                return true;
            } else {
                mob.loseHome();
            }
        }

        return failAndSeek(mob);
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
}
