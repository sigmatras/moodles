package com.moodles;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class anim {

    private static final long DURATION_MS = 350L;
    private static final Map<Holder<MobEffect>, EffectState> EFFECT_STATES = new HashMap<>();

    private record EffectState(long startTime, int amplifier) {}

    public record AnimState(float scale, float yOffset, float alpha) {}

    public static void updateActiveEffects(Collection<MobEffectInstance> activeEffects, long now) {
        Set<Holder<MobEffect>> activeHolders = activeEffects.stream()
                .map(MobEffectInstance::getEffect)
                .collect(Collectors.toSet());

        EFFECT_STATES.keySet().removeIf(holder -> !activeHolders.contains(holder));

        for (MobEffectInstance effect : activeEffects) {
            Holder<MobEffect> holder = effect.getEffect();
            EffectState existing = EFFECT_STATES.get(holder);
            if (existing == null) {
                EFFECT_STATES.put(holder, new EffectState(now, effect.getAmplifier()));
            } else if (existing.amplifier() != effect.getAmplifier()) {
                EFFECT_STATES.put(holder, new EffectState(now, effect.getAmplifier()));
            }
        }
    }

    public static AnimState getAnimState(MobEffectInstance effect, float baseHeight, long now) {
        Holder<MobEffect> holder = effect.getEffect();
        EffectState state = EFFECT_STATES.get(holder);

        long startTime = (state != null) ? state.startTime() : now;
        long elapsed = Math.max(0L, now - startTime);

        if (elapsed >= DURATION_MS) {
            return new AnimState(1.0f, 0.0f, 1.0f);
        }

        double progress = Math.min(1.0, (double) elapsed / DURATION_MS);
        float eased = (float) easeExpoOut(progress);

        float scale = 1.5f + (1.0f - 1.5f) * eased;
        float yOffset = -(baseHeight * 1.1f) * (1.0f - eased);
        float alpha = eased;

        return new AnimState(scale, yOffset, alpha);
    }

    private static double easeExpoOut(double x) {
        return x == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * x);
    }
}