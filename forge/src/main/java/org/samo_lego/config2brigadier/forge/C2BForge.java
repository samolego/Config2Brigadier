package org.samo_lego.config2brigadier.forge;

import net.minecraftforge.fml.common.Mod;
import org.samo_lego.config2brigadier.Config2Brigadier;

import static org.samo_lego.config2brigadier.util.TranslatedText.SERVER_TRANSLATIONS_LOADED;

@Mod(Config2Brigadier.MOD_ID)
public class C2BForge {
    public C2BForge() {
        SERVER_TRANSLATIONS_LOADED = false;
        Config2Brigadier.init();
    }
}
