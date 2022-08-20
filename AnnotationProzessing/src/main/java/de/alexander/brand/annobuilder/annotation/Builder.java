package de.alexander.brand.annobuilder.annotation;

import de.alexander.brand.annobuilder.prozessor.ValueHandlingMode;

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

    /**
     * Wenn true wird der ValueHandlingMode von {@link ValueHandlingMode#ALWAYS_SET} auf {@link ValueHandlingMode#ONLY_SET_WHEN_SET} geändert.
     * Kann auch von der Konfiguration auf true gesetzt werden.
     * {@link ValueHandling} kann die Einstellung für einzelnde Felder überschreiben
     * @return true, wenn der ValueHandlingMode geändert werden soll
     */
    boolean onlySetWhenSet() default false;

    /**
     * Deklariert ein Feld als Parameter.
     * Enthält Konfigurationen
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface CollectionProperties {
        String addMethodSuffix() default CONFIG_ID;
        String parameterName() default CONFIG_ID;
        Class<?> implementation() default Collection.class;
        boolean hasWithMethod() default false;
    }

    /**
     * Sorgt dafür ,dass das Feld in der Build-Methode als Parameter vorkommt.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface IncludeInBuildMethod {

    }

    /**
     * Beachtet das Feld nicht für die Builder-Generation
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Exclude {

    }

    /**
     * Das Feld wird automatisch im Konstruktor als Parameter genutzt
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface IncludeInConstructor {

    }

    /**
     * Soll das Feld initialisiert werden, wird der Angegebene String als Anweisung eingefügt.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Provider {
        String providerCode();
    }

    /**
     * Gibt an wie mit dem Wert des Feldes umgegangen werden soll.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    @interface ValueHandling {
        ValueHandlingMode mode();
    }

    /**
     * Kann an ein Feld oder eine Methode gesetzt werden.
     * {@link SetMethod#link()} gibt entweder den Namen des Feldes an, für das die Methode als Setter genutzt werden soll oder den Namen des Setters.
     * Die Methode darf nur den Parameter haben.
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.SOURCE)
    @interface SetMethod {
        String link();
    }






}
