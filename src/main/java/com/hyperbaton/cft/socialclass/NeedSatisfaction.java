package com.hyperbaton.cft.socialclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class NeedSatisfaction {

public static final Codec<NeedSatisfaction> NEED_SATISFACTION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("need").forGetter(NeedSatisfaction::getNeed),
            Codec.DOUBLE.fieldOf("satisfactionThreshold").forGetter(NeedSatisfaction::getSatisfactionThreshold)
    ).apply(instance, NeedSatisfaction::new));

    // TODO: Make this a dynamic reference to a Need object
    private String need;
    /**
        A threshold for the satisfaction of the need. It may be an upper or lower threshold, referring to an upgrade or
     downgrade of the social class.
        Its value should be between 0 and 1
     **/
    private double satisfactionThreshold;

    public NeedSatisfaction(String need, double satisfactionThreshold) {
        this.need = need;
        this.satisfactionThreshold = satisfactionThreshold;
    }

    public String getNeed() {
        return need;
    }

    public void setNeed(String need) {
        this.need = need;
    }

    public double getSatisfactionThreshold() {
        return satisfactionThreshold;
    }

    public void setSatisfactionThreshold(double satisfactionThreshold) {
        this.satisfactionThreshold = satisfactionThreshold;
    }
}
