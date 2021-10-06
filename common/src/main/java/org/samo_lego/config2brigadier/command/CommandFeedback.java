package org.samo_lego.config2brigadier.command;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import org.samo_lego.config2brigadier.IConfig2B;
import org.samo_lego.config2brigadier.util.TranslatedText;

import java.lang.reflect.Field;
import java.util.function.Predicate;


public class CommandFeedback {

    public static int editConfigAttribute(CommandContext<CommandSourceStack> context, Object parent, IConfig2B config, Field attribute, Object value, Predicate<Field> fieldConsumer) {
        attribute.setAccessible(true);
        boolean result = fieldConsumer.test(attribute);

        String option = parent.getClass().getSimpleName();

        if(!option.isEmpty()) {
            option = option.replaceAll("\\$", ".") + ".";
        }
        option += attribute.getName();

        if(result) {
            config.save();

            context.getSource().sendSuccess(new TranslatedText("config2brigadier.command.config.edit.success", option, value.toString()).withStyle(ChatFormatting.GREEN), false);
        } else {
            context.getSource().sendFailure(new TranslatedText("config2brigadier.command.config.edit.failure", option).withStyle(ChatFormatting.RED));
        }

        return result ? 1 : 0;
    }

    public static int editConfigBoolean(CommandContext<CommandSourceStack> context, Object parent, IConfig2B config, Field attribute) {
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

    public static int editConfigInt(CommandContext<CommandSourceStack> context, Object parent, IConfig2B config, Field attribute) {
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

    public static int editConfigFloat(CommandContext<CommandSourceStack> context, Object parent, IConfig2B config, Field attribute) {
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

    public static int editConfigDouble(CommandContext<CommandSourceStack> context, Object parent, IConfig2B config, Field attribute) {
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

    public static int editConfigObject(CommandContext<CommandSourceStack> context, Object parent, IConfig2B config, Field attribute) {
        String value = StringArgumentType.getString(context, "value");

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
