package com.moodles;

import net.neoforged.neoforge.common.ModConfigSpec;

public class config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<DisplayMode> DISPLAY_MODE;

    public enum DisplayMode {
        POTENCY,
        CAS_UNKNOWN
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        DISPLAY_MODE = builder
                .defineEnum("displayMode", DisplayMode.POTENCY);

        SPEC = builder.build();
    }
}