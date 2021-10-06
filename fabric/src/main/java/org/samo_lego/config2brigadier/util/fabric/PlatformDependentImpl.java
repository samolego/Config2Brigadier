package org.samo_lego.config2brigadier.util.fabric;

import net.minecraft.network.chat.TranslatableComponent;

public class PlatformDependentImpl {
    public static TranslatableComponent translatedComponent(String string, Object... objects) {
        return new TranslatableComponent(string, objects);
    }
}
