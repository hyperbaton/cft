package com.hyperbaton.cft.structure;

import com.hyperbaton.cft.util.RegistryEntries;
import com.hyperbaton.cft.CftRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public abstract class StructureType {

    public static final Codec<StructureType> STRUCTURE_TYPE_CODEC = Codec.lazyInitialized(
            () -> CftRegistry.STRUCTURE_TYPE_CODEC_REGISTRY.byNameCodec()
                    .dispatch("type", StructureType::structureTypeCodec, codec -> MapCodec.assumeMapUnsafe(codec))
    );

    /**
     * The fields every structure type has, read along with the type's own fields (they're in the
     * same JSON object). A structure type's codec includes them with {@link #propertiesCodec()} and
     * passes them to the {@link StructureType} constructor, as vanilla blocks do with their properties.
     *
     * @param keyBlock          the block the leader clicks with the staff to detect the structure
     * @param maxUsers          how many Xoonglins can use a structure at once; 0 for no limit
     * @param requiresContainer whether the structure must hold a container
     * @param priority          types with a higher priority are tried first on a shared key block
     */
    public record Properties(RegistryEntries<Block> keyBlock, int maxUsers, boolean requiresContainer, int priority) {

        /** The codec of the properties, with the given default for {@code max_users}. */
        public static MapCodec<Properties> mapCodec(int defaultMaxUsers) {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    RegistryEntries.codec(Registries.BLOCK).fieldOf("key_block").forGetter(Properties::keyBlock),
                    Codec.INT.optionalFieldOf("max_users", defaultMaxUsers).forGetter(Properties::maxUsers),
                    Codec.BOOL.optionalFieldOf("requires_container", false).forGetter(Properties::requiresContainer),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(Properties::priority)
            ).apply(instance, Properties::new));
        }
    }

    private static final MapCodec<Properties> PROPERTIES_CODEC = Properties.mapCodec(1);

    /** The fields every structure type has, for a type's codec; it takes a single field of the codec. */
    protected static <S extends StructureType> RecordCodecBuilder<S, Properties> propertiesCodec() {
        return PROPERTIES_CODEC.forGetter(StructureType::getProperties);
    }

    /** {@link #propertiesCodec()}, for a type whose structures usually have no user limit, or another default. */
    protected static <S extends StructureType> RecordCodecBuilder<S, Properties> propertiesCodec(int defaultMaxUsers) {
        return Properties.mapCodec(defaultMaxUsers).forGetter(StructureType::getProperties);
    }

    private final Properties properties;

    protected StructureType(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    /**
     * Runs this type's detector at the key block. Implemented per type so it can pass
     * {@code this} (the concrete type) to a stateless detector dedicated for each type
     */
    public abstract StructureDetectionResult detect(BlockPos keyBlockPos, ServerLevel level, UUID leaderId);

    public abstract Codec<? extends StructureType> structureTypeCodec();

    public boolean matchesKeyBlock(BlockState state) {
        return properties.keyBlock().contains(state.getBlockHolder());
    }

    public boolean isRequiresContainer() {
        return properties.requiresContainer();
    }

    public RegistryEntries<Block> getKeyBlock() {
        return properties.keyBlock();
    }

    public int getMaxUsers() {
        return properties.maxUsers();
    }

    public int getPriority() {
        return properties.priority();
    }
}
