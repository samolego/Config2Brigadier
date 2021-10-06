package org.samo_lego.config2brigadier.forge;

import net.minecraftforge.fml.common.Mod;
import org.samo_lego.config2brigadier.Config2Brigadier;
import org.samo_lego.config2brigadier.util.forge.PlatformDependentImpl;

@Mod(Config2Brigadier.MOD_ID)
public class Config2BrigadierForge {
    public Config2BrigadierForge() {
        PlatformDependentImpl.setLang("en_us");
        Config2Brigadier.init();
    }
}
