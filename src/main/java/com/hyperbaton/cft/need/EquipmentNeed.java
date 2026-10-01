package com.hyperbaton.cft.need;

import com.hyperbaton.cft.CftRegistry;
import com.hyperbaton.cft.need.satisfaction.EquipmentNeedSatisfier;
import com.hyperbaton.cft.need.satisfaction.NeedSatisfier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.hyperbaton.cft.need.codec.CftCodec.INGREDIENT_CODEC;

public class EquipmentNeed extends Need {

    public static final Codec<EquipmentNeed> EQUIPMENT_NEED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("damage").forGetter(EquipmentNeed::getDamage),
            Codec.DOUBLE.fieldOf("damage_threshold").forGetter(EquipmentNeed::getDamageThreshold),
            Codec.DOUBLE.fieldOf("provided_happiness").forGetter(EquipmentNeed::getProvidedHappiness),
            Codec.DOUBLE.fieldOf("satisfaction_threshold").forGetter(EquipmentNeed::getSatisfactionThreshold),
            INGREDIENT_CODEC.fieldOf("item").forGetter(EquipmentNeed::getIngredient),
            Codec.DOUBLE.fieldOf("frequency").forGetter(EquipmentNeed::getFrequency),
            Codec.BOOL.optionalFieldOf("hidden", DEFAULT_HIDDEN).forGetter(EquipmentNeed::isHidden),
            Codec.BOOL.optionalFieldOf("bonus", DEFAULT_BONUS).forGetter(EquipmentNeed::isBonus),
            Codec.STRING.optionalFieldOf("slot", "mainhand").forGetter(EquipmentNeed::getSlotName),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(EquipmentNeed::getIcon)
    ).apply(instance, EquipmentNeed::new));

    private final Ingredient item;
    private final EquipmentSlot slot;
    private final String slotName;

    public EquipmentNeed(double damage, double damageThreshold, double providedHappiness,
                         double satisfactionThreshold, Ingredient item, double frequency, boolean hidden, boolean bonus,
                         String slotName, Optional<ResourceLocation> icon) {
        super(damage, damageThreshold, providedHappiness, satisfactionThreshold, frequency, hidden, bonus, icon);
        this.item = item;
        this.slotName = slotName;
        this.slot = parseSlot(slotName);
    }

    private static EquipmentSlot parseSlot(String name) {
        return switch (name) {
            case "offhand" -> EquipmentSlot.OFFHAND;
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    public Ingredient getIngredient() {
        return item;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public String getSlotName() {
        return slotName;
    }

    @Override
    public Codec<? extends Need> needType() {
        return CftRegistry.EQUIPMENT_NEED.get();
    }

    @Override
    public NeedSatisfier<EquipmentNeed> createSatisfier(double satisfaction, boolean isSatisfied) {
        return new EquipmentNeedSatisfier(satisfaction, isSatisfied, this);
    }

    @Override
    public String getTypeName() {
        return Component.translatable("gui.cft.need_type.equipment").getString();
    }

    @Override
    public List<ResourceLocation> getDefaultIcons() {
        return Arrays.stream(item.getItems())
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                .toList();
    }
}
