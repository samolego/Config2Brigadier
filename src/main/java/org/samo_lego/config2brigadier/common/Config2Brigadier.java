package org.samo_lego.config2brigadier.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

public class Config2Brigadier {
    static final String MOD_ID = "config2brigadier";

    public static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
}
