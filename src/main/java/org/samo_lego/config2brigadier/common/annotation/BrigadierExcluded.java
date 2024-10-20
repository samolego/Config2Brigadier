package org.samo_lego.config2brigadier.common.annotation;

import org.samo_lego.config2brigadier.common.IBrigadierConfigurator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * Indicates that this field should be excluded from generated command.
 * <p>
 * Static fields are automatically excluded. (To change this behaviour,
 * please override {@link IBrigadierConfigurator#shouldExclude(Field)})
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BrigadierExcluded {
}