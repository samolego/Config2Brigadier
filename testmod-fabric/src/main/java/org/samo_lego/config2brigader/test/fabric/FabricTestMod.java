package org.samo_lego.config2brigader.test.fabric;


import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;

import java.io.File;
import java.io.IOException;

import static net.minecraft.commands.Commands.literal;

public class FabricTestMod implements ModInitializer {
    public static final String MOD_ID = "config2brigadier";
    public static SimpleConfig config = new SimpleConfig();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(FabricTestMod::registerSimpleCommand);
        TomlWriter tomlWriter = new TomlWriter();
        try {
            tomlWriter.write(config, new File("./config/c2ftest.toml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String write = tomlWriter.write(config);
        System.out.println("-------------TOML-------------------");
        System.out.println(write);
        System.out.println("------------------------------------");
        Toml read = new Toml().read(write);
        config = read.to(SimpleConfig.class);
    }

    private static void registerSimpleCommand(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(literal(MOD_ID));
        LiteralCommandNode<CommandSourceStack> editConfig = literal("editConfig").build();

        config.generateCommand(editConfig);

        root.addChild(editConfig);

        assert editConfig.getChild("activationRange") != null;
        assert editConfig.getChild("message") != null;
        assert editConfig.getChild("show") != null;
        assert editConfig.getChild("nested").getChild("nestedMessage") != null;
        assert editConfig.getChild("randomQuestions") != null;
    }
}
