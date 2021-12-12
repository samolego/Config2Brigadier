package org.samo_lego.config2brigadier.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds description to the option, which is seen if user
 * executes the config edit command but doesn't provide a value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BrigadierDescription {

    /**
     * Description for the field.
     * @return description of field that will be shown in-game.
     */
    String value() default "";

    /**
     * Default option / value for the field.
     * @return the default field value (the one after config object is generated).
     */
    String defaultOption() default "";
}
