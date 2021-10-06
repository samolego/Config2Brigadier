package org.samo_lego.config2brigadier;

import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.ApiStatus;
import org.samo_lego.config2brigadier.command.CommandFeedback;
import org.samo_lego.config2brigadier.util.ConfigFieldList;
import org.samo_lego.config2brigadier.util.TranslatedText;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
     *
     * @param field field to check.
     * @return true if it is a description (comment) field, otherwise false.
     */
    boolean isDescription(Field field);

    /**
     * Indicates whether this field should be excluded from command.
     *
     * @param field field to check.
     * @return true if it should not be included in command, otherwise false.
     */
    boolean shouldExclude(Field field);

    /**
     * Generates the command and attaches it to the provided node.
     *
     * @param editNode    node to attach fields to.
     */
    default void generateCommand(LiteralCommandNode<CommandSourceStack> editNode) {
        ConfigFieldList configFields = populateFields(null, this, this);
        recursiveEditCommand(editNode, configFields);
    }

    /**
     * Recursively generates the command for config editing and attaches it to node.
     * @param root node to attach available fields to, e. g. `/modid editConfig`
     * @param configFields a list of fields for this config.
     */
    @ApiStatus.Internal
    default void recursiveEditCommand(LiteralCommandNode<CommandSourceStack> root, ConfigFieldList configFields) {
        configFields.booleans().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", BoolArgumentType.bool())
                            .executes(context -> CommandFeedback.editConfigBoolean(context, configFields.parent(), this, attribute))
                    )
                    .executes(context -> generateFieldInfo(context, configFields.parent(), attribute))
                    .build();
            root.addChild(node);
        });

        configFields.integers().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", IntegerArgumentType.integer())
                            .executes(context -> editConfigInt(context, configFields.parent(), this, attribute))
                    )
                    .executes(context -> generateFieldInfo(context, configFields.parent(), attribute))
                    .build();
            root.addChild(node);
        });

        configFields.floats().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", FloatArgumentType.floatArg())
                            .executes(context -> editConfigFloat(context, configFields.parent(), this, attribute))
                    )
                    .executes(context -> generateFieldInfo(context, configFields.parent(), attribute))
                    .build();
            root.addChild(node);
        });

        configFields.doubles().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", DoubleArgumentType.doubleArg())
                            .executes(context -> editConfigDouble(context, configFields.parent(), this, attribute))
                    )
                    .executes(context -> generateFieldInfo(context, configFields.parent(), attribute))
                    .build();
            root.addChild(node);
        });

        configFields.strings().forEach(attribute -> {
            LiteralCommandNode<CommandSourceStack> node = literal(attribute.getName())
                    .then(argument("value", StringArgumentType.greedyString())
                            .executes(context -> editConfigObject(context, configFields.parent(), this, attribute))
                    )
                    .executes(context -> generateFieldInfo(context, configFields.parent(), attribute))
                    .build();
            root.addChild(node);
        });

        configFields.nestedFields().forEach(generator -> {
            Field parentField = generator.parentField();

            String nodeName;
            // Root node doesn't have a name
            if (parentField == null)
                nodeName = root.getName();
            else
                nodeName = parentField.getName();

            LiteralCommandNode<CommandSourceStack> node = literal(nodeName)
                    .executes(context -> {
                        if (parentField != null)
                            return generateFieldInfo(context, configFields.parent(), parentField);

                        // Root node cannot be executed
                        return -1;
                    })
                    .build();
            recursiveEditCommand(node, generator);
            root.addChild(node);
        });
    }

    /**
     * Gets the description for attribute field of parent object by checking relevant
     * {@link SerializedName} annotations.
     *
     * Some examples:
     * config.simpleToggle -> config = parent, simpleToggle field = attribute
     *
     * @param parent parent config object.
     * @param attribute field to generate description for.
     *
     * @return text description of field.
     */
    default MutableComponent generateFieldDescription(Object parent, Field attribute) {
        TextComponent fieldDesc = new TextComponent("");
        String attributeName = attribute.getName();

        // Filters out relevant comment fields
        Field[] fields = parent.getClass().getFields();
        List<Field> descriptions = Arrays.stream(fields).filter(field -> {
            String name = field.getName();
            return this.isDescription(field) && name.contains(attributeName) && field.isAnnotationPresent(SerializedName.class);
        }).collect(Collectors.toList());

        int size = descriptions.size();
        if (size > 0) {
            String[] descs = new String[size];
            descriptions.forEach(field -> {
                int index = NumberUtils.toInt(field.getName().replaceAll("\\D+", ""), 0);

                SerializedName serializedName = field.getAnnotation(SerializedName.class);
                String description = serializedName.value().substring("// ".length());

                descs[index] = description;
            });

            for (String desc : descs) {
                // Adding descriptions
                fieldDesc.append(new TextComponent(desc + "\n"));
            }
        } else {
            // This field has no comments describing it
            MutableComponent feedback = new TranslatedText("config2brigadier.command.config.edit.no_description_found", attributeName)
                    .withStyle(ChatFormatting.RED)
                    .append("\n");
            fieldDesc.append(feedback);
        }

        return fieldDesc;
    }

    /**
     * Generates text information for field and sends it to command executor.
     * @param context command executor.
     * @param parent parent object that contains the attribute. Used to get current attribute value.
     * @param attribute field to generate description for.
     *
     * @return 1 as success for command execution.
     */
    @ApiStatus.Internal
    default int generateFieldInfo(CommandContext<CommandSourceStack> context, Object parent, Field attribute) {
        MutableComponent fieldDesc = this.generateFieldDescription(parent, attribute);
        
        try {
            Object value = attribute.get(parent);

            String val = value.toString();
            // fixme Ugly check if it's not an object
            // Does this work todo
            if (!(value instanceof IConfig2B)) {
                MutableComponent valueComponent = new TextComponent(val + "\n").withStyle(ChatFormatting.AQUA);
                fieldDesc.append(new TranslatedText("config2brigadier.misc.current_value", valueComponent).withStyle(ChatFormatting.GRAY));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        MutableComponent type = new TextComponent(attribute.getType().getSimpleName()).withStyle(ChatFormatting.AQUA);
        fieldDesc.append(new TranslatedText("config2brigadier.misc.type", type).withStyle(ChatFormatting.GRAY));

        context.getSource().sendSuccess(fieldDesc.withStyle(ChatFormatting.GOLD), false);

        return 1;
    }
}
