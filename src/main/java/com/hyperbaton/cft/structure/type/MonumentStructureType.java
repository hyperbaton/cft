package com.hyperbaton.cft.structure.type;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.structure.IdenticalLayerGroup;
import com.hyperbaton.cft.structure.LayerRule;
import com.hyperbaton.cft.structure.StructureType;
import com.hyperbaton.cft.structure.ValidBlock;
import com.hyperbaton.cft.structure.StructureDetectionResult;
import com.hyperbaton.cft.structure.detector.MonumentDetector;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

public class MonumentStructureType extends StructureType {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<MonumentStructureType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            propertiesCodec(),
            Codec.INT.fieldOf("min_height").forGetter(MonumentStructureType::getMinHeight),
            Codec.INT.fieldOf("max_height").forGetter(MonumentStructureType::getMaxHeight),
            LayerRule.CODEC.listOf().fieldOf("layer_rules").forGetter(MonumentStructureType::getLayerRules),
            IdenticalLayerGroup.CODEC.listOf().optionalFieldOf("identical_layer_groups", List.of()).forGetter(MonumentStructureType::getIdenticalLayerGroups)
    ).apply(inst, MonumentStructureType::new));

    private final int minHeight;
    private final int maxHeight;
    private final List<LayerRule> layerRules;
    private final List<IdenticalLayerGroup> identicalLayerGroups;

    public MonumentStructureType(Properties properties, int minHeight, int maxHeight, List<LayerRule> layerRules,
                                 List<IdenticalLayerGroup> identicalLayerGroups) {
        super(properties);
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.layerRules = layerRules;
        this.identicalLayerGroups = identicalLayerGroups;
        validateLayerRules();
    }

    private void validateLayerRules() {
        for (int i = 0; i < layerRules.size(); i++) {
            LayerRule a = layerRules.get(i);
            if (a.from() > a.to()) {
                LOGGER.error("Monument structure type: layerRule[{}] has from ({}) > to ({})",
                        i, a.from(), a.to());
                throw new IllegalArgumentException(
                        String.format("Monument: layerRule[%d] has from (%d) > to (%d)",
                                i, a.from(), a.to()));
            }
            for (int j = i + 1; j < layerRules.size(); j++) {
                LayerRule b = layerRules.get(j);
                if (a.from() <= b.to() && b.from() <= a.to()) {
                    LOGGER.error("Monument structure type: layerRules[{}] ({}-{}) and [{}] ({}-{}) overlap",
                            i, a.from(), a.to(), j, b.from(), b.to());
                    throw new IllegalArgumentException(
                            String.format("Monument: layerRules[%d] (%d-%d) and [%d] (%d-%d) overlap",
                                    i, a.from(), a.to(), j, b.from(), b.to()));
                }
            }
        }
    }

    @Override
    public StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId) {
        return new MonumentDetector().detect(keyBlockPos, level, leaderId, this);
    }

    @Override
    public Codec<? extends StructureType> structureTypeCodec() {
        return CftRegistry.MONUMENT_STRUCTURE_TYPE.get();
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public List<LayerRule> getLayerRules() {
        return layerRules;
    }

    public List<IdenticalLayerGroup> getIdenticalLayerGroups() {
        return identicalLayerGroups;
    }

    public List<ValidBlock> getBlocksForLayer(int layer) {
        for (LayerRule rule : layerRules) {
            if (rule.containsLayer(layer)) {
                return rule.blocks();
            }
        }
        return null;
    }
}
