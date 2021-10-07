package org.samo_lego.config2brigadier.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * Indicates that this field should be excluded from generated command.
 *
 * Static fields are automatically excluded. (To change this behaviour,
 * please override {@link org.samo_lego.config2brigadier.IBrigadierConfigurator#shouldExclude(Field)})
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BrigadierExcluded {
}