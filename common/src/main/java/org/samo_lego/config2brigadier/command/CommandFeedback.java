package org.samo_lego.config2brigadier.command;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.StringUtils;
import org.samo_lego.config2brigadier.IBrigadierConfigurator;
import org.samo_lego.config2brigadier.util.TranslatedText;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import static org.samo_lego.config2brigadier.Config2Brigadier.GSON;

/**
 * Takes care of field editing and feedbacks for executed commands.
 */
public class CommandFeedback {

    /**
     * Edits the config field.
     * @param context command executor to send feedback to.
     * @param parent parent object that contains field that will be changed.
     * @param config the config object which fields are getting modified.
     * @param attribute field to edit.
     * @param value new value for the field.
     * @param fieldConsumer lambda that modifies the field (needs to be different for each primitive).
     *                      Should return true for success, false on error.
     * @return 1 for success, 0 for error.
     */
    public static int editConfigAttribute(CommandContext<CommandSourceStack> context, Object parent, IBrigadierConfigurator config, Field attribute, Object value, Predicate<Field> fieldConsumer) {
        attribute.setAccessible(true);
        boolean result = fieldConsumer.test(attribute);

        String option = StringUtils.difference(config.getClass().getName(), parent.getClass().getName());

        if(!option.isEmpty()) {
            option = option.replaceAll("\\$", ".") + ".";

            if(option.startsWith("."))
                option = option.substring(1);
        }
        option += attribute.getName();
        MutableComponent optionText = Component.literal(option);

        if(result) {
            config.save();
            MutableComponent newValue = Component.literal(value.toString()).withStyle(ChatFormatting.YELLOW);

            context.getSource().sendSuccess(
                    MutableComponent.create(
                                    new TranslatedText("config2brigadier.command.edit.success",
                                            optionText.withStyle(ChatFormatting.YELLOW),
                                            newValue))
                            .withStyle(ChatFormatting.GREEN),
                    false);
        } else {
            context.getSource().sendFailure(Component.translatable("command.failed").withStyle(ChatFormatting.RED));
        }

        return result ? 1 : 0;
    }

    /**
     * Edits the config boolean field.
     * @param context command executor to send feedback to.
     * @param parent parent object that contains field that will be changed.
     * @param config the config object which fields are getting modified.
     * @param attribute field to edit.
     * @return 1 for success, 0 for error.
     */
    public static int editConfigBoolean(CommandContext<CommandSourceStack> context, Object parent, IBrigadierConfigurator config, Field attribute) {
        boolean value = BoolArgumentType.getBool(context, "value");

        return editConfigAttribute(context, parent, config, attribute, value, field -> {
            try {
                field.setBoolean(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Edits the config integer field.
     * @param context command executor to send feedback to.
     * @param parent parent object that contains field that will be changed.
     * @param config the config object which fields are getting modified.
     * @param attribute field to edit.
     * @return 1 for success, 0 for error.
     */
    public static int editConfigInt(CommandContext<CommandSourceStack> context, Object parent, IBrigadierConfigurator config, Field attribute) {
        int value = IntegerArgumentType.getInteger(context, "value");

        return editConfigAttribute(context, parent, config, attribute, value, field -> {
            try {
                field.setInt(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Edits the config float field.
     * @param context command executor to send feedback to.
     * @param parent parent object that contains field that will be changed.
     * @param config the config object which fields are getting modified.
     * @param attribute field to edit.
     * @return 1 for success, 0 for error.
     */
    public static int editConfigFloat(CommandContext<CommandSourceStack> context, Object parent, IBrigadierConfigurator config, Field attribute) {
        float value = FloatArgumentType.getFloat(context, "value");

        return editConfigAttribute(context, parent, config, attribute, value, field -> {
            try {
                field.setFloat(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Edits the config double field.
     * @param context command executor to send feedback to.
     * @param parent parent object that contains field that will be changed.
     * @param config the config object which fields are getting modified.
     * @param attribute field to edit.
     * @return 1 for success, 0 for error.
     */
    public static int editConfigDouble(CommandContext<CommandSourceStack> context, Object parent, IBrigadierConfigurator config, Field attribute) {
        double value = DoubleArgumentType.getDouble(context, "value");

        return editConfigAttribute(context, parent, config, attribute, value, field -> {
            try {
                field.setDouble(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Edits the config object field.
     * @param context command executor to send feedback to.
     * @param parent parent object that contains field that will be changed.
     * @param config the config object which fields are getting modified.
     * @param attribute field to edit.
     * @return 1 for success, 0 for error.
     */
    public static int editConfigObject(CommandContext<CommandSourceStack> context, Object parent, IBrigadierConfigurator config, Field attribute) {
        String arg = StringArgumentType.getString(context, "value");

        // Fix for strings
        if (attribute.getType().equals(String.class))
            arg = "\"" + arg + "\"";
        Object value = GSON.fromJson(arg, attribute.getType());

        return editConfigAttribute(context, parent, config, attribute, value, field -> {
            try {
                field.set(parent, value);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        });
    }
}
