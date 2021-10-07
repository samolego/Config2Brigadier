package org.samo_lego.config2brigadier;

import org.samo_lego.config2brigadier.util.TranslatedText;

import static org.apache.logging.log4j.LogManager.getLogger;

public class Config2Brigadier {
    public static final String MOD_ID = "config2brigadier";

    public static void init() {
        getLogger(MOD_ID).info("Loaded C2B lib.");
        TranslatedText.setLang("en_us");
    }
}
