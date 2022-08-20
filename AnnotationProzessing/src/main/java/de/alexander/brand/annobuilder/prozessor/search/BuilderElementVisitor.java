package de.alexander.brand.annobuilder.prozessor.search;

import de.alexander.brand.annobuilder.annotation.Builder;

import javax.lang.model.element.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static de.alexander.brand.annobuilder.prozessor.TypeUtils.conatainsAllParamter;
import static de.alexander.brand.annobuilder.prozessor.TypeUtils.getClassName;

/**
 * Generiert {@link BuilderParameter}
 */
public class BuilderElementVisitor implements ElementVisitor<Set<BuilderParameter>, BuilderParameter> {

    @Override
    public Set<BuilderParameter> visit(Element e, BuilderParameter builderParameter) {
        if (e instanceof PackageElement) {
            return visitPackage((PackageElement) e, builderParameter);
        }
        if (e instanceof TypeElement) {
            return visitType((TypeElement) e, builderParameter);
        }
        if (e instanceof VariableElement) {
            return visitVariable((VariableElement) e, builderParameter);
        }
        if (e instanceof ExecutableElement) {
            return visitExecutable((ExecutableElement) e, builderParameter);
        }
        if (e instanceof TypeParameterElement) {
            return visitTypeParameter((TypeParameterElement) e, builderParameter);
        }
        return visitUnknown(e, builderParameter);
    }

    @Override
    public Set<BuilderParameter> visitPackage(PackageElement e, BuilderParameter builderParameter) {
        Set<BuilderParameter> set = new HashSet<>();
        set.add(builderParameter);
        e.getEnclosedElements().forEach(element -> set.addAll(element.accept(this, builderParameter)));
        return set;
    }

    @Override
    public Set<BuilderParameter> visitType(TypeElement e, BuilderParameter builderParameter) {
        Set<BuilderParameter> set = new HashSet<>();
        if (e.getKind().isClass()) {
            Builder annotation = e.getAnnotation(Builder.class);
            BuilderParameter aktive = annotation == null ? null : new BuilderParameter();

            if (aktive != null) {
                String charSequence = getClassName(e);
                aktive.objectClassName = e.getQualifiedName().toString();
                aktive.className = charSequence + "Builder";
                aktive.builder = annotation;
                e.getEnclosedElements().forEach(element -> set.addAll(element.accept(this, aktive)));
            }
            set.add(aktive);
        }
        return set;
    }

    @Override
    public Set<BuilderParameter> visitVariable(VariableElement e, BuilderParameter builderParameter) {
        Set<BuilderParameter> set = new HashSet<>();
        if (builderParameter == null) {
            return set;
        }
        Set<Modifier> modifiers = e.getModifiers();
        if (modifiers.contains(Modifier.STATIC)) {
            return set;
        }

        if (modifiers.contains(Modifier.FINAL)) {
            builderParameter.constructors.removeIf(executableElement -> !executableElement.getParameters().contains(e));
            builderParameter.constructorArgs.add(e);
        }
        if (modifiers.contains(Modifier.PUBLIC)) {
            builderParameter.publicArgs.add(e);
        } else {
            if (builderParameter.unmatchedSetters.containsValue(e)) {
                ExecutableElement element = null;
                for (Map.Entry<ExecutableElement, VariableElement> entry : builderParameter.unmatchedSetters.entrySet()) {
                    ExecutableElement executableElement = entry.getKey();
                    VariableElement variableElement = entry.getValue();
                    if (e.equals(variableElement)) {
                        element = executableElement;
                        break;
                    }
                }
                builderParameter.unmatchedSetters.remove(element);
                builderParameter.argsToSetter.put(e, element);
            } else {
                builderParameter.argsToSetter.put(e, null);
            }
        }
        return set;
    }

    @Override
    public Set<BuilderParameter> visitExecutable(ExecutableElement e, BuilderParameter builderParameter) {
        if (e.getKind() == ElementKind.CONSTRUCTOR) {
            if (conatainsAllParamter(e, builderParameter.constructorArgs)) {
                builderParameter.constructors.add(e);
            }
        } else if (e.getKind() == ElementKind.METHOD) {
            if (e.getParameters().size() != 1) {
                return new HashSet<>();
            }

            if (e.getModifiers().contains(Modifier.STATIC) || !e.getModifiers().contains(Modifier.PUBLIC)) {
                return new HashSet<>();
            }
            String s = e.getSimpleName().toString();
            if (s.startsWith("set")) {
                String variableName = Character.toLowerCase(s.charAt(3)) + s.substring(4);
                for (VariableElement variableElement : builderParameter.argsToSetter.keySet()) {
                    if (variableElement.getSimpleName().contentEquals(variableName) && variableElement.asType().equals(e.getParameters().get(0).asType())) {
                        builderParameter.argsToSetter.put(variableElement, e);
                    }
                }
            }
        }

        return new HashSet<>();
    }

    @Override
    public Set<BuilderParameter> visitTypeParameter(TypeParameterElement e, BuilderParameter builderParameter) {
        return new HashSet<>();
    }

    @Override
    public Set<BuilderParameter> visitUnknown(Element e, BuilderParameter builderParameter) {
        return new HashSet<>();
    }

}
