package com.hyperbaton.cft.capability.need;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ConsumeItemNeedCapability extends INeedCapability<GoodsNeed> {
    public ConsumeItemNeedCapability(double satisfaction, boolean isSatisfied, GoodsNeed need) {
        super(satisfaction, isSatisfied, need);
    }
}
