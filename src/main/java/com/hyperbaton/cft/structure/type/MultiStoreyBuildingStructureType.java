package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.StoreyRule;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.detector.MultiStoreyBuildingDetector;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A building made of stacked enclosed-building storeys. Each storey has its own
 * floor/wall/interior/roof requirements given by storey rules. Consecutive storeys
 * either share one layer of blocks (the lower storey's ceiling is the upper one's
 * floor) or sit exactly one block apart (separate ceiling and floor layers).
 */
public class MultiStoreyBuildingStructureType extends StructureType {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<MultiStoreyBuildingStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("id").forGetter(StructureType::getId),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("key_block").forGetter(s -> Optional.ofNullable(s.getKeyBlock())),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("key_block_tag").forGetter(s -> Optional.ofNullable(s.getKeyBlockTag())),
            Codec.INT.optionalFieldOf("max_users", 1).forGetter(StructureType::getMaxUsers),
            Codec.BOOL.optionalFieldOf("requires_container", false).forGetter(StructureType::isRequiresContainer),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(StructureType::getPriority),
            Codec.INT.fieldOf("min_storeys").forGetter(MultiStoreyBuildingStructureType::getMinStoreys),
            Codec.INT.fieldOf("max_storeys").forGetter(MultiStoreyBuildingStructureType::getMaxStoreys),
            StoreyRule.CODEC.listOf().fieldOf("storeyRules").forGetter(MultiStoreyBuildingStructureType::getStoreyRules)
    ).apply(inst, MultiStoreyBuildingStructureType::new));

    private final int minStoreys;
    private final int maxStoreys;
    private final List<StoreyRule> storeyRules;

    public MultiStoreyBuildingStructureType(String id, Optional<Block> keyBlock, Optional<TagKey<Block>> keyBlockTag,
                                            int maxUsers, boolean requiresContainer, int priority,
                                            int minStoreys, int maxStoreys, List<StoreyRule> storeyRules) {
        super(id, keyBlock, keyBlockTag, maxUsers, requiresContainer, priority);
        this.minStoreys = minStoreys;
        this.maxStoreys = maxStoreys;
        this.storeyRules = storeyRules;
        validateStoreyRules();
    }

    private void validateStoreyRules() {
        if (minStoreys < 1 || maxStoreys < minStoreys) {
            LOGGER.error("Multi-storey structure type '{}': invalid storey bounds min={} max={}",
                    getId(), minStoreys, maxStoreys);
            throw new IllegalArgumentException(
                    String.format("Multi-storey building '%s': invalid storey bounds min=%d max=%d",
                            getId(), minStoreys, maxStoreys));
        }
        for (int i = 0; i < storeyRules.size(); i++) {
            StoreyRule a = storeyRules.get(i);
            if (a.from() > a.to()) {
                LOGGER.error("Multi-storey structure type '{}': storeyRule[{}] has from ({}) > to ({})",
                        getId(), i, a.from(), a.to());
                throw new IllegalArgumentException(
                        String.format("Multi-storey building '%s': storeyRule[%d] has from (%d) > to (%d)",
                                getId(), i, a.from(), a.to()));
            }
            for (int j = i + 1; j < storeyRules.size(); j++) {
                StoreyRule b = storeyRules.get(j);
                if (a.from() <= b.to() && b.from() <= a.to()) {
                    LOGGER.error("Multi-storey structure type '{}': storeyRules[{}] ({}-{}) and [{}] ({}-{}) overlap",
                            getId(), i, a.from(), a.to(), j, b.from(), b.to());
                    throw new IllegalArgumentException(
                            String.format("Multi-storey building '%s': storeyRules[%d] (%d-%d) and [%d] (%d-%d) overlap",
                                    getId(), i, a.from(), a.to(), j, b.from(), b.to()));
                }
            }
        }
        for (int storey = 1; storey <= maxStoreys; storey++) {
            if (getRuleForStorey(storey) == null) {
                LOGGER.error("Multi-storey structure type '{}': no storeyRule covers storey {}", getId(), storey);
                throw new IllegalArgumentException(
                        String.format("Multi-storey building '%s': no storeyRule covers storey %d", getId(), storey));
            }
        }
    }

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId) {
        return new MultiStoreyBuildingDetector().detect(keyBlockPos, level, leaderId, this);
    }

    @Override
    public Codec<? extends StructureType> structureTypeCodec() {
        return CftRegistry.MULTI_STOREY_BUILDING_STRUCTURE_TYPE.get();
    }

    public int getMinStoreys() {
        return minStoreys;
    }

    public int getMaxStoreys() {
        return maxStoreys;
    }

    public List<StoreyRule> getStoreyRules() {
        return storeyRules;
    }

    public StoreyRule getRuleForStorey(int storey) {
        for (StoreyRule rule : storeyRules) {
            if (rule.containsStorey(storey)) {
                return rule;
            }
        }
        return null;
    }
}
