package org.samo_lego.config2brigadier.util;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.samo_lego.config2brigadier.Config2Brigadier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.samo_lego.config2brigadier.Config2Brigadier.GSON;
import static org.samo_lego.config2brigadier.Config2Brigadier.MOD_ID;

public class TranslatedText extends TranslatableContents {

    private static JsonObject LANG = new JsonObject();
    public static boolean SERVER_TRANSLATIONS_LOADED;

    /**
     * Translates the text on server side or leaves it if server translations mod is loaded.
     * @param key text key
     * @param args arguments to include in text
     */
    public TranslatedText(String key, Object... args) {
        super(SERVER_TRANSLATIONS_LOADED ? key : (LANG.has(key) ? LANG.get(key).getAsString() : key), args);
    }

    /**
     * Sets the default language. Has no effect if server translations mod is loaded.
     * @param lang lang string
     */
    public static void setLang(String lang) {
        String langPath = String.format("/data/config2brigadier/lang/%s.json", lang);
        InputStream stream = Config2Brigadier.class.getResourceAsStream(langPath);

        try {
            if(stream == null) {
                getLogger(MOD_ID).warn("[Config2Brigadier] Language " + lang + " not available. Switching to en_us.");
                stream = Config2Brigadier.class.getResourceAsStream("/data/config2brigadier/lang/en_us.json");
            }
            assert stream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                LANG = GSON.fromJson(reader, JsonObject.class);
            }
        } catch (IOException | AssertionError e) {
            getLogger("Config2Brigadier").error("[Config2Brigadier] Problem occurred when trying to load language: ", e);
        }

        if(LANG == null) {
            LANG = new JsonObject();
        }
    }
}
