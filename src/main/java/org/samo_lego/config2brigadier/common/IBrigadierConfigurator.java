package org.samo_lego.config2brigadier.common;

import static java.util.logging.Logger.getLogger;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.samo_lego.config2brigadier.common.Config2Brigadier.GSON;
import static org.samo_lego.config2brigadier.common.Config2Brigadier.MOD_ID;
import static org.samo_lego.config2brigadier.common.util.ConfigFieldList.populateFields;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.permissions.Permission.HasCommandLevel;
import net.minecraft.server.permissions.PermissionLevel;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.config2brigadier.common.annotation.BrigadierDescription;
import org.samo_lego.config2brigadier.common.annotation.BrigadierExcluded;
import org.samo_lego.config2brigadier.common.command.CommandFeedback;
import org.samo_lego.config2brigadier.common.util.ConfigFieldList;

/**
 * An interface your config should implement.
 */
public interface IBrigadierConfigurator {
    /**
     * Default comment field prefix.
     */
    String COMMENT_PREFIX = "_comment_";

    String CONFIG_STR = "config";
    String EDIT_STR = "edit";
    String RELOAD_STR = "reload";

    Gson MSG_GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)

        .create();

    /**
     * Method called after a value is edited. The config should be saved to prevent
     * in-memory-only changes.
     * <p>
     * You can call {@link #writeToFile(File)} to save the config to a file.
     */
    void save();

    /**
     * Loads changes from given config object into this object.
     * Useful as if we overwrite the config, we'd have to re-register command.
     *
     * @param newConfig new config object which field values will be copied over to default one.
     */
    default void reload(IBrigadierConfigurator newConfig) {
        this.reloadValues(this, newConfig);
    }

    /**
     * Loads changes from given config object into this object recursively.
     * Useful as if we overwrite the config, we'd have to re-register command.
     * Usage:
     *     config.reloadValues(config, newConfig);
     *
     * @param config config object that will have its values changed.
     * @param newConfig new config object which field values will be copied over to default one.
     */
    @ApiStatus.Internal
    default void reloadValues(Object config, Object newConfig) {
        try {
            for (Field field : config.getClass().getFields()) {
                Class<?> type = field.getType();

                if (
                    Modifier.isFinal(field.getModifiers()) ||
                    Modifier.isStatic(field.getModifiers())
                ) {
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(newConfig);

                if (
                    type.isPrimitive() ||
                    type.equals(String.class) ||
                    Collection.class.isAssignableFrom(type) ||
                    Map.class.isAssignableFrom(type)
                ) {
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
     * @return description string of the field.
     */
    default String getDescription(Field field) {
        String description = "";

        if (field.isAnnotationPresent(BrigadierDescription.class)) {
            description = field
                .getAnnotation(BrigadierDescription.class)
                .value();
        } else if (this.enableSerializedNameComments()) {
            String name = field.getName();

            // Comments in style https://quiltservertools.github.io/ServerSideDevDocs/config/gson_config/#poor-mans-comments
            if (
                name.startsWith(this.getCommentPrefix()) &&
                field.isAnnotationPresent(SerializedName.class)
            ) {
                SerializedName serializedName = field.getAnnotation(
                    SerializedName.class
                );
                description += serializedName.value().substring("// ".length());
            }
        }

        return description;
    }

    /**
     * Whether to try getting comments from fields
     * that are prefixed with {@link IBrigadierConfigurator#getCommentPrefix()} and
     * have custom {@link SerializedName} values.
     *
     * @see <a href="https://quiltservertools.github.io/ServerSideDevDocs/config/gson_config/#poor-mans-comments">Server Dev Docs</a>.
     * @return true by default.
     */
    default boolean enableSerializedNameComments() {
        return true;
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
        return (
            field.getName().startsWith(this.getCommentPrefix()) ||
            field.isAnnotationPresent(BrigadierExcluded.class) ||
            Modifier.isStatic(field.getModifiers())
        );
    }

    /**
     * Loads config file.
     *
     * @param file          file to load the language file from.
     * @param configClass   class of config object.
     * @param fallbackConstructor default config supplier.
     * @return config object
     */
    static <C extends IBrigadierConfigurator> C loadConfigFile(
        File file,
        Class<C> configClass,
        Supplier<C> fallbackConstructor
    ) {
        C config = null;
        if (file.exists()) {
            try (
                BufferedReader fileReader = new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(file),
                        StandardCharsets.UTF_8
                    )
                )
            ) {
                config = GSON.fromJson(fileReader, configClass);
            } catch (IOException e) {
                getLogger(MOD_ID).severe(
                    "[Config2Brigadier] Problem occurred when trying to load config: " +
                        e.getMessage()
                );
            }
        }
        if (config == null) {
            config = fallbackConstructor.get();
        }

        config.writeToFile(file);

        return config;
    }

    /**
     * Generates the command and attaches it to the provided child.
     *
     * @param editNode    child to attach fields to.
     */
    default void buildEditCommand(
        LiteralCommandNode<CommandSourceStack> editNode,
        String permissionPrefix
    ) {
        ConfigFieldList configFields = populateFields(null, this, this);

        var permissionNodes = new LinkedList<String>();
        permissionNodes.add(permissionPrefix);
        recursiveEditCommand(editNode, configFields, permissionNodes, context ->
            this
        );
    }

    /**
     * Generates the command and attaches it to the provided child.
     * @param editNode the command node to attach fields to.
     * @deprecated use {@link #generateReloadableConfigCommand(String, CommandDispatcher, Supplier)} or {@link #generateConfigCommand(String, CommandDispatcher)} instead.
     * If you still want to manually generate the command, use {@link #buildEditCommand(LiteralCommandNode, String)}.
     */
    @Deprecated
    default void generateCommand(
        LiteralCommandNode<CommandSourceStack> editNode
    ) {
        ConfigFieldList configFields = populateFields(null, this, this);
        recursiveEditCommand(
            editNode,
            configFields,
            new LinkedList<>(),
            context -> this
        );
    }

    /**
     * All-in-one solution for generating config command.
     * Doesn't include reloading.
     * @param modId your mod id
     * @param dispatcher command dispatcher
     */
    default void generateConfigCommand(
        String modId,
        CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        this.generateReloadableConfigCommand(modId, dispatcher, null);
    }

    /**
     * All-in-one solution for generating config command.
     * @param modId mod id
     * @param dispatcher command dispatcher
     * Generates command like:
     * /modid config edit &lt;config fields&gt;
     * And the reloading (if provided):
     * /modid config reload
     */
    default void generateReloadableConfigCommand(
        String modId,
        CommandDispatcher<CommandSourceStack> dispatcher,
        @Nullable Supplier<? extends IBrigadierConfigurator> newConfigLoader
    ) {
        var root = dispatcher.register(literal(modId));
        var configNode = literal(CONFIG_STR).build();

        var editNode = literal(EDIT_STR).build();

        ConfigFieldList configFields = populateFields(null, this, this);

        var permissionNodes = new LinkedList<String>();
        permissionNodes.add(modId);
        permissionNodes.add(CONFIG_STR);
        permissionNodes.add(EDIT_STR);
        recursiveEditCommand(editNode, configFields, permissionNodes, context ->
            this
        );

        permissionNodes.removeLast();

        if (newConfigLoader != null) {
            permissionNodes.add(RELOAD_STR);
            var gameMastersPermission = new HasCommandLevel(
                PermissionLevel.GAMEMASTERS
            );
            var reloadNode = literal(RELOAD_STR)
                .requires(src ->
                    Permissions.check(
                        src,
                        String.join(".", permissionNodes),
                        src.permissions().hasPermission(gameMastersPermission)
                    )
                )
                .executes(context -> {
                    this.reload(newConfigLoader.get());
                    this.save();
                    context
                        .getSource()
                        .sendSuccess(
                            () ->
                                Component.translatable(
                                    "commands.reload.success"
                                ).withStyle(ChatFormatting.GREEN),
                            false
                        );
                    return 1;
                })
                .build();
            configNode.addChild(reloadNode);
            permissionNodes.removeLast();
        }

        configNode.addChild(editNode);

        root.addChild(configNode);
    }

    /**
     * Recursively generates the command for config editing and attaches it to child.
     * @param root child to attach available fields to, e. g. `/modid editConfig`
     * @param configFields a list of fields for this config.
     * @param permissionNodes list of permission nodes. If empty, no permission check will be done.
     * @param parentProvider provider for the parent object.
     */
    @ApiStatus.Internal
    default void recursiveEditCommand(
        CommandNode<CommandSourceStack> root,
        ConfigFieldList configFields,
        List<String> permissionNodes,
        Function<CommandContext<CommandSourceStack>, Object> parentProvider
    ) {
        // CommandData class to store data for command generation.
        record CommandData(
            Iterable<Field> fields,
            ArgumentType<?> argumentType,
            BiFunction<
                CommandContext<CommandSourceStack>,
                Field,
                Integer
            > editorFunction
        ) {}

        var commandData = new CommandData[] {
            new CommandData(
                configFields.booleans(),
                BoolArgumentType.bool(),
                (context, field) ->
                    CommandFeedback.editConfigBoolean(
                        context,
                        parentProvider.apply(context),
                        this,
                        field
                    )
            ),
            new CommandData(
                configFields.integers(),
                IntegerArgumentType.integer(),
                (context, field) ->
                    CommandFeedback.editConfigInt(
                        context,
                        parentProvider.apply(context),
                        this,
                        field
                    )
            ),
            new CommandData(
                configFields.floats(),
                FloatArgumentType.floatArg(),
                (context, field) ->
                    CommandFeedback.editConfigFloat(
                        context,
                        parentProvider.apply(context),
                        this,
                        field
                    )
            ),
            new CommandData(
                configFields.doubles(),
                DoubleArgumentType.doubleArg(),
                (context, field) ->
                    CommandFeedback.editConfigDouble(
                        context,
                        parentProvider.apply(context),
                        this,
                        field
                    )
            ),
            new CommandData(
                configFields.objects(),
                StringArgumentType.greedyString(),
                (context, field) ->
                    CommandFeedback.editConfigObject(
                        context,
                        parentProvider.apply(context),
                        this,
                        field
                    )
            ),
        };

        var ownersPermission = new HasCommandLevel(PermissionLevel.OWNERS);
        String commandPrefix = String.join(" ", permissionNodes);

        // Build the edit nodes for sub-*values*.
        for (CommandData data : commandData) {
            for (Field field : data.fields) {
                var argType = data.argumentType;

                var commandNodeName = field.getName();

                String permission;
                if (!permissionNodes.isEmpty()) {
                    permissionNodes.add(commandNodeName);
                    permission = String.join(".", permissionNodes);
                    permissionNodes.removeLast();
                } else {
                    permission = "";
                }

                LiteralCommandNode<CommandSourceStack> node = literal(
                    commandNodeName
                )
                    .requires(
                        src ->
                            (permission.isEmpty() &&
                                src
                                    .permissions()
                                    .hasPermission(ownersPermission)) ||
                            Permissions.check(
                                src,
                                permission,
                                src
                                    .permissions()
                                    .hasPermission(ownersPermission)
                            )
                    )
                    .then(
                        argument("value", argType).executes(context ->
                            data.editorFunction.apply(context, field)
                        )
                    )
                    .executes(context ->
                        generateFieldInfo(
                            context,
                            parentProvider.apply(context),
                            field,
                            commandPrefix + " " + commandNodeName
                        )
                    )
                    .build();
                root.addChild(node);
            }
        }

        // Maps
        for (Field field : configFields.maps()) {
            var commandNodeName = field.getName();
            String permission;
            if (!permissionNodes.isEmpty()) {
                permissionNodes.add(commandNodeName);
                permission = String.join(".", permissionNodes);
                permissionNodes.removeLast();
            } else {
                permission = "";
            }

            ParameterizedType mapType =
                (ParameterizedType) field.getGenericType();
            Type keyType = mapType.getActualTypeArguments()[0];
            Type valType = mapType.getActualTypeArguments()[1];

            ArgumentType<?> keyArg = getArgumentTypeFor(keyType, false);
            ArgumentType<?> valArg = getArgumentTypeFor(valType, true);

            var node = literal(commandNodeName)
                .requires(
                    src ->
                        (permission.isEmpty() &&
                            src
                                .permissions()
                                .hasPermission(ownersPermission)) ||
                        Permissions.check(
                            src,
                            permission,
                            src.permissions().hasPermission(ownersPermission)
                        )
                )
                .executes(context ->
                    generateFieldInfo(
                        context,
                        parentProvider.apply(context),
                        field,
                        commandPrefix + " " + commandNodeName
                    )
                )
                .build();

            var setNode = literal("set")
                .then(
                    argument("key", keyArg)
                        .suggests((context, builder) -> {
                            try {
                                Map<?, ?> map = (Map<?, ?>) field.get(
                                    parentProvider.apply(context)
                                );
                                if (map != null) map
                                    .keySet()
                                    .forEach(k ->
                                        builder.suggest(String.valueOf(k))
                                    );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return builder.buildFuture();
                        })
                        .then(
                            argument("value", valArg).executes(context ->
                                CommandFeedback.editConfigMapSet(
                                    context,
                                    parentProvider.apply(context),
                                    this,
                                    field
                                )
                            )
                        )
                )
                .build();
            node.addChild(setNode);

            var removeNode = literal("remove")
                .then(
                    argument("key", keyArg)
                        .suggests((context, builder) -> {
                            try {
                                Map<?, ?> map = (Map<?, ?>) field.get(
                                    parentProvider.apply(context)
                                );
                                if (map != null) map
                                    .keySet()
                                    .forEach(k ->
                                        builder.suggest(String.valueOf(k))
                                    );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return builder.buildFuture();
                        })
                        .executes(context ->
                            CommandFeedback.editConfigMapRemove(
                                context,
                                parentProvider.apply(context),
                                this,
                                field
                            )
                        )
                )
                .build();
            node.addChild(removeNode);

            // Nested editing for Map entries
            if (valType instanceof Class<?> valClass && !isSimple(valClass)) {
                var entryNode = literal("entry")
                    .then(
                        argument("key", keyArg).suggests((context, builder) -> {
                            try {
                                Map<?, ?> map = (Map<?, ?>) field.get(
                                    parentProvider.apply(context)
                                );
                                if (map != null) map
                                    .keySet()
                                    .forEach(k ->
                                        builder.suggest(String.valueOf(k))
                                    );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return builder.buildFuture();
                        })
                    )
                    .build();

                var keyArgNode = entryNode.getChild("key");

                ConfigFieldList entryTemplate = ConfigFieldList.template(
                    null,
                    valClass,
                    this
                );
                if (!permissionNodes.isEmpty()) permissionNodes.add(
                    commandNodeName
                );
                recursiveEditCommand(
                    keyArgNode,
                    entryTemplate,
                    permissionNodes,
                    context -> {
                        try {
                            Map<?, ?> map = (Map<?, ?>) field.get(
                                parentProvider.apply(context)
                            );
                            if (map == null) return null;
                            Object k = CommandFeedback.getArg(
                                context,
                                "key",
                                (Class<?>) keyType
                            );
                            return map.get(k);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                );
                if (!permissionNodes.isEmpty()) permissionNodes.removeLast();
                node.addChild(entryNode);
            }

            root.addChild(node);
        }

        // Lists
        for (Field field : configFields.lists()) {
            var commandNodeName = field.getName();
            String permission;
            if (!permissionNodes.isEmpty()) {
                permissionNodes.add(commandNodeName);
                permission = String.join(".", permissionNodes);
                permissionNodes.removeLast();
            } else {
                permission = "";
            }

            ParameterizedType listType =
                (ParameterizedType) field.getGenericType();
            Type valType = listType.getActualTypeArguments()[0];
            ArgumentType<?> valArg = getArgumentTypeFor(valType, true);

            var node = literal(commandNodeName)
                .requires(
                    src ->
                        (permission.isEmpty() &&
                            src
                                .permissions()
                                .hasPermission(ownersPermission)) ||
                        Permissions.check(
                            src,
                            permission,
                            src.permissions().hasPermission(ownersPermission)
                        )
                )
                .executes(context ->
                    generateFieldInfo(
                        context,
                        parentProvider.apply(context),
                        field,
                        commandPrefix + " " + commandNodeName
                    )
                )
                .build();

            var addNode = literal("add")
                .then(
                    argument("value", valArg).executes(context ->
                        CommandFeedback.editConfigListAdd(
                            context,
                            parentProvider.apply(context),
                            this,
                            field
                        )
                    )
                )
                .build();
            node.addChild(addNode);

            var editNode = literal("edit")
                .then(
                    argument("index", IntegerArgumentType.integer(0)).then(
                        argument("value", valArg).executes(context ->
                            CommandFeedback.editConfigListSet(
                                context,
                                parentProvider.apply(context),
                                this,
                                field
                            )
                        )
                    )
                )
                .build();
            node.addChild(editNode);

            var insertNode = literal("insert")
                .then(
                    argument("index", IntegerArgumentType.integer(0)).then(
                        argument("value", valArg).executes(context ->
                            CommandFeedback.editConfigListInsert(
                                context,
                                parentProvider.apply(context),
                                this,
                                field
                            )
                        )
                    )
                )
                .build();
            node.addChild(insertNode);

            var removeNode = literal("remove")
                .then(
                    argument("value", valArg)
                        .suggests((context, builder) -> {
                            try {
                                List<?> list = (List<?>) field.get(
                                    parentProvider.apply(context)
                                );
                                if (list != null) list.forEach(k ->
                                    builder.suggest(String.valueOf(k))
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return builder.buildFuture();
                        })
                        .executes(context ->
                            CommandFeedback.editConfigListRemove(
                                context,
                                parentProvider.apply(context),
                                this,
                                field
                            )
                        )
                )
                .build();
            node.addChild(removeNode);

            // Nested editing for List entries
            if (valType instanceof Class<?> valClass && !isSimple(valClass)) {
                var entryNode = literal("entry")
                    .then(argument("index", IntegerArgumentType.integer(0)))
                    .build();

                var indexArgNode = entryNode.getChild("index");

                ConfigFieldList entryTemplate = ConfigFieldList.template(
                    null,
                    valClass,
                    this
                );
                if (!permissionNodes.isEmpty()) permissionNodes.add(
                    commandNodeName
                );
                recursiveEditCommand(
                    indexArgNode,
                    entryTemplate,
                    permissionNodes,
                    context -> {
                        try {
                            List<?> list = (List<?>) field.get(
                                parentProvider.apply(context)
                            );
                            if (list == null) return null;
                            int index = IntegerArgumentType.getInteger(
                                context,
                                "index"
                            );
                            return list.get(index);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                );
                if (!permissionNodes.isEmpty()) permissionNodes.removeLast();
                node.addChild(entryNode);
            }

            root.addChild(node);
        }

        // Sets
        for (Field field : configFields.sets()) {
            var commandNodeName = field.getName();
            String permission;
            if (!permissionNodes.isEmpty()) {
                permissionNodes.add(commandNodeName);
                permission = String.join(".", permissionNodes);
                permissionNodes.removeLast();
            } else {
                permission = "";
            }

            ParameterizedType setType =
                (ParameterizedType) field.getGenericType();
            Type valType = setType.getActualTypeArguments()[0];
            ArgumentType<?> valArg = getArgumentTypeFor(valType, true);

            var node = literal(commandNodeName)
                .requires(
                    src ->
                        (permission.isEmpty() &&
                            src
                                .permissions()
                                .hasPermission(ownersPermission)) ||
                        Permissions.check(
                            src,
                            permission,
                            src.permissions().hasPermission(ownersPermission)
                        )
                )
                .executes(context ->
                    generateFieldInfo(
                        context,
                        parentProvider.apply(context),
                        field,
                        commandPrefix + " " + commandNodeName
                    )
                )
                .build();

            var addNode = literal("add")
                .then(
                    argument("value", valArg).executes(context ->
                        CommandFeedback.editConfigSetAdd(
                            context,
                            parentProvider.apply(context),
                            this,
                            field
                        )
                    )
                )
                .build();
            node.addChild(addNode);

            var removeNode = literal("remove")
                .then(
                    argument("value", valArg)
                        .suggests((context, builder) -> {
                            try {
                                Set<?> set = (Set<?>) field.get(
                                    parentProvider.apply(context)
                                );
                                if (set != null) set.forEach(k ->
                                    builder.suggest(String.valueOf(k))
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return builder.buildFuture();
                        })
                        .executes(context ->
                            CommandFeedback.editConfigSetRemove(
                                context,
                                parentProvider.apply(context),
                                this,
                                field
                            )
                        )
                )
                .build();
            node.addChild(removeNode);

            root.addChild(node);
        }

        // Recursively generate the command for sub-*objects*.
        for (ConfigFieldList generator : configFields.nestedFields()) {
            Field parentField = generator.parentField();

            String nodeName;
            // Root child doesn't have a name
            if (parentField == null) {
                nodeName = root.getName();
            } else {
                nodeName = parentField.getName();
            }

            LiteralCommandNode<CommandSourceStack> child = literal(nodeName)
                .executes(context -> {
                    if (parentField != null) {
                        return generateFieldInfo(
                            context,
                            parentProvider.apply(context),
                            parentField,
                            commandPrefix + " " + nodeName
                        );
                    }

                    // Root child cannot be executed
                    return -1;
                })
                .build();

            if (!permissionNodes.isEmpty()) {
                permissionNodes.add(nodeName);
            }
            recursiveEditCommand(child, generator, permissionNodes, context -> {
                try {
                    Object p = parentProvider.apply(context);
                    if (p == null) return null;
                    parentField.setAccessible(true);
                    return parentField.get(p);
                } catch (Exception e) {
                    return null;
                }
            });
            if (!permissionNodes.isEmpty()) {
                permissionNodes.removeLast();
            }
            root.addChild(child);
        }
    }

    private boolean isSimple(Class<?> clazz) {
        return (
            clazz.isPrimitive() ||
            clazz == Boolean.class ||
            clazz == Integer.class ||
            clazz == Long.class ||
            clazz == Float.class ||
            clazz == Double.class ||
            clazz == String.class
        );
    }

    /**
     * Gets {@link ArgumentType} for given type.
     * @param type type to get {@link ArgumentType} for.
     * @param greedy whether to use greedy string for {@link StringArgumentType}.
     * @return {@link ArgumentType} for given type.
     */
    default ArgumentType<?> getArgumentTypeFor(Type type, boolean greedy) {
        if (type instanceof Class<?> clazz) {
            if (
                clazz == boolean.class || clazz == Boolean.class
            ) return BoolArgumentType.bool();
            if (
                clazz == int.class || clazz == Integer.class
            ) return IntegerArgumentType.integer();
            if (
                clazz == long.class || clazz == Long.class
            ) return LongArgumentType.longArg();
            if (
                clazz == float.class || clazz == Float.class
            ) return FloatArgumentType.floatArg();
            if (
                clazz == double.class || clazz == Double.class
            ) return DoubleArgumentType.doubleArg();
        }
        return greedy
            ? StringArgumentType.greedyString()
            : StringArgumentType.string();
    }

    /**
     * Gets comment prefix of fields.
     * @return field prefix that is used by comment fields.
     */
    default String getCommentPrefix() {
        return IBrigadierConfigurator.COMMENT_PREFIX;
    }

    /**
     * Saves the config to the given file.
     *
     * @param file file to save config to
     */
    default void writeToFile(File file) {
        try (
            Writer writer = new OutputStreamWriter(
                new FileOutputStream(file),
                StandardCharsets.UTF_8
            )
        ) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            getLogger(MOD_ID).severe(
                "[Config2Brigadier] Problem occurred when saving config: " +
                    e.getMessage()
            );
        }
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
    default MutableComponent generateFieldDescription(
        Object parent,
        Field attribute
    ) {
        MutableComponent textFeedback = Component.literal("");
        String attributeName = attribute.getName();

        // Comment from @BrigadierDescription annotation
        String fieldDescription = this.getDescription(attribute);
        boolean emptyBrigadierDesc = fieldDescription.isEmpty();
        if (!emptyBrigadierDesc) {
            // Our annotation
            textFeedback.append(Component.translatable(fieldDescription));
        }

        // Comments from @Serialized name annotations.
        // Filters out relevant fields (ones that contain same name as field)
        Field[] fields = parent.getClass().getFields();
        String prefix = this.getCommentPrefix() + attributeName;
        List<Field> descriptionList = Arrays.stream(fields)
            .filter(field -> {
                String name = field.getName();
                return name.matches("^" + prefix + "\\d*$");
            })
            .toList();

        // -1 as we don't want to include the option field itself, but just its comments.
        int size = descriptionList.size();
        if (size > 0) {
            String[] sortedDescriptions = new String[size];

            descriptionList.forEach(field -> {
                int index = NumberUtils.toInt(
                    field.getName().replaceAll("\\D+", ""),
                    0
                );
                sortedDescriptions[index] = this.getDescription(field);
            });

            for (int i = 0; i < sortedDescriptions.length; ++i) {
                // Adding descriptions
                String desc = sortedDescriptions[i];
                if (i == 0 && emptyBrigadierDesc) textFeedback.append(
                    Component.literal(desc)
                );
                else textFeedback.append(Component.literal("\n").append(desc));
            }
        }

        if (textFeedback.getSiblings().isEmpty()) {
            // This field has no comments describing it
            MutableComponent feedback = Component.translatable(
                "config2brigadier.command.edit.missing_description",
                attributeName
            ).withStyle(ChatFormatting.LIGHT_PURPLE);
            textFeedback.append(feedback);
        }

        return textFeedback;
    }

    /**
     * Generates text information for field and sends it to command executor.
     *
     * @param context   command executor.
     * @param parent    parent object that contains the attribute. Used to get current attribute value.
     * @param attribute field to generate description for.
     * @param commandPrefix command prefix for clickable actions.
     * @return 1 as success for command execution.
     */
    @ApiStatus.Internal
    default int generateFieldInfo(
        CommandContext<CommandSourceStack> context,
        Object parent,
        Field attribute,
        String commandPrefix
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
        MutableComponent fieldDesc = Component.literal("").append(
            this.generateFieldDescription(parent, attribute).withStyle(
                ChatFormatting.ITALIC
            )
        );
        fieldDesc.withStyle(ChatFormatting.RESET);

        // Default value
        String defaultOption = "";
        if (attribute.isAnnotationPresent(BrigadierDescription.class)) {
            defaultOption = attribute
                .getAnnotation(BrigadierDescription.class)
                .defaultOption();

            if (!defaultOption.isEmpty()) {
                final String finalDefaultOption = defaultOption;
                MutableComponent defaultValueComponent = Component.literal(
                    defaultOption
                )
                    .withStyle(ChatFormatting.DARK_GREEN)
                    .withStyle(style ->
                        style
                            .withHoverEvent(
                                new HoverEvent.ShowText(
                                    Component.literal(finalDefaultOption)
                                )
                            )
                            .withClickEvent(
                                new ClickEvent.SuggestCommand(
                                    finalDefaultOption
                                )
                            )
                    );
                fieldDesc
                    .append("\n")
                    .append(
                        Component.translatable(
                            "editGamerule.default",
                            defaultValueComponent
                        )
                    )
                    .withStyle(ChatFormatting.GRAY);
            }
        }

        try {
            attribute.setAccessible(true);
            Object val = attribute.get(parent);
            if (!attribute.getType().isMemberClass()) {
                if (val instanceof Map<?, ?> map) {
                    fieldDesc
                        .append("\n")
                        .append(
                            Component.translatable("config2brigadier.command.edit.values").withStyle(
                                ChatFormatting.GRAY
                            )
                        );

                    ParameterizedType mapType =
                        (ParameterizedType) attribute.getGenericType();
                    Type valType = mapType.getActualTypeArguments()[1];
                    boolean complex =
                        valType instanceof Class<?> valClass &&
                        !isSimple(valClass);

                    map.forEach((k, v) -> {
                        String keyStr =
                            k instanceof String
                                ? (String) k
                                : String.valueOf(k);
                        String valStr = isSimple(v.getClass())
                            ? String.valueOf(v)
                            : MSG_GSON.toJson(v);

                        MutableComponent line = Component.literal(
                            "\n" + keyStr + " -> " + valStr + " "
                        ).withStyle(ChatFormatting.WHITE);

                        if (complex) {
                            MutableComponent entry = Component.literal("[+]")
                                .withStyle(ChatFormatting.AQUA)
                                .withStyle(style ->
                                    style
                                        .withClickEvent(
                                            new ClickEvent.SuggestCommand(
                                                "/" +
                                                    commandPrefix +
                                                    " entry " +
                                                    (k instanceof String
                                                        ? "\"" + k + "\""
                                                        : keyStr) +
                                                    " "
                                            )
                                        )
                                        .withHoverEvent(
                                            new HoverEvent.ShowText(
                                                Component.literal(
                                                    "Manage entry fields"
                                                )
                                            )
                                        )
                                );
                            line.append(entry).append(" ");
                        }

                        MutableComponent edit = Component.literal("[E]")
                            .withStyle(ChatFormatting.YELLOW)
                            .withStyle(style ->
                                style
                                    .withClickEvent(
                                        new ClickEvent.SuggestCommand(
                                            "/" +
                                                commandPrefix +
                                                " set " +
                                                (k instanceof String
                                                    ? "\"" + k + "\""
                                                    : keyStr) +
                                                " "
                                        )
                                    )
                                    .withHoverEvent(
                                        new HoverEvent.ShowText(
                                            Component.translatable(
                                                "selectWorld.edit"
                                            )
                                        )
                                    )
                            );

                        MutableComponent remove = Component.literal("[X]")
                            .withStyle(ChatFormatting.RED)
                            .withStyle(style ->
                                style
                                    .withClickEvent(
                                        new ClickEvent.SuggestCommand(
                                            "/" +
                                                commandPrefix +
                                                " remove " +
                                                (k instanceof String
                                                    ? "\"" + k + "\""
                                                    : keyStr)
                                        )
                                    )
                                    .withHoverEvent(
                                        new HoverEvent.ShowText(
                                            Component.translatable(
                                                "selectWorld.delete"
                                            )
                                        )
                                    )
                            );

                        line
                            .append(edit)
                            .append(
                                Component.literal(" | ").withStyle(
                                    ChatFormatting.GRAY
                                )
                            )
                            .append(remove);
                        fieldDesc.append(line);
                    });
                } else if (val instanceof List<?> list) {
                    fieldDesc
                        .append("\n")
                        .append(
                            Component.translatable("config2brigadier.command.edit.values").withStyle(
                                ChatFormatting.GRAY
                            )
                        );

                    ParameterizedType listType =
                        (ParameterizedType) attribute.getGenericType();
                    Type valType = listType.getActualTypeArguments()[0];
                    boolean complex =
                        valType instanceof Class<?> valClass &&
                        !isSimple(valClass);

                    for (int i = 0; i < list.size(); i++) {
                        Object v = list.get(i);
                        String valStr = isSimple(v.getClass())
                            ? String.valueOf(v)
                            : MSG_GSON.toJson(v);
                        final int finalI = i;

                        MutableComponent line = Component.literal(
                            "\n" + (i + 1) + ". " + valStr + " "
                        ).withStyle(ChatFormatting.WHITE);

                        if (complex) {
                            MutableComponent entry = Component.literal("[+]")
                                .withStyle(ChatFormatting.AQUA)
                                .withStyle(style ->
                                    style
                                        .withClickEvent(
                                            new ClickEvent.SuggestCommand(
                                                "/" +
                                                    commandPrefix +
                                                    " entry " +
                                                    finalI +
                                                    " "
                                            )
                                        )
                                        .withHoverEvent(
                                            new HoverEvent.ShowText(
                                                Component.literal(
                                                    "Manage entry fields"
                                                )
                                            )
                                        )
                                );
                            line.append(entry).append(" ");
                        }

                        MutableComponent edit = Component.literal("[E]")
                            .withStyle(ChatFormatting.YELLOW)
                            .withStyle(style ->
                                style
                                    .withClickEvent(
                                        new ClickEvent.SuggestCommand(
                                            "/" +
                                                commandPrefix +
                                                " edit " +
                                                finalI +
                                                " "
                                        )
                                    )
                                    .withHoverEvent(
                                        new HoverEvent.ShowText(
                                            Component.translatable(
                                                "selectWorld.edit"
                                            )
                                        )
                                    )
                            );

                        MutableComponent remove = Component.literal("[X]")
                            .withStyle(ChatFormatting.RED)
                            .withStyle(style ->
                                style
                                    .withClickEvent(
                                        new ClickEvent.SuggestCommand(
                                            "/" +
                                                commandPrefix +
                                                " remove " +
                                                valStr
                                        )
                                    )
                                    .withHoverEvent(
                                        new HoverEvent.ShowText(
                                            Component.translatable(
                                                "selectWorld.delete"
                                            )
                                        )
                                    )
                            );

                        line
                            .append(edit)
                            .append(
                                Component.literal(" | ").withStyle(
                                    ChatFormatting.GRAY
                                )
                            )
                            .append(remove);
                        fieldDesc.append(line);
                    }
                } else if (val instanceof Set<?> set) {
                    fieldDesc
                        .append("\n")
                        .append(
                            Component.translatable("config2brigadier.command.edit.values").withStyle(
                                ChatFormatting.GRAY
                            )
                        );
                    for (Object v : set) {
                        String valStr = isSimple(v.getClass())
                            ? String.valueOf(v)
                            : MSG_GSON.toJson(v);

                        MutableComponent line = Component.literal(
                            "\n - " + valStr + " "
                        ).withStyle(ChatFormatting.WHITE);

                        MutableComponent remove = Component.literal("[X]")
                            .withStyle(ChatFormatting.RED)
                            .withStyle(style ->
                                style
                                    .withClickEvent(
                                        new ClickEvent.SuggestCommand(
                                            "/" +
                                                commandPrefix +
                                                " remove " +
                                                valStr
                                        )
                                    )
                                    .withHoverEvent(
                                        new HoverEvent.ShowText(
                                            Component.translatable(
                                                "selectWorld.delete"
                                            )
                                        )
                                    )
                            );

                        line.append(remove);
                        fieldDesc.append(line);
                    }
                } else {
                    String value =
                        val != null
                            ? (isSimple(val.getClass())
                                  ? val.toString()
                                  : MSG_GSON.toJson(val))
                            : "null";
                    MutableComponent valueComponent = Component.literal(value)
                        .withStyle(ChatFormatting.GREEN)
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(style ->
                            style
                                .withHoverEvent(
                                    new HoverEvent.ShowText(
                                        Component.literal(value)
                                    )
                                )
                                .withClickEvent(
                                    new ClickEvent.SuggestCommand(value)
                                )
                        );

                    if (
                        !defaultOption.isEmpty() && !defaultOption.equals(value)
                    ) {
                        // This value is modified
                        valueComponent.append(
                            Component.literal(" (*)").withStyle(
                                ChatFormatting.YELLOW
                            )
                        );
                    }

                    fieldDesc
                        .append("\n")
                        .append(
                            Component.translatable("options.fullscreen.current")
                                .append(": ")
                                .append(valueComponent)
                                .withStyle(ChatFormatting.GRAY)
                        );
                }
            }
        } catch (Exception e) {
            getLogger(MOD_ID).severe(
                "[Config2Brigadier] Problem occurred when trying to get field value: " +
                    e.getMessage()
            );
        }

        // Field type
        if (!attribute.getType().isMemberClass()) {
            var type = Component.literal(
                attribute.getType().getSimpleName()
            ).withStyle(ChatFormatting.AQUA);
            fieldDesc
                .append("\n")
                .append(Component.translatable("gui.entity_tooltip.type", type))
                .withStyle(ChatFormatting.GRAY);
        }

        context
            .getSource()
            .sendSuccess(() -> fieldDesc.withStyle(ChatFormatting.GOLD), false);

        return 1;
    }
}
