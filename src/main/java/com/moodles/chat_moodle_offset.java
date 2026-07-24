package com.moodles;

import net.minecraft.client.Minecraft;

public class chat_moodle_offset {
    private static long animStartTime = 0L;
    private static boolean wasChatOpen = false;
    private static final float CHAT_OFFSET_Y = -20.0f;
    private static final long DURATION_MS = 200L;

    public static float getYOffset(long now) {
        Minecraft mc = Minecraft.getInstance();
        boolean chatOpen = mc.gui.getChat().isChatFocused();

        if (chatOpen != wasChatOpen) {
            wasChatOpen = chatOpen;
            animStartTime = now;
        }

        long elapsed = now - animStartTime;
        float rawProgress = Math.clamp((float) elapsed / DURATION_MS, 0.0f, 1.0f);

        float mappedProgress = Math.min(1.0f, rawProgress / 0.8f);

        float eased = easeOutQuad(mappedProgress);

        return chatOpen ? (eased * CHAT_OFFSET_Y) : ((1.0f - eased) * CHAT_OFFSET_Y);
    }

    private static float easeOutQuad(float x) {
        return 1.0f - (1.0f - x) * (1.0f - x);
    }
}