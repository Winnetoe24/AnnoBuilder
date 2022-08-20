package de.alexander.brand.annobuilder.prozessor.search;

import de.alexander.brand.annobuilder.annotation.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * enthält alle wichtigen Informationen zum Generieren eines Builders
 */
@Getter
public class BuilderParameter {
    /**
     * Name für zu erstellende Builder Klasse
     */
    protected String className = "";
    /**
     * Full-Qualified Name der Klasse, zu der ein Builder erstellt werden soll
     */
    protected String objectClassName = "java.lang.Object";
    /**
     * Die Annotation zu der der Builder erstellt wird
     */
    protected Builder builder;

    /**
     * Die public-Argumente, welche ohne Setter gesetzt werden können
     */
    protected final Set<VariableElement> publicArgs = new HashSet<>();

    /**
     * Argumente die über einen Setter gesetzt werden sollen
     */
    protected final Map<VariableElement, ExecutableElement> argsToSetter = new HashMap<>();
    /**
     * Die Methoden die gefunden wurden und möglicherweise als Setter genutzt werden können
     * => Startet mit "set", hat Return Type void hat einen Parameter
     */
    protected final Map<ExecutableElement, VariableElement> unmatchedSetters = new HashMap<>();
    /**
     * Alle Konstruktoren die gefunden wurden
     */
    protected final Set<ExecutableElement> constructors = new HashSet<>();

    /**
     * Alle Argumente die als Parameter im Konstruktor gesetzt werden sollen
     * => Final Args oder @IncludeInBuildMethod
     */
    protected final Set<VariableElement> constructorArgs = new HashSet<>();

    @Override
    public String toString() {
        return "BuilderParameter{" +
                "className='" + className + '\'' +
                ", builder=" + builder +
                ", publicArgs=" + publicArgs +
                ", argsToSetter=" + argsToSetter +
                ", unmatchedSetters=" + unmatchedSetters +
                ", constructors=" + constructors +
                ", constructorArgs=" + constructorArgs +
                '}';
    }
}
