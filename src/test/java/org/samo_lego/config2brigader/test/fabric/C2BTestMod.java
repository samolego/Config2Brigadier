package org.samo_lego.config2brigader.test.fabric;


import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.samo_lego.config2brigadier.common.IBrigadierConfigurator;

import java.io.File;
import java.io.IOException;

public class C2BTestMod implements ModInitializer {
    public static final String MOD_ID = "config2brigadier_test";
    public static SimpleConfig config = new SimpleConfig();

    @Override
    public void onInitialize() {
        config = readConfig();
        
        CommandRegistrationCallback.EVENT.register(C2BTestMod::registerConfigCommand);
    }


    /**
     * Generates the config edit command.
     * @param dispatcher Command dispatcher.
     * @param _commandBuildContext
     * @param _selection
     */
    private static void registerConfigCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext _commandBuildContext, Commands.CommandSelection _selection) {
        config.generateReloadableConfigCommand(MOD_ID, dispatcher, C2BTestMod::readConfig);
    }

    /**
     * Reads the config file.
     * @return SimpleConfig object.
     */
    private static SimpleConfig readConfig() {
        return IBrigadierConfigurator.loadConfigFile(new File("config/config2brigadier_test.json"), SimpleConfig.class, SimpleConfig::new);
    }
}
