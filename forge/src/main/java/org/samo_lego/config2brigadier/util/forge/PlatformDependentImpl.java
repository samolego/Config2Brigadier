package org.samo_lego.config2brigadier.util.forge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.TranslatableComponent;
import org.samo_lego.config2brigadier.Config2Brigadier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.apache.logging.log4j.LogManager.getLogger;

public class PlatformDependentImpl {
    private static final Gson gson = new GsonBuilder().create();
    private static JsonObject LANG;

    public static TranslatableComponent translatedComponent(String string, Object... objects) {
        return new TranslatableComponent(LANG.has(string) ? LANG.get(string).getAsString() : string, objects);
    }

    public static void setLang(String lang) {
        String langPath = String.format("/data/config2brigadier/lang/%s.json", lang);
        InputStream stream = Config2Brigadier.class.getResourceAsStream(langPath);

        try {
            if(stream == null) {
                stream = Config2Brigadier.class.getResourceAsStream("/data/config2brigadier/lang/en_us.json");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                LANG = gson.fromJson(reader, JsonObject.class);
            }
        } catch (IOException | NullPointerException e) {
            getLogger("Config2Brigadier").error("[Config2Brigadier]: Problem occurred when trying to load language: ", e);
            LANG = new JsonObject();
        }
    }
}
