package de.alexander.brand.annobuilder.prozessor;

import de.alexander.brand.annobuilder.annotation.Builder;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * enthält alle wichtigen Informationen zum Generieren eines Builders
 */
class BuilderParameter {
    protected String className = "";
    protected String objectClassName = "java.lang.Object";
    protected Builder builder;
    protected final Set<VariableElement> publicArgs = new HashSet<>();
    protected final Map<VariableElement, ExecutableElement> privateArgsToSetter = new HashMap<>();
    protected final Map<ExecutableElement, VariableElement> unmatchedSetters = new HashMap<>();
    protected final Set<ExecutableElement> constructors = new HashSet<>();
    protected final Set<VariableElement> finalArgs = new HashSet<>();

    @Override
    public String toString() {
        return "BuilderParameter{" +
                "className='" + className + '\'' +
                ", builder=" + builder +
                ", publicArgs=" + publicArgs +
                ", privateArgsToSetter=" + privateArgsToSetter +
                ", unmatchedSetters=" + unmatchedSetters +
                ", constructors=" + constructors +
                ", finalArgs=" + finalArgs +
                '}';
    }
}
