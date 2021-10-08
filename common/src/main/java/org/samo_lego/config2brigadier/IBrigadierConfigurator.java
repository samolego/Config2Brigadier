package org.samo_lego.config2brigadier;

import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.ApiStatus;
import org.samo_lego.config2brigadier.annotation.BrigadierDescription;
import org.samo_lego.config2brigadier.annotation.BrigadierExcluded;
import org.samo_lego.config2brigadier.command.CommandFeedback;
import org.samo_lego.config2brigadier.util.ConfigFieldList;
import org.samo_lego.config2brigadier.util.TranslatedText;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
public interface IBrigadierConfigurator {

    String COMMENT_PREFIX = "_comment_";

    /**
     * Method called after a value is edited. The config should be saved to prevent
     * in-memory-only changes.
     */
    void save();

    /**
     * Loads changes from given config object into this object.
     * Useful as if we overwrite the config, we'd have to re-register command.
     * Usage:
     *     config.reloadValues(config, newConfig);
     *
     * @param config config object that will have its values changed.
     * @param newConfig new config object which field values will be copied over to default one.
     */
    default void reloadValues(Object config, Object newConfig) {
        try {
            for(Field field : config.getClass().getFields()) {
                Class<?> type = field.getType();

                if(Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                    continue;

                field.setAccessible(true);
                Object value = field.get(newConfig);

                if (type.isPrimitive() || type.equals(String.class) || type.equals(List.class)) {
                    field.set(config, value);
                } else {
                    // Recursion
                    this.reloadValues(field.get(config), value);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets description for the field.
     *
     * @param field field to check.
     * @return true if it is a description (comment) field, otherwise false.
     */
    default String getDescription(Field field) {
        String description = "";

        if(field.isAnnotationPresent(BrigadierDescription.class)) {
            description = field.getAnnotation(BrigadierDescription.class).value();
        } else {
            String name = field.getName();

            // Comments in style https://quiltservertools.github.io/ServerSideDevDocs/config/gson_config/#poor-mans-comments
            if(name.startsWith(COMMENT_PREFIX) && field.isAnnotationPresent(SerializedName.class)) {
                SerializedName serializedName = field.getAnnotation(SerializedName.class);
                description += serializedName.value().substring("// ".length());
            }
        }

        return description;
    }

    /**
     * Indicates whether this field should be excluded from command.
     * Field is excluded if it
     * <ul>
     *     <li>
     *         is static
     *     </li>
     *     <li>
     *         starts with "_comment_"
     *     </li>
     *     <li>
     *         has {@link BrigadierExcluded} annotation
     *     </li>
     * </ul>
     *
     * @param field field to check.
     * @return true if it should not be included in command, otherwise false.
     */
    default boolean shouldExclude(Field field) {
        return field.getName().startsWith(COMMENT_PREFIX) || field.isAnnotationPresent(BrigadierExcluded.class) || Modifier.isStatic(field.getModifiers());
    }

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

        configFields.lists().forEach(attribute -> {
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
        MutableComponent fieldDesc = new TextComponent("");
        String attributeName = attribute.getName();

        // Comment from @BrigadierDescription annotation
        String fieldDescription = this.getDescription(attribute);
        if(!fieldDescription.isEmpty()) {
            // Our annotation
            fieldDesc.append(new TextComponent(fieldDescription + "\n"));
        }


        // Comments from @Serialized name annotations.
        // Filters out relevant fields (ones that contain same name as field)
        Field[] fields = parent.getClass().getFields();
        List<Field> descriptions = Arrays.stream(fields).filter(field -> {
            String name = field.getName();
            return name.contains(attributeName);
        }).collect(Collectors.toList());

        // -1 as we don't want to include the option field itself, but just its comments.
        int size = descriptions.size() - 1;
        if (size > 0) {
            String[] descs = new String[size];

            descriptions.forEach(field -> {
                int index = NumberUtils.toInt(field.getName().replaceAll("\\D+", ""), 0);
                String description = this.getDescription(field);
                if(!description.isEmpty()) {
                    descs[index] = description;
                }
            });

            for (String desc : descs) {
                // Adding descriptions
                fieldDesc.append(new TextComponent(desc + "\n"));
            }
        }

        if(fieldDesc.getSiblings().isEmpty()) {
            // This field has no comments describing it
            MutableComponent feedback = new TranslatedText("config2brigadier.command.edit.no_description_found", attributeName)
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
        MutableComponent fieldDesc = new TextComponent("").append(this.generateFieldDescription(parent, attribute).withStyle(ChatFormatting.ITALIC));
        fieldDesc.withStyle(ChatFormatting.RESET);
        
        try {
            Object value = attribute.get(parent);

            String val = value.toString();
            // fixme Ugly check if it's not an object
            if (!val.contains("@")) {
                MutableComponent valueComponent = new TextComponent(val + "\n").withStyle(ChatFormatting.GREEN)
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(style -> style
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(val)))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, val))
                        );
                fieldDesc.append(new TranslatedText("config2brigadier.command.misc.current_value", valueComponent).withStyle(ChatFormatting.GRAY));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        // Default value
        if (attribute.isAnnotationPresent(BrigadierDescription.class)) {
            String defaultOption = attribute.getAnnotation(BrigadierDescription.class).defaultOption();
            MutableComponent defaultValueComponent = new TextComponent(defaultOption + "\n").withStyle(ChatFormatting.DARK_GREEN)
                .withStyle(style -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(defaultOption)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, defaultOption))
                );
            fieldDesc.append(new TranslatedText("config2brigadier.command.misc.default_value", defaultValueComponent).withStyle(ChatFormatting.GRAY));
        }

        MutableComponent type = new TextComponent(attribute.getType().getSimpleName()).withStyle(ChatFormatting.AQUA);
        fieldDesc.append(new TranslatedText("config2brigadier.command.misc.type", type).withStyle(ChatFormatting.GRAY));

        context.getSource().sendSuccess(fieldDesc.withStyle(ChatFormatting.GOLD), false);

        return 1;
    }
}
