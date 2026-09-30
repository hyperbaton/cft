package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.ai.memory.CftMemoryModuleType;
import com.hyperbaton.cft.structure.Structure;
import com.hyperbaton.cft.world.StructuresData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfierMapper;
import com.hyperbaton.cft.need.satisfaction.ReadingNeedSatisfier;
import com.hyperbaton.cft.network.NeedSatisfactionData;
import com.hyperbaton.cft.socialclass.SocialClass;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class NeedUtils {
    /** Builds the network-facing snapshot of a Xoonglin's non-hidden needs. */
    public static Map<String, NeedSatisfactionData> buildNeedsData(XoonglinEntity xoonglin) {
        Map<String, NeedSatisfactionData> result = new HashMap<>();
        for (NeedSatisfier<? extends Need> satisfier : xoonglin.getNeeds()) {
            if (satisfier.getNeed().isHidden()) continue;

            String extraTooltip = null;
            if (satisfier instanceof ReadingNeedSatisfier readingSatisfier) {
                extraTooltip = readingSatisfier.wantedBookEntry(xoonglin)
                        .map(entry -> entry.title() + " by " + entry.authorName())
                        .orElse(null);
            }

            result.put(satisfier.getNeedId(), new NeedSatisfactionData(
                    satisfier.getSatisfaction(),
                    satisfier.getNeed().getDamageThreshold(),
                    satisfier.getNeed().getSatisfactionThreshold(),
                    satisfier.getNeed().getIcons(),
                    extraTooltip
            ));
        }
        return result;
    }

    /**
     * Where a Xoonglin can take supplies from for its goods, fluid and energy needs: its home
     * and, while it's visiting a structure that one of its visit needs lets it use
     * (use_supplies), that structure, which comes first.
     */
    public static List<BlockPos> supplySourcePositions(XoonglinEntity xoonglin) {
        List<BlockPos> positions = new ArrayList<>();
        visitedStructureWithSupplies(xoonglin).ifPresent(structure -> positions.addAll(structure.getAllBlockPositions()));
        if (xoonglin.getHome() != null) {
            positions.addAll(xoonglin.getHome().getInteriorBlocks());
        }
        return positions;
    }

    private static Optional<Structure> visitedStructureWithSupplies(XoonglinEntity xoonglin) {
        if (!(xoonglin.level() instanceof ServerLevel level) || xoonglin.getNeeds() == null) return Optional.empty();
        return xoonglin.getBrain().getMemory(CftMemoryModuleType.VISITING.get())
                .flatMap(keyPos -> level.getDataStorage().computeIfAbsent(StructuresData.factory(), "structuresData")
                        .findByKeyBlock(keyPos))
                .filter(structure -> xoonglin.getNeeds().stream()
                        .map(NeedSatisfier::getNeed)
                        .anyMatch(need -> need instanceof VisitNeed visitNeed
                                && visitNeed.isUseSupplies()
                                && visitNeed.getRequiredStructure().equals(structure.getStructureTypeId())));
    }

    public static List<NeedSatisfier<? extends Need>> getNeedsForClass(SocialClass socialClass) {
        List<NeedSatisfier<? extends Need>> satisfiers = new ArrayList<>();
        for (String needId : socialClass.getNeeds()) {
            Need need = CftRegistry.NEEDS.get(ResourceLocation.parse(needId));
            if (need != null) {
                satisfiers.add(NeedSatisfierMapper.createNeedSatisfier(needId, need));
            }
        }
        return satisfiers;
    }

    public static boolean classMeetsStructureType(SocialClass socialClass, String structureTypeId) {
        if (CftRegistry.NEEDS == null) return false;
        return socialClass.getNeeds().stream()
                .map(needId -> CftRegistry.NEEDS.get(ResourceLocation.parse(needId)))
                .filter(Objects::nonNull)
                .filter(need -> need instanceof HomeNeed)
                .map(need -> (HomeNeed) need)
                .anyMatch(homeNeed -> homeNeed.getRequiredStructure().equals(structureTypeId));
    }
}
