package com.hyperbaton.cft.network;

public class NeedSatisfactionData {
    public final double satisfaction;
    public final double damageThreshold;
    public final double satisfactionThreshold;

    public NeedSatisfactionData(double satisfaction, double damageThreshold, double satisfactionThreshold) {
        this.satisfaction = satisfaction;
        this.damageThreshold = damageThreshold;
        this.satisfactionThreshold = satisfactionThreshold;
    }

}
