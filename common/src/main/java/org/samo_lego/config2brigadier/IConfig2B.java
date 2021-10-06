package org.samo_lego.config2brigadier;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import org.samo_lego.config2brigadier.command.CommandFeedback;
import org.samo_lego.config2brigadier.util.ConfigFieldList;

import java.lang.reflect.Field;
import java.util.function.BiFunction;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.samo_lego.config2brigadier.command.CommandFeedback.*;
import static org.samo_lego.config2brigadier.util.ConfigFieldList.populateFields;

/**
 * An interface your config should implement.
 */
public interface IConfig2B {

    /**
     * Method called after a value is edited.
     */
    void save();

    /**
     * Whether this field is a description field for other field (aka comment).
     * @param field field to check.
     * @return true if it is a description (comment) field, otherwise false.
     */
    boolean isDescription(Field field);

    /**
     * Indicates whether this field should be excluded from command.
     * @param field field to check.
     * @return true if it should not be included in command, otherwise false.
     */
    boolean shouldExclude(Field field);

    /**
     * Generates the command and attaches it to the provided node.
     * @param editNode node to attach fields to.
     */
    default void generateCommand(LiteralCommandNode<CommandSourceStack> editNode) {
        this.generateCommand(editNode, CommandFeedback::defaultFieldDescription);
    }

    /**
     * Generates the command and attaches it to the provided node.
     * @param editNode node to attach fields to.
     * @param commentText text that will be sent to player, describing the field.
     */
    default void generateCommand(LiteralCommandNode<CommandSourceStack> editNode, BiFunction<IConfig2B, Field, MutableComponent> commentText) {
        ConfigFieldList configFields = populateFields(null, this);
        recursiveEditCommand(editNode, configFields, commentText);
    }

    static void recursiveEditCommand(LiteralCommandNode<CommandSourceStack> root, ConfigFieldList configFields, BiFunction<IConfig2B, Field, MutableComponent> commentText) {
        configFields.booleans().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", BoolArgumentType.bool())
                            .executes(context -> CommandFeedback.editConfigBoolean(context, configFields.parent(), attribute))
                    )
                    .executes(context -> printFieldDescription(context, configFields.parent(), attribute, commentText))
                    .build();
            root.addChild(node);
        });

        configFields.integers().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", IntegerArgumentType.integer())
                            .executes(context -> editConfigInt(context, configFields.parent(), attribute))
                    )
                    .executes(context -> printFieldDescription(context, configFields.parent(), attribute, commentText))
                    .build();
            root.addChild(node);
        });

        configFields.floats().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", FloatArgumentType.floatArg())
                            .executes(context -> editConfigFloat(context, configFields.parent(), attribute))
                    )
                    .executes(context -> printFieldDescription(context, configFields.parent(), attribute, commentText))
                    .build();
            root.addChild(node);
        });

        configFields.doubles().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", DoubleArgumentType.doubleArg())
                            .executes(context -> editConfigDouble(context, configFields.parent(), attribute))
                    )
                    .executes(context -> printFieldDescription(context, configFields.parent(), attribute, commentText))
                    .build();
            root.addChild(node);
        });

        configFields.strings().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", StringArgumentType.greedyString())
                            .executes(context -> editConfigObject(context, configFields.parent(), attribute))
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
            recursiveEditCommand(node, generator, commentText);
            root.addChild(node);
        });
    }
}
