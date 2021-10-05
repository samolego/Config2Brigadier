package org.samo_lego.config2brigadier.command;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import org.samo_lego.config2brigadier.util.ConfigFieldList;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiFunction;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.samo_lego.config2brigadier.command.CommandFeedback.*;
import static org.samo_lego.config2brigadier.util.ConfigFieldList.populateFields;

public class CommandGenerator {

    /**
     * Generates /rootNode edit command
     *
     * @param config config object containing all toggles and options.
     * @param root root command node, usually /modid
     * @param saveConfigFile a runnable function that saves the config, e.g. config.save();
     * @param commentPrefix prefix of fields that are comments (fields that start with this prefix will be excluded)
     *                      * leave empty to include all fields
     * @param excludedFields fields that will be excluded. Useful of you want to manually create nodes for editing those.
     * @param commentText function that generates text for certain field. See also {@link CommandFeedback#defaultFieldDescription(Object, Field, String)}.
     */
    static void generateEditCommand(Object config, LiteralCommandNode<CommandSourceStack> root, Runnable saveConfigFile, String commentPrefix, List<String> excludedFields, BiFunction<Object, Field, MutableComponent> commentText) {
        ConfigFieldList configFields = populateFields(null, config, commentPrefix, excludedFields);
        recursiveEditCommand(root, configFields, saveConfigFile, commentText);
    }

    /**
     * Generates a command tree for selected {@link ConfigFieldList} and attaches it
     * to {@link LiteralCommandNode<CommandSourceStack>}. As attributes have different
     * types and therefore should accept different paramters as values, this goes
     * through all needed primitives and then recursively repeats for nested {@link ConfigFieldList}s.
     */
    private static void recursiveEditCommand(LiteralCommandNode<CommandSourceStack> root, ConfigFieldList configFields, Runnable saveConfigFile, BiFunction<Object, Field, MutableComponent> commentText) {
        configFields.booleans().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", BoolArgumentType.bool())
                            .executes(context -> CommandFeedback.editConfigBoolean(context, configFields.parent(), attribute, saveConfigFile))
                    )
                    .executes(context -> printFieldDescription(context, configFields.parent(), attribute, commentText))
                    .build();
            root.addChild(node);
        });

        configFields.integers().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", IntegerArgumentType.integer())
                            .executes(context -> editConfigInt(context, configFields.parent(), attribute, saveConfigFile))
                    )
                    .executes(context -> printFieldDescription(context, configFields.parent(), attribute, commentText))
                    .build();
            root.addChild(node);
        });

        configFields.floats().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", FloatArgumentType.floatArg())
                            .executes(context -> editConfigFloat(context, configFields.parent(), attribute, saveConfigFile))
                    )
                    .executes(context -> printFieldDescription(context, configFields.parent(), attribute, commentText))
                    .build();
            root.addChild(node);
        });

        configFields.doubles().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", DoubleArgumentType.doubleArg())
                            .executes(context -> editConfigDouble(context, configFields.parent(), attribute, saveConfigFile))
                    )
                    .executes(context -> printFieldDescription(context, configFields.parent(), attribute, commentText))
                    .build();
            root.addChild(node);
        });

        configFields.strings().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", StringArgumentType.greedyString())
                            .executes(context -> editConfigObject(context, configFields.parent(), attribute, saveConfigFile))
                    )
                    .executes(context -> printFieldDescription(context, configFields.parent(), attribute, commentText))
                    .build();
            root.addChild(node);
        });

        configFields.nestedFields().forEach(generator -> {
            Field parentField = generator.parentField();

            String nodeName;
            // Root node doesn't have a name
            if(parentField == null)
                nodeName = root.getName();
            else
                nodeName = parentField.getName();

            LiteralCommandNode<CommandSourceStack> node = literal(nodeName)
                    .executes(context -> {
                        if(parentField != null)
                            return printFieldDescription(context, configFields.parent(), parentField, commentText);

                        // Root node cannot be executed
                        return -1;
                    })
                    .build();
            recursiveEditCommand(node, generator, saveConfigFile, commentText);
            root.addChild(node);
        });
    }
}
