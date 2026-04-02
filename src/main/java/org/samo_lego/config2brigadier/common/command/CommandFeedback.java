package org.samo_lego.config2brigadier.common.command;

import static java.lang.System.getLogger;
import static org.samo_lego.config2brigadier.common.Config2Brigadier.GSON;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.StringUtils;
import org.samo_lego.config2brigadier.common.Config2Brigadier;
import org.samo_lego.config2brigadier.common.IBrigadierConfigurator;

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
    public static int editConfigAttribute(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute,
        Object value,
        Predicate<Field> fieldConsumer
    ) {
        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            value,
            "config2brigadier.command.edit.success",
            false,
            fieldConsumer
        );
    }

    /**
     * Edits the config field with custom translation key.
     * @param context command executor to send feedback to.
     * @param parent parent object that contains field that will be changed.
     * @param config the config object which fields are getting modified.
     * @param attribute field to edit.
     * @param value new value for the field.
     * @param translationKey custom translation key for success message.
     * @param swapArgs whether to swap optionText and newValue in translation.
     * @param fieldConsumer lambda that modifies the field.
     * @return 1 for success, 0 for error.
     */
    public static int editConfigAttribute(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute,
        Object value,
        String translationKey,
        boolean swapArgs,
        Predicate<Field> fieldConsumer
    ) {
        if (parent == null) {
            context
                .getSource()
                .sendFailure(
                    Component.literal(
                        "Parent object is null. Key/index might be invalid."
                    )
                );
            return 0;
        }
        attribute.setAccessible(true);
        boolean successfulChange = fieldConsumer.test(attribute);

        String option = StringUtils.difference(
            config.getClass().getName(),
            parent.getClass().getName()
        );

        if (!option.isEmpty()) {
            option = option.replaceAll("\\$", ".") + ".";

            if (option.startsWith(".")) {
                option = option.substring(1);
            }
        }
        option += attribute.getName();
        var optionText = Component.literal(option);

        if (successfulChange) {
            config.save();
            MutableComponent newValue = Component.literal(
                value != null ? value.toString() : "null"
            ).withStyle(ChatFormatting.YELLOW);

            context
                .getSource()
                .sendSuccess(
                    () ->
                        Component.translatable(
                            translationKey,
                            swapArgs
                                ? newValue
                                : optionText.withStyle(ChatFormatting.YELLOW),
                            swapArgs
                                ? optionText.withStyle(ChatFormatting.YELLOW)
                                : newValue
                        ).withStyle(ChatFormatting.GREEN),
                    false
                );
        } else {
            context
                .getSource()
                .sendFailure(
                    Component.translatable("command.failed").withStyle(
                        ChatFormatting.RED
                    )
                );
        }

        return successfulChange ? 1 : 0;
    }

    public static Object getArg(
        CommandContext<CommandSourceStack> context,
        String name,
        Class<?> type
    ) {
        if (
            type == boolean.class || type == Boolean.class
        ) return BoolArgumentType.getBool(context, name);
        if (
            type == int.class || type == Integer.class
        ) return IntegerArgumentType.getInteger(context, name);
        if (
            type == long.class || type == Long.class
        ) return LongArgumentType.getLong(context, name);
        if (
            type == float.class || type == Float.class
        ) return FloatArgumentType.getFloat(context, name);
        if (
            type == double.class || type == Double.class
        ) return DoubleArgumentType.getDouble(context, name);
        if (type == String.class) return StringArgumentType.getString(
            context,
            name
        );
        return GSON.fromJson(StringArgumentType.getString(context, name), type);
    }

    /**
     * Edits the config boolean field.
     */
    public static int editConfigBoolean(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        boolean value = BoolArgumentType.getBool(context, "value");

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            value,
            field -> {
                try {
                    field.setBoolean(parent, value);
                    return true;
                } catch (IllegalAccessException e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    /**
     * Edits the config integer field.
     */
    public static int editConfigInt(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        int value = IntegerArgumentType.getInteger(context, "value");

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            value,
            field -> {
                try {
                    field.setInt(parent, value);
                    return true;
                } catch (IllegalAccessException e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    /**
     * Edits the config float field.
     */
    public static int editConfigFloat(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        float value = FloatArgumentType.getFloat(context, "value");

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            value,
            field -> {
                try {
                    field.setFloat(parent, value);
                    return true;
                } catch (IllegalAccessException e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    /**
     * Edits the config double field.
     */
    public static int editConfigDouble(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        double value = DoubleArgumentType.getDouble(context, "value");

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            value,
            field -> {
                try {
                    field.setDouble(parent, value);
                    return true;
                } catch (IllegalAccessException e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    /**
     * Edits the config object field.
     */
    public static int editConfigObject(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        String arg = StringArgumentType.getString(context, "value");

        // Fix for strings
        if (attribute.getType().equals(String.class)) {
            arg = "\"" + arg + "\"";
        }
        var value = GSON.fromJson(arg, attribute.getType());

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            value,
            field -> {
                try {
                    field.set(parent, value);
                    return true;
                } catch (IllegalAccessException e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    public static int editConfigMapSet(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        ParameterizedType mapType =
            (ParameterizedType) attribute.getGenericType();
        Class<?> keyClass = (Class<?>) mapType.getActualTypeArguments()[0];
        Class<?> valClass = (Class<?>) mapType.getActualTypeArguments()[1];

        Object k = getArg(context, "key", keyClass);
        Object v = getArg(context, "value", valClass);

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            v,
            field -> {
                try {
                    Map<Object, Object> map = (Map<Object, Object>) field.get(
                        parent
                    );
                    try {
                        map.put(k, v);
                    } catch (UnsupportedOperationException e) {
                        Map<Object, Object> newMap = new HashMap<>(map);
                        newMap.put(k, v);
                        field.set(parent, newMap);
                    }
                    return true;
                } catch (Exception e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    public static int editConfigMapRemove(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        ParameterizedType mapType =
            (ParameterizedType) attribute.getGenericType();
        Class<?> keyClass = (Class<?>) mapType.getActualTypeArguments()[0];
        Object k = getArg(context, "key", keyClass);

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            k,
            "config2brigadier.command.edit.remove.success",
            true,
            field -> {
                try {
                    Map<Object, Object> map = (Map<Object, Object>) field.get(
                        parent
                    );
                    try {
                        map.remove(k);
                    } catch (UnsupportedOperationException e) {
                        Map<Object, Object> newMap = new HashMap<>(map);
                        newMap.remove(k);
                        field.set(parent, newMap);
                    }
                    return true;
                } catch (Exception e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    public static int editConfigListAdd(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        ParameterizedType listType =
            (ParameterizedType) attribute.getGenericType();
        Class<?> valClass = (Class<?>) listType.getActualTypeArguments()[0];
        Object v = getArg(context, "value", valClass);

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            v,
            "config2brigadier.command.edit.add.success",
            true,
            field -> {
                try {
                    List<Object> list = (List<Object>) field.get(parent);
                    try {
                        list.add(v);
                    } catch (UnsupportedOperationException e) {
                        List<Object> newList = new ArrayList<>(list);
                        newList.add(v);
                        field.set(parent, newList);
                    }
                    return true;
                } catch (Exception e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    public static int editConfigListSet(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        int index = IntegerArgumentType.getInteger(context, "index");
        ParameterizedType listType =
            (ParameterizedType) attribute.getGenericType();
        Class<?> valClass = (Class<?>) listType.getActualTypeArguments()[0];
        Object v = getArg(context, "value", valClass);

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            v,
            field -> {
                try {
                    List<Object> list = (List<Object>) field.get(parent);
                    if (index < 0 || index >= list.size()) return false;
                    try {
                        list.set(index, v);
                    } catch (UnsupportedOperationException e) {
                        List<Object> newList = new ArrayList<>(list);
                        newList.set(index, v);
                        field.set(parent, newList);
                    }
                    return true;
                } catch (Exception e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    public static int editConfigListInsert(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        int index = IntegerArgumentType.getInteger(context, "index");
        ParameterizedType listType =
            (ParameterizedType) attribute.getGenericType();
        Class<?> valClass = (Class<?>) listType.getActualTypeArguments()[0];
        Object v = getArg(context, "value", valClass);

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            v,
            "config2brigadier.command.edit.add.success",
            true,
            field -> {
                try {
                    List<Object> list = (List<Object>) field.get(parent);
                    if (index < 0 || index > list.size()) return false;
                    try {
                        list.add(index, v);
                    } catch (UnsupportedOperationException e) {
                        List<Object> newList = new ArrayList<>(list);
                        newList.add(index, v);
                        field.set(parent, newList);
                    }
                    return true;
                } catch (Exception e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    public static int editConfigListRemove(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        ParameterizedType listType =
            (ParameterizedType) attribute.getGenericType();
        Class<?> valClass = (Class<?>) listType.getActualTypeArguments()[0];
        Object v = getArg(context, "value", valClass);

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            v,
            "config2brigadier.command.edit.remove.success",
            true,
            field -> {
                try {
                    List<Object> list = (List<Object>) field.get(parent);
                    try {
                        return list.remove(v);
                    } catch (UnsupportedOperationException e) {
                        List<Object> newList = new ArrayList<>(list);
                        boolean removed = newList.remove(v);
                        if (removed) field.set(parent, newList);
                        return removed;
                    }
                } catch (Exception e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    public static int editConfigSetAdd(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        ParameterizedType setType =
            (ParameterizedType) attribute.getGenericType();
        Class<?> valClass = (Class<?>) setType.getActualTypeArguments()[0];
        Object v = getArg(context, "value", valClass);

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            v,
            "config2brigadier.command.edit.add.success",
            true,
            field -> {
                try {
                    Set<Object> set = (Set<Object>) field.get(parent);
                    try {
                        return set.add(v);
                    } catch (UnsupportedOperationException e) {
                        Set<Object> newSet = new HashSet<>(set);
                        boolean added = newSet.add(v);
                        if (added) field.set(parent, newSet);
                        return added;
                    }
                } catch (Exception e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }

    public static int editConfigSetRemove(
        CommandContext<CommandSourceStack> context,
        Object parent,
        IBrigadierConfigurator config,
        Field attribute
    ) {
        ParameterizedType setType =
            (ParameterizedType) attribute.getGenericType();
        Class<?> valClass = (Class<?>) setType.getActualTypeArguments()[0];
        Object v = getArg(context, "value", valClass);

        return editConfigAttribute(
            context,
            parent,
            config,
            attribute,
            v,
            "config2brigadier.command.edit.remove.success",
            true,
            field -> {
                try {
                    Set<Object> set = (Set<Object>) field.get(parent);
                    try {
                        return set.remove(v);
                    } catch (UnsupportedOperationException e) {
                        Set<Object> newSet = new HashSet<>(set);
                        boolean removed = newSet.remove(v);
                        if (removed) field.set(parent, newSet);
                        return removed;
                    }
                } catch (Exception e) {
                    getLogger(Config2Brigadier.MOD_ID).log(
                        System.Logger.Level.ERROR,
                        "Failed to set boolean field: " + e.getMessage()
                    );
                }
                return false;
            }
        );
    }
}
