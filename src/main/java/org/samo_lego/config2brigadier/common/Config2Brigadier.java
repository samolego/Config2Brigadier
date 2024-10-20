package org.samo_lego.config2brigadier.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.apache.logging.log4j.LogManager.getLogger;

public class Config2Brigadier {
    public static final String MOD_ID = "config2brigadier";
    public static final Gson GSON = new GsonBuilder()
            .setLenient()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * Loads config file.
     *
     * @param file file to load the language file from.
     * @return config object
     */
    public static<T extends IBrigadierConfigurator> T loadConfigFile(File file, Class<T> clazz, Supplier<T> fallbackConstructor) {
        T config = null;
        if (file.exists()) {
            try (BufferedReader fileReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                config = GSON.fromJson(fileReader, clazz);
            } catch (IOException e) {
                getLogger(MOD_ID).error("[Config2Brigadier] Problem occurred when trying to load config: ", e);
            }
        }
        if (config == null) {
            config = fallbackConstructor.get();
        }

        config.writeToFile(file);

        return config;
    }
}
