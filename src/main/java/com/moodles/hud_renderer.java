package com.moodles;

import com.moodles.registry.EffectType;
import com.moodles.registry.MoodleDef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class hud_renderer {

    private static final int SLOT_SIZE = 22;

    public static void renderHud(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        render(guiGraphics);
    }

    public static void renderFromScreen(GuiGraphics guiGraphics) {
        render(guiGraphics);
    }

    private static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Collection<MobEffectInstance> rawEffects = mc.player.getActiveEffects();
        if (rawEffects.isEmpty()) return;

        long now = System.currentTimeMillis();
        anim.updateActiveEffects(rawEffects, now);
        texture_manager.ensureLoaded(mc);

        config.DisplayMode displayMode = config.DISPLAY_MODE.get();

        List<MobEffectInstance> sortedEffects = rawEffects.stream()
                .collect(Collectors.toMap(
                        e -> e.getEffect().value(),
                        e -> e,
                        (e1, e2) -> e1.getAmplifier() >= e2.getAmplifier() ? e1 : e2
                ))
                .values()
                .stream()
                .sorted((e1, e2) -> {
                    if (displayMode == config.DisplayMode.CAS_UNKNOWN) {
                        MoodleDef d1 = registry.getMoodleDef(e1);
                        MoodleDef d2 = registry.getMoodleDef(e2);
                        return Integer.compare(getOriginalOrder(d1.type()), getOriginalOrder(d2.type()));
                    } else {
                        return Boolean.compare(
                                registry.getMoodleDef(e1).scalesWithPotency(),
                                registry.getMoodleDef(e2).scalesWithPotency()
                        );
                    }
                })
                .toList();

        double guiScale = mc.getWindow().getGuiScale();
        int guiScaleInt = Math.max(1, (int) guiScale);
        int moodleScaleFactor = Math.max(1, guiScaleInt - 1);
        float renderScale = (float) moodleScaleFactor / (float) guiScaleInt;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int texWidth = texture_manager.getTextureWidth();
        int texHeight = texture_manager.getTextureHeight();

        int displaySize = 22;
        int padding = 4;
        int spacing = 2;
        int iconStep = displaySize + spacing;

        float scaledScreenHeight = screenHeight / renderScale;

        float rawHotbarLeft = (screenWidth - 182.0f) / 2.0f;
        float maxAllowedX = rawHotbarLeft / renderScale;

        int maxCols = Math.max(1, (int) Math.floor((maxAllowedX - padding - displaySize) / iconStep) + 1);

        double mouseX = mc.mouseHandler.getXVelocity() * (double) screenWidth / (double) mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.getYVelocity() * (double) screenHeight / (double) mc.getWindow().getScreenHeight();

        float scaledMouseX = (float) mouseX / renderScale;
        float scaledMouseY = (float) mouseY / renderScale;

        float chatYOffset = chat_moodle_offset.getYOffset(now);

        MobEffectInstance hoveredEffect = null;

        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        poseStack.scale(renderScale, renderScale);

        int col = 0;
        int row = 0;

        for (MobEffectInstance effect : sortedEffects) {
            MoodleDef def = registry.getMoodleDef(effect);

            int frameIndex = 0;
            int foregroundIndex = 0;
            Identifier frameTex = texture_manager.ORIGINAL_LOCATION;

            if (displayMode == config.DisplayMode.CAS_UNKNOWN) {
                switch (def.type()) {
                    case POSITIVE -> {
                        frameIndex = 43;
                        foregroundIndex = 48;
                        frameTex = texture_manager.ORIGINAL_LOCATION;
                    }
                    case NEGATIVE -> {
                        frameIndex = getPotencyFrameIndex(effect.getAmplifier());
                        foregroundIndex = 47;
                        frameTex = texture_manager.ORIGINAL_LOCATION;
                    }
                    case NEUTRAL -> {
                        frameIndex = getPotencyFrameIndex(effect.getAmplifier());
                        foregroundIndex = 47;
                        frameTex = texture_manager.HUE_SHIFTED_40_LOCATION;
                    }
                }
            } else {
                if (def.scalesWithPotency()) {
                    frameIndex = getPotencyFrameIndex(effect.getAmplifier());
                    foregroundIndex = 47;

                    if (effect.getAmplifier() > 0) {
                        if (def.type() == EffectType.POSITIVE) {
                            frameTex = texture_manager.HUE_SHIFTED_90_LOCATION;
                        } else if (def.type() == EffectType.NEUTRAL) {
                            frameTex = texture_manager.HUE_SHIFTED_40_LOCATION;
                        }
                    }
                } else {
                    switch (def.type()) {
                        case POSITIVE -> {
                            frameIndex = 43;
                            foregroundIndex = 48;
                        }
                        case NEGATIVE -> {
                            frameIndex = 42;
                            foregroundIndex = 48;
                        }
                        case NEUTRAL -> {
                            frameIndex = 44;
                            foregroundIndex = 48;
                        }
                    }
                }
            }

            int x = padding + col * iconStep;
            int baseY = (int) (scaledScreenHeight - padding - displaySize - row * iconStep);
            int y = baseY + (int) chatYOffset;

            if (scaledMouseX >= x && scaledMouseX < x + displaySize &&
                    scaledMouseY >= y && scaledMouseY < y + displaySize) {
                hoveredEffect = effect;
            }

            anim.AnimState anim = com.moodles.anim.getAnimState(effect, displaySize, now);

            float wiggleOffset = 0.0f;
            if (displayMode == config.DisplayMode.POTENCY && def.scalesWithPotency() && def.type() != EffectType.NEUTRAL && effect.getAmplifier() >= 4) {
                wiggleOffset = (float) Math.sin((now % 1500L) / 1500.0 * Math.PI * 2.0) * 1.5f;
            }

            poseStack.pushMatrix();

            float centerX = x + displaySize / 2.0f;
            float centerY = y + displaySize / 2.0f;
            poseStack.translate(centerX, centerY + anim.yOffset() + wiggleOffset);
            poseStack.scale(anim.scale(), anim.scale());
            poseStack.translate(-centerX, -centerY);

            int color = ARGB.colorFromFloat(anim.alpha(), 1.0f, 1.0f, 1.0f);

            guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    frameTex,
                    x, y,
                    (float) (frameIndex * SLOT_SIZE), 0.0f,
                    displaySize, displaySize,
                    texWidth, texHeight,
                    color
            );

            guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    texture_manager.OUTLINED_ICONS_LOCATION,
                    x, y,
                    (float) (def.iconIndex() * SLOT_SIZE), 0.0f,
                    displaySize, displaySize,
                    texWidth, texHeight,
                    color
            );

            guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    frameTex,
                    x, y,
                    (float) (foregroundIndex * SLOT_SIZE), 0.0f,
                    displaySize, displaySize,
                    texWidth, texHeight,
                    color
            );

            poseStack.popMatrix();

            overlay.renderOverlay(guiGraphics, effect, def, x, y, displaySize, now);

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }

        poseStack.popMatrix();

        if (hoveredEffect != null) {
            tooltip.render(guiGraphics, hoveredEffect, (int) mouseX, (int) mouseY, mc.font);
        }
    }

    private static int getPotencyFrameIndex(int amplifier) {
        if (amplifier <= 0) return 38;
        if (amplifier == 1) return 39;
        if (amplifier == 2) return 40;
        return 41;
    }

    private static int getOriginalOrder(EffectType type) {
        return switch (type) {
            case POSITIVE -> 0;
            case NEUTRAL  -> 1;
            case NEGATIVE -> 2;
        };
    }
}