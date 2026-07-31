package com.moodles;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

public class texture_manager implements ResourceManagerReloadListener {
    public static final Identifier ORIGINAL_LOCATION = Identifier.fromNamespaceAndPath(moodles.MODID, "gui/moodle.png");
    public static final Identifier OUTLINED_ICONS_LOCATION = Identifier.fromNamespaceAndPath(moodles.MODID, "gui/moodle_outlined.png");
    public static final Identifier HUE_SHIFTED_90_LOCATION = Identifier.fromNamespaceAndPath(moodles.MODID, "gui/moodle_hue_90.png");
    public static final Identifier HUE_SHIFTED_40_LOCATION = Identifier.fromNamespaceAndPath(moodles.MODID, "gui/moodle_hue_40.png");

    private static final int SLOT_SIZE = 22;
    private static final int ICON_SLOT_COUNT = 38;

    private static boolean loaded = false;
    private static int textureWidth = 1034;
    private static int textureHeight = 22;

    public static void ensureLoaded(Minecraft mc) {
        if (!loaded) {
            loadTextures(mc.getResourceManager(), mc);
        }
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        loadTextures(resourceManager, Minecraft.getInstance());
    }

    public static int calculateOutlineThickness(int baseThickness) {
        return Math.max(1, (int) Math.floor(baseThickness * 0.5));
    }

    private static synchronized void loadTextures(ResourceManager resourceManager, Minecraft mc) {
        Identifier[] possiblePaths = new Identifier[]{
                Identifier.fromNamespaceAndPath(moodles.MODID, "gui/moodle.png"),
                Identifier.fromNamespaceAndPath(moodles.MODID, "textures/gui/moodle.png")
        };

        InputStream inputStream = null;
        for (Identifier loc : possiblePaths) {
            var resourceOpt = resourceManager.getResource(loc);
            if (resourceOpt.isPresent()) {
                try {
                    inputStream = resourceOpt.get().open();
                    break;
                } catch (Exception ignored) {}
            }
        }

        if (inputStream == null) return;

        try (InputStream stream = inputStream) {
            NativeImage original = NativeImage.read(stream);

            textureWidth = original.getWidth();
            textureHeight = original.getHeight();

            NativeImage hueShifted90 = new NativeImage(NativeImage.Format.RGBA, textureWidth, textureHeight, false);
            NativeImage hueShifted40 = new NativeImage(NativeImage.Format.RGBA, textureWidth, textureHeight, false);
            NativeImage outlinedImg = new NativeImage(NativeImage.Format.RGBA, textureWidth, textureHeight, false);

            for (int y = 0; y < textureHeight; y++) {
                for (int x = 0; x < textureWidth; x++) {
                    int pixelABGR = original.getPixel(x, y);
                    hueShifted90.setPixel(x, y, shiftHueABGR(pixelABGR, -90.0f));
                    hueShifted40.setPixel(x, y, shiftHueABGR(pixelABGR, -40.0f));
                    outlinedImg.setPixel(x, y, pixelABGR);
                }
            }

            generateIconOutlines(original, outlinedImg, 1);

            mc.getTextureManager().register(HUE_SHIFTED_90_LOCATION, new DynamicTexture(() -> "moodle_hue_90", hueShifted90));
            mc.getTextureManager().register(HUE_SHIFTED_40_LOCATION, new DynamicTexture(() -> "moodle_hue_40", hueShifted40));
            mc.getTextureManager().register(OUTLINED_ICONS_LOCATION, new DynamicTexture(() -> "moodle_outlined", outlinedImg));
            mc.getTextureManager().register(ORIGINAL_LOCATION, new DynamicTexture(() -> "moodle_original", original));

            loaded = true;
        } catch (Exception ignored) {
        }
    }

    private static void generateIconOutlines(NativeImage src, NativeImage dest, int baseThickness) {
        int effectiveThickness = calculateOutlineThickness(baseThickness);

        for (int slot = 0; slot < ICON_SLOT_COUNT; slot++) {
            int startX = slot * SLOT_SIZE;
            int endX = startX + SLOT_SIZE;

            for (int y = 0; y < SLOT_SIZE; y++) {
                for (int x = startX; x < endX; x++) {
                    int currentABGR = src.getPixel(x, y);
                    int currentAlpha = (currentABGR >> 24) & 0xFF;

                    if (currentAlpha < 30) {
                        int bestNeighborABGR = 0;
                        int maxNeighborAlpha = 0;

                        for (int dy = -effectiveThickness; dy <= effectiveThickness; dy++) {
                            for (int dx = -effectiveThickness; dx <= effectiveThickness; dx++) {
                                if (dx == 0 && dy == 0) continue;

                                int nx = x + dx;
                                int ny = y + dy;

                                if (nx >= startX && nx < endX && ny >= 0 && ny < SLOT_SIZE) {
                                    int neighborABGR = src.getPixel(nx, ny);
                                    int neighborAlpha = (neighborABGR >> 24) & 0xFF;

                                    if (neighborAlpha > 100 && neighborAlpha > maxNeighborAlpha) {
                                        maxNeighborAlpha = neighborAlpha;
                                        bestNeighborABGR = neighborABGR;
                                    }
                                }
                            }
                        }

                        if (maxNeighborAlpha > 0) {
                            int outlineABGR = createOutlinePixel(bestNeighborABGR);
                            dest.setPixel(x, y, outlineABGR);
                        }
                    }
                }
            }
        }
    }

    private static int createOutlinePixel(int neighborABGR) {
        int b = (neighborABGR >> 16) & 0xFF;
        int g = (neighborABGR >> 8) & 0xFF;
        int r = neighborABGR & 0xFF;

        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0f;
        float s = (max == 0f) ? 0f : (delta / max);
        float v = max;

        if (delta != 0f) {
            if (max == rf) {
                h = (gf - bf) / delta + (gf < bf ? 6f : 0f);
            } else if (max == gf) {
                h = (bf - rf) / delta + 2f;
            } else {
                h = (rf - gf) / delta + 4f;
            }
            h /= 6f;
        }

        float invertedS = 1.0f - s;
        float finalS = Math.clamp(s + (invertedS - s) * 0.65f, 0.05f, 0.95f);
        float finalV = (v > 0.5f) ? (v * 0.35f) : Math.min(1.0f, v + 0.45f);

        float c = finalV * finalS;
        float x = c * (1.0f - Math.abs((h * 6.0f) % 2.0f - 1.0f));
        float m = finalV - c;

        float rNew = 0, gNew = 0, bNew = 0;
        int hCategory = (int) (h * 6.0f);
        switch (hCategory % 6) {
            case 0 -> { rNew = c; gNew = x; bNew = 0; }
            case 1 -> { rNew = x; gNew = c; bNew = 0; }
            case 2 -> { rNew = 0; gNew = c; bNew = x; }
            case 3 -> { rNew = 0; gNew = x; bNew = c; }
            case 4 -> { rNew = x; gNew = 0; bNew = c; }
            case 5 -> { rNew = c; gNew = 0; bNew = x; }
        }

        int rOut = Math.round((rNew + m) * 255.0f);
        int gOut = Math.round((gNew + m) * 255.0f);
        int bOut = Math.round((bNew + m) * 255.0f);

        int outlineAlpha = 80;

        return (outlineAlpha << 24) | (bOut << 16) | (gOut << 8) | rOut;
    }

    public static int getTextureWidth() { return textureWidth; }
    public static int getTextureHeight() { return textureHeight; }

    private static int shiftHueABGR(int abgr, float degrees) {
        int a = (abgr >> 24) & 0xFF;
        if (a == 0) return 0;

        int b = (abgr >> 16) & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int r = abgr & 0xFF;

        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0f;
        float s = (max == 0f) ? 0f : (delta / max);
        float v = max;

        if (delta != 0f) {
            if (max == rf) {
                h = (gf - bf) / delta + (gf < bf ? 6f : 0f);
            } else if (max == gf) {
                h = (bf - rf) / delta + 2f;
            } else {
                h = (rf - gf) / delta + 4f;
            }
            h /= 6f;
        }

        h = (h + (degrees / 360.0f)) % 1.0f;
        if (h < 0) h += 1.0f;

        float c = v * s;
        float x = c * (1.0f - Math.abs((h * 6.0f) % 2.0f - 1.0f));
        float m = v - c;

        float rNew = 0, gNew = 0, bNew = 0;
        int hCategory = (int) (h * 6.0f);
        switch (hCategory % 6) {
            case 0 -> { rNew = c; gNew = x; bNew = 0; }
            case 1 -> { rNew = x; gNew = c; bNew = 0; }
            case 2 -> { rNew = 0; gNew = c; bNew = x; }
            case 3 -> { rNew = 0; gNew = x; bNew = c; }
            case 4 -> { rNew = x; gNew = 0; bNew = c; }
            case 5 -> { rNew = c; gNew = 0; bNew = x; }
        }

        int rOut = Math.round((rNew + m) * 255.0f);
        int gOut = Math.round((gNew + m) * 255.0f);
        int bOut = Math.round((bNew + m) * 255.0f);

        return (a << 24) | (bOut << 16) | (gOut << 8) | rOut;
    }
}