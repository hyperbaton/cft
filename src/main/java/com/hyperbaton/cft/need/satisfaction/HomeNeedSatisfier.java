package com.hyperbaton.cft.need.satisfaction;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.need.HomeNeed;
import com.hyperbaton.cft.need.Need;
import com.hyperbaton.cft.socialclass.SocialClass;
import com.hyperbaton.cft.structure.home.HomeDetection;
import com.hyperbaton.cft.world.HomesData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public class HomeNeedSatisfier extends NeedSatisfier<HomeNeed> {
    public HomeNeedSatisfier(double satisfaction, boolean isSatisfied, HomeNeed need) {
        super(satisfaction, isSatisfied, need);
    }

    @Override
    public boolean satisfy(XoonglinEntity mob) {
        if (mob.getHome() != null &&
                HomeDetection.detectHouse(mob.getHome().getEntrance(),
                                (ServerLevel) mob.level(), mob.getLeaderId(),
                                getHomeNeedOfSocialClass(mob.getSocialClass()))
                        .isHomeDetected()) {
            super.satisfy(mob);
        } else {
            if (mob.getHome() != null) {
                if (!mob.level().isClientSide) {
                    HomesData homesData = ((ServerLevel) mob.level()).getDataStorage().computeIfAbsent(HomesData::load, HomesData::new, "homesData");
                    homesData.getHomes().removeIf(home -> home.getEntrance().equals(mob.getHome().getEntrance()));
                    homesData.setDirty();
                }

                mob.setHome(null);
                mob.getBrain().eraseMemory(CftMemoryModuleType.HOME_CONTAINER_POSITION.get());
            }
            this.unsatisfy(need.getFrequency(), mob);
            mob.decreaseHappiness(need.getProvidedHappiness(), need.getFrequency());
            addMemoriesForSatisfaction(mob);
            return false;
        }
        return true;
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

    /**
     * Given a home, return the HomeNeed that is expected to be satisfied by it.
     */
    private HomeNeed getHomeNeedOfSocialClass(SocialClass socialClass) {
        return (HomeNeed) socialClass.getNeeds().stream()
                .map(need -> CftRegistry.NEEDS.get(new ResourceLocation(need)))
                .filter(need1 -> need1 instanceof HomeNeed).findFirst().orElseThrow();
    }

}
