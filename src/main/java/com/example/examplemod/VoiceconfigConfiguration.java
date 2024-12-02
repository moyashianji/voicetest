package com.example.examplemod;

import net.minecraftforge.common.ForgeConfigSpec;

public class VoiceconfigConfiguration {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<Float> RANGE;
    static {
        BUILDER.push("Range");
        RANGE = BUILDER.comment("この数値は音声が聞こえる最大距離を示しています").define("range", (float) 32.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

}
