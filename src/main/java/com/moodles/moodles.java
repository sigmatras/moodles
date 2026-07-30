package com.moodles;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(moodles.MODID)
public class moodles {
    public static final String MODID = "moodles";

    public moodles(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, config.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}