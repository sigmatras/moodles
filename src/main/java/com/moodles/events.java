package com.moodles;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class events {

    @EventBusSubscriber(modid = moodles.MODID, value = Dist.CLIENT)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(
                    VanillaGuiLayers.HOTBAR,
                    Identifier.fromNamespaceAndPath(moodles.MODID, "hud"),
                    (guiGraphics, deltaTracker) -> hud_renderer.renderHud(guiGraphics)
            );
        }

        @SubscribeEvent
        public static void registerReloadListeners(AddClientReloadListenersEvent event) {
            event.addListener(
                    Identifier.fromNamespaceAndPath(moodles.MODID, "texture_manager"),
                    new texture_manager()
            );
        }

    @EventBusSubscriber(modid = moodles.MODID, value = Dist.CLIENT)
    public static class GameBusEvents {

        @SubscribeEvent
        public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
            if (event.getName().equals(VanillaGuiLayers.EFFECTS)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onRenderInventoryMobEffects(ScreenEvent.RenderInventoryMobEffects event) {
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
            Screen screen = event.getScreen();
            if (!isExcludedScreen(screen)) {
                hud_renderer.renderFromScreen(event.getGuiGraphics());
            }
        }

        private static boolean isExcludedScreen(Screen screen) {
            return screen.isPauseScreen()
                    || screen instanceof TitleScreen
                    || screen instanceof OptionsScreen
                    || screen instanceof ConnectScreen
                    || screen instanceof LevelLoadingScreen;
        }}
    }
}