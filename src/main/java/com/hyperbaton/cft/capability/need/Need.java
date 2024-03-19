package com.hyperbaton.cft.capability.need;

public abstract class Need {

    private String needType;
    private double damage;
    private double providedHappiness;

    public Need(String needType, double damage, double providedHappiness) {
        this.needType = needType;
        this.damage = damage;
        this.providedHappiness = providedHappiness;
    }

    public String getNeedType() {
        return needType;
    }

    public void setNeedType(String needType) {
        this.needType = needType;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getProvidedHappiness() {
        return providedHappiness;
    }

    public void setProvidedHappiness(double providedHappiness) {
        this.providedHappiness = providedHappiness;
    }
}
