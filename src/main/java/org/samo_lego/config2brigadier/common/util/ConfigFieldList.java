package org.samo_lego.config2brigadier.common.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.config2brigadier.common.IBrigadierConfigurator;

/**
 * Creates an object containing lists with primitives, {@link Object}s and nested {@link ConfigFieldList}s.
 */
public record ConfigFieldList(
    Field parentField,
    @Nullable Object parent,
    List<Field> booleans,
    List<Field> integers,
    List<Field> floats,
    List<Field> doubles,
    List<Field> maps,
    List<Field> lists,
    List<Field> sets,
    List<Field> objects,
    List<ConfigFieldList> nestedFields
) {
    /**
     * Generates a {@link ConfigFieldList} for selected object with recursion.
     * Supports nested values as well.
     *
     * @param parentField - field whose name will be used for the command node. If null, it will default to "edit",
     *                    as the only config object that doesn't have a field is object itself, as it's a class.
     * @param parent - object to generate {@link ConfigFieldList} for
     */
    public static ConfigFieldList populateFields(
        @Nullable Field parentField,
        Object parent,
        IBrigadierConfigurator config
    ) {
        return populateFields(parentField, parent, parent.getClass(), config);
    }

    /**
     * Generates a {@link ConfigFieldList} template for selected class with recursion.
     * @param parentField field that holds this class
     * @param clazz class to generate template for
     * @param config configurator
     * @return template
     */
    public static ConfigFieldList template(
        @Nullable Field parentField,
        Class<?> clazz,
        IBrigadierConfigurator config
    ) {
        return populateFields(parentField, null, clazz, config);
    }

    private static ConfigFieldList populateFields(
        @Nullable Field parentField,
        @Nullable Object parent,
        Class<?> clazz,
        IBrigadierConfigurator config
    ) {
        List<Field> bools = new ArrayList<>();
        List<Field> ints = new ArrayList<>();
        List<Field> floats = new ArrayList<>();
        List<Field> doubles = new ArrayList<>();
        List<Field> maps = new ArrayList<>();
        List<Field> lists = new ArrayList<>();
        List<Field> sets = new ArrayList<>();
        List<Field> objects = new ArrayList<>();
        List<ConfigFieldList> nested = new ArrayList<>();

        for (Field attribute : clazz.getFields()) {
            Class<?> type = attribute.getType();

            if (config.shouldExclude(attribute)) {
                continue;
            }

            if (type.equals(boolean.class)) {
                bools.add(attribute);
            } else if (type.equals(int.class)) {
                ints.add(attribute);
            } else if (type.equals(float.class)) {
                floats.add(attribute);
            } else if (type.equals(double.class)) {
                doubles.add(attribute);
            } else if (Map.class.isAssignableFrom(type)) {
                maps.add(attribute);
            } else if (List.class.isAssignableFrom(type)) {
                lists.add(attribute);
            } else if (Set.class.isAssignableFrom(type)) {
                sets.add(attribute);
            } else if (type.isMemberClass()) {
                // a subclass in our config
                try {
                    attribute.setAccessible(true);
                    Object childAttribute =
                        parent != null ? attribute.get(parent) : null;
                    if (parent != null && childAttribute != null) {
                        nested.add(
                            populateFields(attribute, childAttribute, config)
                        );
                    } else {
                        nested.add(template(attribute, type, config));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                objects.add(attribute);
            }
        }

        return new ConfigFieldList(
            parentField,
            parent,
            Collections.unmodifiableList(bools),
            Collections.unmodifiableList(ints),
            Collections.unmodifiableList(floats),
            Collections.unmodifiableList(doubles),
            Collections.unmodifiableList(maps),
            Collections.unmodifiableList(lists),
            Collections.unmodifiableList(sets),
            Collections.unmodifiableList(objects),
            Collections.unmodifiableList(nested)
        );
    }
}
