package org.samo_lego.config2brigadier;

public class Config2Brigadier {
    public static final String MOD_ID = "config2brigadier";

    public static void init() {
        System.out.println(ExampleExpectPlatform.getConfigDirectory().toAbsolutePath().normalize().toString());
    }
}
