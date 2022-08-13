package de.alexander.brand.annobuilder.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Builder {

    String CONFIG_ID = "$Config";

    boolean finalInBuildFunktion() default true;

    String packageString() default CONFIG_ID;

    public @interface CollectionProperties {
        String addMethodSuffix() default CONFIG_ID;
        String parameterName() default CONFIG_ID;
        Class<?> implementation() default Collection.class;
    }
}
