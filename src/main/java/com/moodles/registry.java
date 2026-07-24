package com.moodles;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.Map;

public class registry {

    public enum EffectType {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }

    public record MoodleDef(int iconIndex, boolean scalesWithPotency, EffectType type) {}

    private static final Map<ResourceLocation, MoodleDef> REGISTRY = new HashMap<>();

    static {
        register("absorption", 0, true, EffectType.POSITIVE);
        register("speed", 1, true, EffectType.POSITIVE);
        register("conduit_power", 2, true, EffectType.POSITIVE);
        register("dolphins_grace", 3, true, EffectType.POSITIVE);
        register("slowness", 4, true, EffectType.NEGATIVE);
        register("haste", 5, true, EffectType.POSITIVE);
        register("health_boost", 6, true, EffectType.POSITIVE);
        register("mining_fatigue", 7, true, EffectType.NEGATIVE);
        register("strength", 8, true, EffectType.POSITIVE);
        register("instant_health", 9, true, EffectType.POSITIVE);
        register("instant_damage", 10, true, EffectType.NEGATIVE);
        register("resistance", 11, true, EffectType.POSITIVE);
        register("hunger", 12, true, EffectType.NEGATIVE);
        register("saturation", 13, true, EffectType.POSITIVE);
        register("weakness", 14, true, EffectType.NEGATIVE);
        register("poison", 15, true, EffectType.NEGATIVE);
        register("wither", 16, true, EffectType.NEGATIVE);
        register("levitation", 17, true, EffectType.NEGATIVE);
        register("luck", 18, true, EffectType.POSITIVE);
        register("unluck", 19, true, EffectType.NEGATIVE);
        register("bad_omen", 20, true, EffectType.NEUTRAL);
        register("raid_omen", 21, true, EffectType.NEUTRAL);
        register("darkness", 22, false, EffectType.NEGATIVE);
        register("nausea", 23, false, EffectType.NEGATIVE);
        register("water_breathing", 24, false, EffectType.POSITIVE);
        register("invisibility", 25, false, EffectType.POSITIVE);
        register("blindness", 26, false, EffectType.NEGATIVE);
        register("night_vision", 27, false, EffectType.POSITIVE);
        register("glowing", 28, false, EffectType.NEUTRAL);
        register("slow_falling", 29, false, EffectType.POSITIVE);
        register("trial_omen", 30, false, EffectType.NEUTRAL);
        register("wind_charged", 31, false, EffectType.NEGATIVE);
        register("weaving", 32, false, EffectType.NEGATIVE);
        register("oozing", 33, false, EffectType.NEGATIVE);
        register("infested", 34, false, EffectType.NEGATIVE);
        register("breath_of_the_nautilus", 35, false, EffectType.POSITIVE);
        register("fire_resistance", 36, false, EffectType.POSITIVE);
        register("hero_of_the_village", 37, false, EffectType.POSITIVE);
        register("regeneration", 45, true, EffectType.POSITIVE);
        register("jump_boost", 46, true, EffectType.POSITIVE);
    }

    private static void register(String path, int iconIndex, boolean scalesWithPotency, EffectType type) {
        REGISTRY.put(ResourceLocation.fromNamespaceAndPath("minecraft", path),
                new MoodleDef(iconIndex, scalesWithPotency, type));
    }

    public static MoodleDef getMoodleDef(MobEffectInstance instance) {
        ResourceLocation key = instance.getEffect().unwrapKey().map(ResourceKey::location).orElse(null);
        if (key != null && REGISTRY.containsKey(key)) {
            return REGISTRY.get(key);
        }

        MobEffectCategory category = instance.getEffect().value().getCategory();
        EffectType type = switch (category) {
            case BENEFICIAL -> EffectType.POSITIVE;
            case HARMFUL -> EffectType.NEGATIVE;
            case NEUTRAL -> EffectType.NEUTRAL;
        };
        return new MoodleDef(0, true, type);
    }
}