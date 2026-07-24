package com.moodles;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

import java.util.List;

public class tooltip {

    private static Holder<MobEffect> lastHoveredEffect = null;
    private static long hoverStartTime = 0L;

    public static void render(GuiGraphics guiGraphics, MobEffectInstance effect, int mouseX, int mouseY, Font font) {
        long now = System.currentTimeMillis();
        Holder<MobEffect> currentHolder = effect.getEffect();

        if (!currentHolder.equals(lastHoveredEffect)) {
            lastHoveredEffect = currentHolder;
            hoverStartTime = now;
        }

        long elapsed = Math.max(0L, now - hoverStartTime);
        double progress = Math.min(1.0, (double) elapsed / 500.0);
        float scale = 0.5f + 0.5f * (float) easeExpoOut(progress);

        MutableComponent title = Component.translatable(effect.getEffect().value().getDescriptionId())
                .withStyle(ChatFormatting.BOLD);

        if (effect.getAmplifier() > 0) {
            title.append(" ").append(Component.translatable("potion.potency." + effect.getAmplifier()).withStyle(ChatFormatting.BOLD));
        }

        Minecraft mc = Minecraft.getInstance();
        float tps = (mc.level != null) ? mc.level.tickRateManager().tickrate() : 20.0F;
        Component durationComp = MobEffectUtil.formatDuration(effect, 1.0F, tps);

        Component header = Component.empty()
                .append(title)
                .append(Component.literal(" (").withStyle(ChatFormatting.RESET))
                .append(durationComp.copy().withStyle(ChatFormatting.RESET))
                .append(Component.literal(")").withStyle(ChatFormatting.RESET));

        Component descComp = Component.translatable(effect.getEffect().value().getDescriptionId() + ".description")
                .withStyle(ChatFormatting.GRAY);

        int maxDescWidth = 200;
        List<FormattedCharSequence> descLines = font.split(descComp, maxDescWidth);

        int maxTextWidth = font.width(header);
        for (FormattedCharSequence line : descLines) {
            maxTextWidth = Math.max(maxTextWidth, font.width(line));
        }

        int padding = 5;
        int lineSpacing = 3;

        int tooltipWidth = maxTextWidth + padding * 2;
        int tooltipHeight = padding * 2 + font.lineHeight;
        if (!descLines.isEmpty()) {
            tooltipHeight += lineSpacing + descLines.size() * font.lineHeight;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = mouseX + 10;
        int y = mouseY - 10;

        if (x + tooltipWidth > screenWidth) {
            x = mouseX - tooltipWidth - 5;
        }
        if (x < 2) {
            x = 2;
        }
        if (y + tooltipHeight > screenHeight) {
            y = screenHeight - tooltipHeight - 2;
        }
        if (y < 2) {
            y = 2;
        }

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 400);

        float anchorX = x;
        float anchorY = y + tooltipHeight;

        poseStack.translate(anchorX, anchorY, 0.0f);
        poseStack.scale(scale, scale, 1.0f);
        poseStack.translate(-anchorX, -anchorY, 0.0f);

        guiGraphics.fill(x, y, x + tooltipWidth, y + tooltipHeight, 0xE1000000);

        int textX = x + padding;
        int textY = y + padding;

        guiGraphics.drawString(font, header, textX, textY, 0xFFFFFFFF, true);

        int descY = textY + font.lineHeight + lineSpacing;
        for (FormattedCharSequence line : descLines) {
            guiGraphics.drawString(font, line, textX, descY, 0xAAAAAAAA, true);
            descY += font.lineHeight;
        }

        poseStack.popPose();
    }

    private static double easeExpoOut(double x) {
        return x == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * x);
    }
}