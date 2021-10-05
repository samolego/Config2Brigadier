package org.samo_lego.config2brigadier.command;

import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.lang3.math.NumberUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CommandFeedback {

    public static int editConfigAttribute(CommandContext<CommandSourceStack> context, Object parent, Field attribute, Object value, Predicate<Field> fieldConsumer, Runnable saveConfigFunction) {
        attribute.setAccessible(true);
        boolean result = fieldConsumer.test(attribute);

        String option = parent.getClass().getSimpleName();

        if(!option.isEmpty()) {
            option = option.replaceAll("\\$", ".") + ".";
        }
        option += attribute.getName();

        if(result) {
            saveConfigFunction.run();

            context.getSource().sendSuccess(new TranslatableComponent("taterzens.command.config.edit.success", option, value.toString()).withStyle(ChatFormatting.GREEN), false);
        } else {
            context.getSource().sendFailure(new TranslatableComponent("taterzens.command.config.edit.failure", option).withStyle(ChatFormatting.RED));
        }

        return result ? 1 : 0;
    }

    public static int editConfigBoolean(CommandContext<CommandSourceStack> context, Object parent, Field attribute, Runnable saveConfigFunction) {
        boolean value = BoolArgumentType.getBool(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.setBoolean(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }, saveConfigFunction);
    }

    public static int editConfigInt(CommandContext<CommandSourceStack> context, Object parent, Field attribute, Runnable saveConfigFunction) {
        int value = IntegerArgumentType.getInteger(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.setInt(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }, saveConfigFunction);
    }

    public static int editConfigFloat(CommandContext<CommandSourceStack> context, Object parent, Field attribute, Runnable saveConfigFunction) {
        float value = FloatArgumentType.getFloat(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.setFloat(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }, saveConfigFunction);
    }

    public static int editConfigDouble(CommandContext<CommandSourceStack> context, Object parent, Field attribute, Runnable saveConfigFunction) {
        double value = DoubleArgumentType.getDouble(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.setDouble(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }, saveConfigFunction);
    }

    public static int editConfigObject(CommandContext<CommandSourceStack> context, Object parent, Field attribute, Runnable saveConfigFunction) {
        String value = StringArgumentType.getString(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.set(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }, saveConfigFunction);
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
     * @param commentPrefix comment prefix of description fields.
     * @return text description of field.
     */
    public static TextComponent defaultFieldDescription(Object parent, Field attribute, String commentPrefix) {
        TextComponent fieldDesc = new TextComponent("");
        String attributeName = attribute.getName();

        // Filters out relevant comment fields
        Field[] fields = parent.getClass().getFields();
        List<Field> descriptions = Arrays.stream(fields).filter(field -> {
            String name = field.getName();
            return name.startsWith(commentPrefix) && name.contains(attributeName) && field.isAnnotationPresent(SerializedName.class);
        }).collect(Collectors.toList());

        int size = descriptions.size();
        if(size > 0) {
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
            MutableComponent feedback = new TranslatableComponent("taterzens.command.config.edit.no_description_found", attributeName)
                    .withStyle(ChatFormatting.RED)
                    .append("\n");
            fieldDesc.append(feedback);
        }

        return fieldDesc;
    }

    public static int printFieldDescription(CommandContext<CommandSourceStack> context, Object parent, Field attribute, BiFunction<Object, Field, MutableComponent> commentText) {
        MutableComponent fieldDesc = commentText.apply(parent, attribute);

        try {
            Object value = attribute.get(parent);

            String val = value.toString();
            // fixme Ugly check if it's not an object
            if(!val.contains("@")) {
                MutableComponent valueComponent = new TextComponent(val + "\n").withStyle(ChatFormatting.AQUA);
                fieldDesc.append(new TranslatableComponent("taterzens.misc.current_value", valueComponent).withStyle(ChatFormatting.GRAY));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        MutableComponent type = new TextComponent(attribute.getType().getSimpleName()).withStyle(ChatFormatting.AQUA);
        fieldDesc.append(new TranslatableComponent("taterzens.misc.type", type).withStyle(ChatFormatting.GRAY));

        context.getSource().sendSuccess(fieldDesc.withStyle(ChatFormatting.GOLD), false);

        return 1;
    }
}
