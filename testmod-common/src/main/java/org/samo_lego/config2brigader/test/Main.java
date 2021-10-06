package org.samo_lego.config2brigader.test;


import static org.apache.logging.log4j.LogManager.getLogger;

public class Main {
    public static final String MOD_ID = "config2brigadier";

    public static void init() {
        getLogger(MOD_ID).info("Loaded C2B lib testmod.");
    }
}
