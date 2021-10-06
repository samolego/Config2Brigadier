package org.samo_lego.config2brigadier;

import org.samo_lego.config2brigadier.util.TranslatedText;

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.samo_lego.config2brigadier.util.TranslatedText.SERVER_TRANSLATIONS_LOADED;

public class Config2Brigadier {
    public static final String MOD_ID = "config2brigadier";

    public static void init() {
        getLogger(MOD_ID).info("Loaded C2B lib.");
        if(!SERVER_TRANSLATIONS_LOADED)
            TranslatedText.setLang("en_us");
    }
}
