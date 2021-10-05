package org.samo_lego.config2brigadier.fabric;

import org.samo_lego.config2brigadier.Config2Brigadier;
import net.fabricmc.api.ModInitializer;

public class ExampleModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Config2Brigadier.init();
    }
}
