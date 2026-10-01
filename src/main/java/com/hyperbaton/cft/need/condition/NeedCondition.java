package com.hyperbaton.cft.need.condition;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.entity.custom.XoonglinEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * When a need applies, for its {@code active_when} list: while any of its conditions doesn't hold,
 * the need isn't checked at all (it doesn't wear off, nor give or take happiness). Condition types
 * are registered like need types, so addons can add their own, for example one built on another
 * mod's seasons.
 */
public interface NeedCondition {

    Codec<NeedCondition> CODEC = Codec.lazyInitialized(() -> CftRegistry.NEED_CONDITIONS_CODEC_REGISTRY.byNameCodec()
            .dispatch("type", NeedCondition::conditionType, codec -> MapCodec.assumeMapUnsafe(codec)));

    /** Whether the condition holds for this Xoonglin right now. Only called on the server. */
    boolean test(XoonglinEntity xoonglin);

    /** The codec of this condition's type, as registered in {@link CftRegistry#NEED_CONDITIONS_CODEC}. */
    Codec<? extends NeedCondition> conditionType();
}
