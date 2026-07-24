package com.moodles;

import com.moodles.registry.EffectType;
import com.moodles.registry.MoodleDef;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;
import java.util.Comparator;
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

        List<MobEffectInstance> sortedEffects = rawEffects.stream()
                .collect(Collectors.toMap(
                        e -> e.getEffect().value(),
                        e -> e,
                        (e1, e2) -> e1.getAmplifier() >= e2.getAmplifier() ? e1 : e2
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(e -> registry.getMoodleDef(e).scalesWithPotency()))
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

        double mouseX = mc.mouseHandler.xpos() * (double) screenWidth / (double) mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * (double) screenHeight / (double) mc.getWindow().getScreenHeight();

        float scaledMouseX = (float) mouseX / renderScale;
        float scaledMouseY = (float) mouseY / renderScale;

        float chatYOffset = chat_moodle_offset.getYOffset(now);

        MobEffectInstance hoveredEffect = null;

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(renderScale, renderScale, 1.0f);

        int col = 0;
        int row = 0;

        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (MobEffectInstance effect : sortedEffects) {
            MoodleDef def = registry.getMoodleDef(effect);

            int frameIndex = 0;

            if (def.scalesWithPotency()) {
                int amp = effect.getAmplifier();
                if (amp <= 0) {
                    frameIndex = 38;
                } else if (amp == 1) {
                    frameIndex = 39;
                } else if (amp == 2) {
                    frameIndex = 40;
                } else {
                    frameIndex = 41;
                }
            } else {
                switch (def.type()) {
                    case NEGATIVE -> frameIndex = 42;
                    case POSITIVE -> frameIndex = 43;
                    case NEUTRAL  -> frameIndex = 44;
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
            if (def.scalesWithPotency() && def.type() != EffectType.NEUTRAL && effect.getAmplifier() >= 4) {
                wiggleOffset = (float) Math.sin((now % 1500L) / 1500.0 * Math.PI * 2.0) * 1.5f;
            }

            poseStack.pushPose();

            float centerX = x + displaySize / 2.0f;
            float centerY = y + displaySize / 2.0f;
            poseStack.translate(centerX, centerY + anim.yOffset() + wiggleOffset, 0.0f);
            poseStack.scale(anim.scale(), anim.scale(), 1.0f);
            poseStack.translate(-centerX, -centerY, 0.0f);

            guiGraphics.setColor(1.0f, 1.0f, 1.0f, anim.alpha());

            ResourceLocation frameTex = texture_manager.ORIGINAL_LOCATION;
            if (def.scalesWithPotency() && effect.getAmplifier() > 0) {
                if (def.type() == EffectType.POSITIVE) {
                    frameTex = texture_manager.HUE_SHIFTED_90_LOCATION;
                } else if (def.type() == EffectType.NEUTRAL) {
                    frameTex = texture_manager.HUE_SHIFTED_40_LOCATION;
                }
            }

            guiGraphics.blit(
                    frameTex,
                    x, y,
                    displaySize, displaySize,
                    (float) (frameIndex * SLOT_SIZE), 0.0f,
                    SLOT_SIZE, SLOT_SIZE,
                    texWidth, texHeight
            );

            guiGraphics.blit(
                    texture_manager.OUTLINED_ICONS_LOCATION,
                    x, y,
                    displaySize, displaySize,
                    (float) (def.iconIndex() * SLOT_SIZE), 0.0f,
                    SLOT_SIZE, SLOT_SIZE,
                    texWidth, texHeight
            );

            int foregroundIndex = def.scalesWithPotency() ? 47 : 48;
            guiGraphics.blit(
                    frameTex,
                    x, y,
                    displaySize, displaySize,
                    (float) (foregroundIndex * SLOT_SIZE), 0.0f,
                    SLOT_SIZE, SLOT_SIZE,
                    texWidth, texHeight
            );

            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            poseStack.popPose();

            overlay.renderOverlay(guiGraphics, effect, def, x, y, displaySize, now);

            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }

        guiGraphics.flush();
        RenderSystem.disableBlend();

        poseStack.popPose();

        if (hoveredEffect != null) {
            tooltip.render(guiGraphics, hoveredEffect, (int) mouseX, (int) mouseY, mc.font);
        }
    }
}