package de.alexander.brand.annobuilder.prozessor.search;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Eine Implementierung des {@link ExecutableElement} für Konstruktoren, welche durch Annotations erkannt wurden.
 *
 * @see lombok.NoArgsConstructor
 * @see lombok.RequiredArgsConstructor
 * @see lombok.AllArgsConstructor
 */
public class InferredConstructor implements ExecutableElement {

    private final TypeMirror returnType;
    private final Types types;
    private final boolean isDefault;
    private final TypeElement parent;
    private final List<VariableElement> parameter;

    private final TypeMirror noneType;
    private final Name name;
    private DeclaredType declaredType;

    public InferredConstructor(TypeElement parent, Types types, Elements elements, boolean isDefault, VariableElement... parameter) {
        this.returnType = types.getNoType(TypeKind.VOID);
        this.types = types;
        this.isDefault = isDefault;
        this.parent = parent;
        this.parameter = Lists.newArrayList(parameter);
        noneType = types.getNoType(TypeKind.NONE);
        name = elements.getName("<init>");
        declaredType = types.getDeclaredType(parent, Arrays.stream(parameter).map(VariableElement::asType).toArray(TypeMirror[]::new));
    }

    @Override
    public TypeMirror asType() {
        return declaredType;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.CONSTRUCTOR;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Sets.newHashSet(Modifier.PUBLIC);
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    public TypeMirror getReturnType() {
        return returnType;
    }

    @Override
    public List<? extends VariableElement> getParameters() {
        return parameter;
    }

    @Override
    public TypeMirror getReceiverType() {
        return noneType;
    }

    @Override
    public boolean isVarArgs() {
        return false;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public List<? extends TypeMirror> getThrownTypes() {
        return new ArrayList<>();
    }

    @Override
    public AnnotationValue getDefaultValue() {
        return null;
    }

    @Override
    public Element getEnclosingElement() {
        return parent;
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return new ArrayList<>();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return new ArrayList<>();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return null;
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        return null;
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitExecutable(this, p);
    }

    @Override
    public Name getSimpleName() {
       return name;
    }

    public void addParameter(VariableElement variableElement) {
        parameter.add(variableElement);
    }
}
