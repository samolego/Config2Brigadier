package org.samo_lego.config2brigadier.forge;

import dev.architectury.platform.forge.EventBuses;
import org.samo_lego.config2brigadier.Config2Brigadier;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Config2Brigadier.MOD_ID)
public class ExampleModForge {
    public ExampleModForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(Config2Brigadier.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        Config2Brigadier.init();
    }
}
