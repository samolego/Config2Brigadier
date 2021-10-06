package org.samo_lego.config2brigadier.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.samo_lego.config2brigadier.Config2Brigadier;

import static org.samo_lego.config2brigadier.util.TranslatedText.SERVER_TRANSLATIONS_LOADED;

public class C2BFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SERVER_TRANSLATIONS_LOADED = FabricLoader.getInstance().isModLoaded("server_translations_api");
        Config2Brigadier.init();
    }
}
