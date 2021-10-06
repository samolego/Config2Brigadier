package org.samo_lego.config2brigadier.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.chat.TranslatableComponent;

public class PlatformDependent {
    @ExpectPlatform
    public static TranslatableComponent translatedComponent(String string, Object... objects) {
        throw new AssertionError();
    }
}
