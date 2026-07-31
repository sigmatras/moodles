package com.moodles;

import com.moodles.registry.EffectType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.Map;

public class overlay {

    public static final Identifier OVERLAY_LOCATION = Identifier.fromNamespaceAndPath(moodles.MODID, "textures/gui/moodle_overlay.png");

    private static final int TEX_WIDTH = 22;
    private static final int TEX_HEIGHT = 48;
    private static final long FADE_DURATION_MS = 500L;
    private static final long PULSE_CYCLE_MS = 1200L;

    private static final Map<Holder<MobEffect>, Long> OVERLAY_START_TIMES = new HashMap<>();

    public static void renderOverlay(GuiGraphics guiGraphics, MobEffectInstance effect, registry.MoodleDef def, int x, int y, int size, long now) {
        Holder<MobEffect> holder = effect.getEffect();
        int amplifier = effect.getAmplifier();

        if (config.DISPLAY_MODE.get() != config.DisplayMode.POTENCY || !def.scalesWithPotency() || def.type() == EffectType.NEUTRAL || amplifier < 4) {
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

        int color = ARGB.colorFromFloat(alpha, 1.0f, 1.0f, 1.0f);
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                OVERLAY_LOCATION,
                renderX, renderY,
                0.0f, vOffset,
                renderWidth, currentHeight,
                TEX_WIDTH, vHeight,
                TEX_WIDTH, TEX_HEIGHT,
                color
        );
    }

    private static double easeExpoOut(double x) {
        return x == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * x);
    }
}