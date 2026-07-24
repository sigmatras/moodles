package com.moodles;

import com.moodles.registry.EffectType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.Map;

public class overlay {

    public static final ResourceLocation OVERLAY_LOCATION = ResourceLocation.fromNamespaceAndPath(moodles.MODID, "textures/gui/moodle_overlay.png");

    private static final int TEX_WIDTH = 22;
    private static final int TEX_HEIGHT = 48;
    private static final long FADE_DURATION_MS = 500L;
    private static final long PULSE_CYCLE_MS = 1200L;

    private static final Map<Holder<MobEffect>, Long> OVERLAY_START_TIMES = new HashMap<>();

    public static void renderOverlay(GuiGraphics guiGraphics, MobEffectInstance effect, registry.MoodleDef def, int x, int y, int size, long now) {
        Holder<MobEffect> holder = effect.getEffect();
        int amplifier = effect.getAmplifier();

        if (!def.scalesWithPotency() || def.type() == EffectType.NEUTRAL || amplifier < 4) {
            OVERLAY_START_TIMES.remove(holder);
            return;
        }

        long startTime = OVERLAY_START_TIMES.computeIfAbsent(holder, k -> now);
        long elapsed = Math.max(0L, now - startTime);

        double appearProgress = Math.clamp((double) elapsed / FADE_DURATION_MS, 0.0, 1.0);
        float appearFactor = (float) easeExpoOut(appearProgress);

        long pulseElapsed = now % PULSE_CYCLE_MS;
        double pulseProgress = (pulseElapsed < PULSE_CYCLE_MS / 2L)
                ? (double) pulseElapsed / (PULSE_CYCLE_MS / 2.0)
                : (double) (PULSE_CYCLE_MS - pulseElapsed) / (PULSE_CYCLE_MS / 2.0);

        float pulseFactor = (float) easeExpoOut(pulseProgress);
        float alpha = ((30.0f + (255.0f - 30.0f) * pulseFactor) / 255.0f) * appearFactor;

        int fullHeight = 48;
        int currentHeight = Math.max(1, Math.round(fullHeight * appearFactor));
        int renderY = y - 1 - currentHeight + 8;

        int renderWidth = size + 6;
        int renderX = x - 3;

        float vOffset = (1.0f - appearFactor) * TEX_HEIGHT;
        int vHeight = Math.max(1, Math.round(appearFactor * TEX_HEIGHT));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        guiGraphics.blit(
                OVERLAY_LOCATION,
                renderX, renderY,
                renderWidth, currentHeight,
                0.0f, vOffset,
                TEX_WIDTH, vHeight,
                TEX_WIDTH, TEX_HEIGHT
        );
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

    }

    private static double easeExpoOut(double x) {
        return x == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * x);
    }
}