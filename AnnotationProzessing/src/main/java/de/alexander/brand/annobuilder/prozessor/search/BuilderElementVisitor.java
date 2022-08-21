package de.alexander.brand.annobuilder.prozessor.search;

import com.squareup.javapoet.ClassName;
import de.alexander.brand.annobuilder.annotation.Builder;
import de.alexander.brand.annobuilder.prozessor.AnnotationProcessingException;
import de.alexander.brand.annobuilder.prozessor.ConfigProzessor;
import de.alexander.brand.annobuilder.prozessor.TypeUtils;
import de.alexander.brand.annobuilder.prozessor.ValueHandlingMode;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static de.alexander.brand.annobuilder.annotation.Builder.CONFIG_ID;

/**
 * Generiert {@link SearchParameter}
 */
public class BuilderElementVisitor implements ElementVisitor<Set<SearchParameter>, SearchParameter> {

    private final ConfigProzessor configProzessor;

    private final Types typeUtils;

    public BuilderElementVisitor(ConfigProzessor configProzessor, Types typeUtils) {

        this.configProzessor = configProzessor;
        this.typeUtils = typeUtils;
    }

    @Override
    public Set<SearchParameter> visit(Element e, SearchParameter searchParameter) {
        if (e instanceof PackageElement) {
            return visitPackage((PackageElement) e, searchParameter);
        }
        if (e instanceof TypeElement) {
            return visitType((TypeElement) e, searchParameter);
        }
        if (e instanceof VariableElement) {
            return visitVariable((VariableElement) e, searchParameter);
        }
        if (e instanceof ExecutableElement) {
            return visitExecutable((ExecutableElement) e, searchParameter);
        }
        if (e instanceof TypeParameterElement) {
            return visitTypeParameter((TypeParameterElement) e, searchParameter);
        }
        return visitUnknown(e, searchParameter);
    }

    @Override
    public Set<SearchParameter> visitPackage(PackageElement e, SearchParameter searchParameter) {
        Set<SearchParameter> set = new HashSet<>();
        set.add(searchParameter);
        e.getEnclosedElements().forEach(element -> set.addAll(element.accept(this, searchParameter)));
        return set;
    }

    @Override
    public Set<SearchParameter> visitType(TypeElement e, SearchParameter searchParameter) {
        Set<SearchParameter> set = new HashSet<>();
        if (e.getKind().isClass()) {
            Builder annotation = e.getAnnotation(Builder.class);
            if (annotation == null) return set;
            SearchParameter aktive = new SearchParameter(ClassName.get(e), annotation.finalInBuildFunktion(), annotation.packageString(), annotation.mode());
            e.getEnclosedElements().forEach(element -> set.addAll(element.accept(this, aktive)));
            set.add(aktive);
        }
        return set;
    }

    @Override
    public Set<SearchParameter> visitVariable(VariableElement e, SearchParameter searchParameter) {
        Set<SearchParameter> set = new HashSet<>();
        if (searchParameter == null) {
            return set;
        }
        if (e.getAnnotation(Builder.Exclude.class) != null) {
            return set;
        }
        Set<Modifier> modifiers = e.getModifiers();
        if (modifiers.contains(Modifier.STATIC)) {
            return set;
        }

        SearchVariable searchVariable = new SearchVariable(ClassName.get(e.asType()), e.getSimpleName().toString());


        if (modifiers.contains(Modifier.FINAL)) {
            if (searchParameter.isFinalInBuildFunktion()) {
                searchVariable.setIncludeInBuildMethod(true);
            }
            searchVariable.setIncludeInConstructor(true);
        }
        if (e.getAnnotation(Builder.IncludeInBuildMethod.class) != null) {
            searchVariable.setIncludeInBuildMethod(true);
        }
        if (e.getAnnotation(Builder.IncludeInConstructor.class) != null) {
            searchVariable.setIncludeInConstructor(true);
        }
        if (e.getAnnotation(Builder.SetMethod.class) != null) {
            Builder.SetMethod setMethod = e.getAnnotation(Builder.SetMethod.class);
            searchVariable.setSetMethod(setMethod.link());
        } else if (!modifiers.contains(Modifier.PUBLIC)) {
            searchVariable.setSetMethod("");
        }

        if (e.getAnnotation(Builder.CollectionProperties.class) != null) {
            Builder.CollectionProperties collectionProperties = e.getAnnotation(Builder.CollectionProperties.class);
            String typeNameString = e.asType().toString();
            int i = typeNameString.indexOf('<');
            typeNameString = typeNameString.substring(i + 1, typeNameString.length() - 1);
            ClassName type = TypeUtils.toClassName(typeNameString);

            String methodSuffix = CONFIG_ID.equals(collectionProperties.addMethodSuffix()) ? configProzessor.getAddMethodSuffix(e.getSimpleName().toString()) : collectionProperties.addMethodSuffix();
            String parameterName = CONFIG_ID.equals(collectionProperties.parameterName()) ? type.simpleName().toLowerCase(Locale.ROOT) : collectionProperties.parameterName();


            ClassName implementation;
            try {
                if (collectionProperties.implementation() != Collection.class) {
                    implementation = ClassName.get(collectionProperties.implementation());
                } else {
                    implementation = configProzessor.getCollectionConstructorMap().get(ClassName.get(e.asType()));
                }
            } catch (MirroredTypeException mte) {
                DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                implementation = ClassName.get(classTypeElement);
                if (implementation.equals(ClassName.get(Collection.class))) {
                    implementation = configProzessor.getCollectionConstructorMap().get(ClassName.get(e.asType()));
                }
            }


            searchVariable.setCollectionArgs(new SearchVariable.CollectionArgs("add" + methodSuffix, parameterName, implementation, collectionProperties.hasWithMethod(), type));
        } else if (!(searchVariable.getTypeName().isPrimitive() || searchVariable.getTypeName().isBoxedPrimitive()) && configProzessor.getCollectionConstructorMap().containsKey(TypeUtils.toClassName(searchVariable.getTypeName().toString()))) {
            String typeNameString = e.asType().toString();
            int i = typeNameString.indexOf('<');
            typeNameString = typeNameString.substring(i + 1, typeNameString.length() - 1);
            ClassName type = TypeUtils.toClassName(typeNameString);


            String methodSuffix = configProzessor.getAddMethodSuffix(e.getSimpleName().toString());
            String parameterName = type.simpleName().toLowerCase(Locale.ROOT);
            ClassName implementation = typeUtils.asElement(e.asType()).getModifiers().contains(Modifier.ABSTRACT) ? configProzessor.getCollectionConstructorMap().get((ClassName) ClassName.get(e.asType())) : (ClassName) ClassName.get(e.asType());
            searchVariable.setCollectionArgs(new SearchVariable.CollectionArgs("add" + methodSuffix, parameterName, implementation, false, type));
        }

        if (e.getAnnotation(Builder.Provider.class) != null) {
            searchVariable.setProvider(e.getAnnotation(Builder.Provider.class).providerCode());
        } else {
            searchVariable.setProvider(configProzessor.getDefaultProvider());
        }

        searchVariable.setValueHandling(configProzessor.getValueHandlingMode());
        if (searchParameter.getValueHandlingMode() != ValueHandlingMode.ALWAYS_SET) {
            searchVariable.setValueHandling(searchParameter.getValueHandlingMode());
        }
        if (e.getAnnotation(Builder.ValueHandling.class) != null) {
            Builder.ValueHandling valueHandling = e.getAnnotation(Builder.ValueHandling.class);
            searchVariable.setValueHandling(valueHandling.mode());
        }

        return set;
    }

    @Override
    public Set<SearchParameter> visitExecutable(ExecutableElement e, SearchParameter searchParameter) {
        if (searchParameter == null) return new HashSet<>();
        if (e.getKind() == ElementKind.CONSTRUCTOR) {
            searchParameter.getConstructors().add(e);
        } else if (e.getKind() == ElementKind.METHOD) {
            if (e.getParameters().size() != 1) {
                if (e.getAnnotation(Builder.SetMethod.class) != null) throw new AnnotationProcessingException("Methode mit SetMethod-Annotation hat nicht einen Parameter:"+e.getSimpleName()+" in "+e.getEnclosingElement().getSimpleName());
                return new HashSet<>();
            }

            if (e.getModifiers().contains(Modifier.STATIC) || !e.getModifiers().contains(Modifier.PUBLIC)) {
                if (e.getAnnotation(Builder.SetMethod.class) != null) throw new AnnotationProcessingException("Methode mit SetMethod-Annotation hat falsche Modifier:"+e.getSimpleName()+" in "+e.getEnclosingElement().getSimpleName());
                return new HashSet<>();
            }

            searchParameter.getPossibleSetters().add(e);
        }

        return new HashSet<>();
    }

    @Override
    public Set<SearchParameter> visitTypeParameter(TypeParameterElement e, SearchParameter searchParameter) {
        System.out.println("TypeParameter:" + e);
        return new HashSet<>();
    }

    @Override
    public Set<SearchParameter> visitUnknown(Element e, SearchParameter searchParameter) {
        return new HashSet<>();
    }

}
