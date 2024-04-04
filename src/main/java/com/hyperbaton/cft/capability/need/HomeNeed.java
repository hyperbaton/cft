package com.hyperbaton.cft.capability.need;

import com.hyperbaton.cft.CftRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public class HomeNeed extends Need{

    public static final Codec<HomeNeed> HOME_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(HomeNeed::getId),
            Codec.DOUBLE.fieldOf("damage").forGetter(HomeNeed::getDamage),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(HomeNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(HomeNeed::getSatisfactionThreshold),
            Codec.DOUBLE.fieldOf("frequency").forGetter(HomeNeed::getFrequency),
            HomeValidBlock.HOME_VALID_BLOCK_CODEC.listOf().fieldOf("floorBlocks").forGetter(HomeNeed::getFloorBlocks),
            HomeValidBlock.HOME_VALID_BLOCK_CODEC.listOf().fieldOf("wallBlocks").forGetter(HomeNeed::getWallBlocks),
            HomeValidBlock.HOME_VALID_BLOCK_CODEC.listOf().fieldOf("interiorBlocks").forGetter(HomeNeed::getInteriorBlocks),
            HomeValidBlock.HOME_VALID_BLOCK_CODEC.listOf().fieldOf("roofBlocks").forGetter(HomeNeed::getRoofBlocks)
    ).apply(instance, HomeNeed::new));

    List<HomeValidBlock> floorBlocks;
    List<HomeValidBlock> wallBlocks;
    List<HomeValidBlock> interiorBlocks;
    List<HomeValidBlock> roofBlocks;
    public HomeNeed(String id, double damage, double providedHappiness, double satisfactionThreshold, double frequency, List<HomeValidBlock> floorBlocks, List<HomeValidBlock> wallBlocks, List<HomeValidBlock> interiorBlocks, List<HomeValidBlock> roofBlocks) {
        super(id, damage, providedHappiness, satisfactionThreshold, frequency);
        this.floorBlocks = floorBlocks;
        this.wallBlocks = wallBlocks;
        this.interiorBlocks = interiorBlocks;
        this.roofBlocks = roofBlocks;
    }

    public List<HomeValidBlock> getFloorBlocks() {
        return floorBlocks;
    }

    public void setFloorBlocks(List<HomeValidBlock> floorBlocks) {
        this.floorBlocks = floorBlocks;
    }

    public List<HomeValidBlock> getWallBlocks() {
        return wallBlocks;
    }

    public void setWallBlocks(List<HomeValidBlock> wallBlocks) {
        this.wallBlocks = wallBlocks;
    }

    public List<HomeValidBlock> getInteriorBlocks() {
        return interiorBlocks;
    }

    public void setInteriorBlocks(List<HomeValidBlock> interiorBlocks) {
        this.interiorBlocks = interiorBlocks;
    }

    public List<HomeValidBlock> getRoofBlocks() {
        return roofBlocks;
    }

    public void setRoofBlocks(List<HomeValidBlock> roofBlocks) {
        this.roofBlocks = roofBlocks;
    }

    @Override
    public Codec<?extends Need> needType() {
        return CftRegistry.HOME_NEED.get();
    }
}