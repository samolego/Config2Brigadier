package org.samo_lego.config2brigadier.command;

import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.lang3.math.NumberUtils;
import org.samo_lego.config2brigadier.IConfig2B;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.samo_lego.config2brigadier.util.PlatformDependent.translatedComponent;

public class CommandFeedback {

    public static int editConfigAttribute(CommandContext<CommandSourceStack> context, IConfig2B parent, Field attribute, Object value, Predicate<Field> fieldConsumer) {
        attribute.setAccessible(true);
        boolean result = fieldConsumer.test(attribute);

        String option = parent.getClass().getSimpleName();

        if(!option.isEmpty()) {
            option = option.replaceAll("\\$", ".") + ".";
        }
        option += attribute.getName();

        if(result) {
            parent.save();

            context.getSource().sendSuccess(translatedComponent("config2brigadier.command.config.edit.success", option, value.toString()).withStyle(ChatFormatting.GREEN), false);
        } else {
            context.getSource().sendFailure(translatedComponent("config2brigadier.command.config.edit.failure", option).withStyle(ChatFormatting.RED));
        }

        return result ? 1 : 0;
    }

    public static int editConfigBoolean(CommandContext<CommandSourceStack> context, IConfig2B parent, Field attribute) {
        boolean value = BoolArgumentType.getBool(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.setBoolean(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    public static int editConfigInt(CommandContext<CommandSourceStack> context, IConfig2B parent, Field attribute) {
        int value = IntegerArgumentType.getInteger(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.setInt(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    public static int editConfigFloat(CommandContext<CommandSourceStack> context, IConfig2B parent, Field attribute) {
        float value = FloatArgumentType.getFloat(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.setFloat(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    public static int editConfigDouble(CommandContext<CommandSourceStack> context, IConfig2B parent, Field attribute) {
        double value = DoubleArgumentType.getDouble(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.setDouble(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    public static int editConfigObject(CommandContext<CommandSourceStack> context, IConfig2B parent, Field attribute) {
        String value = StringArgumentType.getString(context, "value");

        return editConfigAttribute(context, parent, attribute, value, field -> {
            try {
                field.set(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
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
     * @return text description of field.
     */
    public static TextComponent defaultFieldDescription(IConfig2B parent, Field attribute) {
        TextComponent fieldDesc = new TextComponent("");
        String attributeName = attribute.getName();

        // Filters out relevant comment fields
        Field[] fields = parent.getClass().getFields();
        List<Field> descriptions = Arrays.stream(fields).filter(field -> {
            String name = field.getName();
            return parent.isDescription(field) && name.contains(attributeName) && field.isAnnotationPresent(SerializedName.class);
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
            MutableComponent feedback = translatedComponent("config2brigadier.command.config.edit.no_description_found", attributeName)
                    .withStyle(ChatFormatting.RED)
                    .append("\n");
            fieldDesc.append(feedback);
        }

        return fieldDesc;
    }

    public static int printFieldDescription(CommandContext<CommandSourceStack> context, IConfig2B parent, Field attribute, BiFunction<IConfig2B, Field, MutableComponent> commentText) {
        MutableComponent fieldDesc = commentText.apply(parent, attribute);

        try {
            Object value = attribute.get(parent);

            String val = value.toString();
            // fixme Ugly check if it's not an object
            // Does this work todo
            if(!(value instanceof IConfig2B)) {
                MutableComponent valueComponent = new TextComponent(val + "\n").withStyle(ChatFormatting.AQUA);
                fieldDesc.append(translatedComponent("config2brigadier.misc.current_value", valueComponent).withStyle(ChatFormatting.GRAY));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        MutableComponent type = new TextComponent(attribute.getType().getSimpleName()).withStyle(ChatFormatting.AQUA);
        fieldDesc.append(translatedComponent("config2brigadier.misc.type", type).withStyle(ChatFormatting.GRAY));

        context.getSource().sendSuccess(fieldDesc.withStyle(ChatFormatting.GOLD), false);

        return 1;
    }
}
