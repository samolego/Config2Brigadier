package org.samo_lego.config2brigadier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.samo_lego.config2brigadier.util.TranslatedText;

public class Config2Brigadier {
    public static final Gson GSON = new GsonBuilder()
            .setLenient()
            .disableHtmlEscaping()
            .create();

    static {
        TranslatedText.setLang("en_us");
    }
}
