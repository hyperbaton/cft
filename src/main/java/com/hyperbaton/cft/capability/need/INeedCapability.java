package com.hyperbaton.cft.capability.need;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class INeedCapability<T extends Need> {

    /**
     * A value between 0 and 1 about how much this need is currently satisfied
     */
    double satisfaction;

    /**
     * Quick access to know if the need is currently satisfied
     */
    boolean isSatisfied;

    T need;

    public INeedCapability(double satisfaction, boolean isSatisfied, T need) {
        this.satisfaction = satisfaction;
        this.isSatisfied = isSatisfied;
        this.need = need;
    }

    public void satisfy(double frequency) {
        satisfaction = Math.min(
                BigDecimal.valueOf(20)
                        .divide(BigDecimal.valueOf(24000 * frequency),
                                RoundingMode.HALF_UP)
                        .doubleValue(),
                1);
    }

    public void unsatisfy(double frequency) {
        satisfaction -= Math.max(
                BigDecimal.valueOf(20)
                        .divide(BigDecimal.valueOf(24000 * frequency),
                                RoundingMode.HALF_UP)
                        .doubleValue(),
                0);
    }

    public double getSatisfaction() {
        return satisfaction;
    }

    public void setSatisfaction(double satisfaction) {
        this.satisfaction = satisfaction;
    }

    public boolean isSatisfied() {
        return isSatisfied;
    }

    public void setSatisfied(boolean satisfied) {
        isSatisfied = satisfied;
    }

    public T getNeed() {
        return need;
    }

    public void setNeed(T need) {
        this.need = need;
    }
}
