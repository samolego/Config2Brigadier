package org.samo_lego.config2brigadier.sponge;

import com.google.inject.Inject;
import org.samo_lego.config2brigadier.Config2Brigadier;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import static org.samo_lego.config2brigadier.util.TranslatedText.SERVER_TRANSLATIONS_LOADED;

@Plugin(Config2Brigadier.MOD_ID)
public class C2BSponge {

    @Inject
    C2BSponge() {
    }

    @Listener
    public void onConstructPlugin(final ConstructPluginEvent event) {
        // Perform any one-time setup
        SERVER_TRANSLATIONS_LOADED = false;
        Config2Brigadier.init();
    }
}
