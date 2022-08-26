package de.alexander.brand.annobuilder.prozessor.search;

import com.squareup.javapoet.ClassName;
import de.alexander.brand.annobuilder.annotation.Builder;
import de.alexander.brand.annobuilder.prozessor.AnnotationProcessingException;
import de.alexander.brand.annobuilder.prozessor.ConfigProzessor;
import de.alexander.brand.annobuilder.prozessor.TypeUtils;
import de.alexander.brand.annobuilder.prozessor.ValueHandlingMode;
import lombok.*;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

import static de.alexander.brand.annobuilder.annotation.Builder.CONFIG_ID;

/**
 * Generiert {@link SearchParameter}
 */
public class BuilderElementVisitor implements ElementVisitor<Set<SearchParameter>, SearchParameter> {

    private final ConfigProzessor configProzessor;

    private final Types typeUtils;

    private final Elements elements;


    public BuilderElementVisitor(ConfigProzessor configProzessor, Types typeUtils, Elements elements) {

        this.configProzessor = configProzessor;
        this.typeUtils = typeUtils;
        this.elements = elements;
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
            SearchParameter aktive = new SearchParameter(ClassName.get(e), annotation.finalInBuildFunktion(), getPackageString(annotation, e.getQualifiedName().toString().substring(0, e.getQualifiedName().toString().lastIndexOf('.'))), annotation.mode());

            Setter setterAnnotation = e.getAnnotation(Setter.class);
            aktive.setSetterAnnotation(setterAnnotation != null);

            NoArgsConstructor noArgsConstructor = e.getAnnotation(NoArgsConstructor.class);
            if (noArgsConstructor != null) {
                aktive.getConstructors().add(new InferredConstructor(e, typeUtils, elements, false));
            }

            RequiredArgsConstructor requiredArgsConstructor = e.getAnnotation(RequiredArgsConstructor.class);
            if (requiredArgsConstructor != null) {
                aktive.setRequiredArgs(new InferredConstructor(e, typeUtils, elements, false));
            }

            AllArgsConstructor allArgsConstructor = e.getAnnotation(AllArgsConstructor.class);
            if (allArgsConstructor != null){
                aktive.setAllArgs(new InferredConstructor(e, typeUtils, elements, false));
            }

            e.getEnclosedElements().forEach(element -> set.addAll(element.accept(this, aktive)));
            set.add(aktive);
        }
        return set;
    }

    private String getPackageString(Builder annotation, String packageName) {
        String configuredBasePackage = Builder.CONFIG_ID.equals(annotation.packageString()) ? configProzessor.getPackageString() : annotation.packageString();


        //Replace other
        Map<String, String> packageReplacements = configProzessor.getPackageNameReplacements();
        for (Map.Entry<String, String> entry : packageReplacements.entrySet()) {
            configuredBasePackage = configuredBasePackage.replace(entry.getKey(), entry.getValue());
        }

        //Replace $PACKAGE
        String replace = packageName.replace(configProzessor.getPackageMask() + ".", "");
        configuredBasePackage = configuredBasePackage.replace("$PACKAGE", replace.replace(configProzessor.getPackageMask(), ""));

        //Remove Trailing .
        if (configuredBasePackage.endsWith(".")) {
            configuredBasePackage = configuredBasePackage.substring(0, configuredBasePackage.length() - 1);
        }
        return configuredBasePackage;
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
            if (e.getAnnotation(Setter.class) != null || searchParameter.isSetterAnnotation()) {
                searchVariable.setSetMethod("set" + TypeUtils.toUpperCaseCamelCase(searchVariable.getVariableName()));
                System.out.println("setterAnnotation:" + e);
            }
        }

        if (e.getAnnotation(Builder.CollectionProperties.class) != null) {
            Builder.CollectionProperties collectionProperties = e.getAnnotation(Builder.CollectionProperties.class);
            ClassName type = TypeUtils.getGenericClassName(e);

            String methodSuffix = CONFIG_ID.equals(collectionProperties.addMethodSuffix()) ? configProzessor.getAddMethodSuffix(TypeUtils.toUpperCaseCamelCase(e.getSimpleName().toString())) : collectionProperties.addMethodSuffix();
            String parameterName = CONFIG_ID.equals(collectionProperties.parameterName()) ? type.simpleName().toLowerCase(Locale.ROOT) : collectionProperties.parameterName();

            System.out.println("methodSuffix:" + methodSuffix);
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

        } else if (!(searchVariable.getTypeName().isPrimitive() || searchVariable.getTypeName().isBoxedPrimitive()) && (configProzessor.getCollectionConstructorMap().containsKey(TypeUtils.toClassName(searchVariable.getTypeName().toString())) || TypeUtils.isCollection((TypeElement) typeUtils.asElement(e.asType()), typeUtils, configProzessor.getCollectionConstructorMap()))) {
            ClassName type = TypeUtils.getGenericClassName(e);

            String methodSuffix = configProzessor.getAddMethodSuffix(TypeUtils.toUpperCaseCamelCase(e.getSimpleName().toString()));
            String parameterName = type.simpleName().toLowerCase(Locale.ROOT);
            ClassName key = TypeUtils.toClassName(e.asType().toString());

            if (key != null) {
                if (typeUtils.asElement(e.asType()).getModifiers().contains(Modifier.ABSTRACT)) {
                    searchVariable.setCollectionArgs(new SearchVariable.CollectionArgs("add" + methodSuffix, parameterName, configProzessor.getCollectionConstructorMap().get(key), false, type));
                } else {
                    searchVariable.setCollectionArgs(new SearchVariable.CollectionArgs("add" + methodSuffix, parameterName, key, false, type));
                }
            }
        } else {

            System.out.println("TypeUtils isCollection:" + TypeUtils.isCollection((TypeElement) typeUtils.asElement(e.asType()), typeUtils, configProzessor.getCollectionConstructorMap()) + " " + e);
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

        //Lombok
        if (searchParameter.getRequiredArgs() != null && (modifiers.contains(Modifier.FINAL) || e.getAnnotation(NonNull.class) != null) && e.getConstantValue() == null) {
            searchParameter.getRequiredArgs().addParameter(e);
        }

        if (searchParameter.getAllArgs() != null) {
            searchParameter.getAllArgs().addParameter(e);
            System.out.println("AllArgsParameterzahl:"+searchParameter.getAllArgs().getParameters().size());
        }

        searchParameter.getSearchVariables().add(searchVariable);
        return set;
    }

    @Override
    public Set<SearchParameter> visitExecutable(ExecutableElement e, SearchParameter searchParameter) {
        if (searchParameter == null) return new HashSet<>();
        if (e.getKind() == ElementKind.CONSTRUCTOR) {

            System.out.println("returnValue:" + e.getReturnType());
            searchParameter.getConstructors().add(e);
        } else if (e.getKind() == ElementKind.METHOD) {
            if (e.getParameters().size() != 1) {
                if (e.getAnnotation(Builder.SetMethod.class) != null)
                    throw new AnnotationProcessingException("Methode mit SetMethod-Annotation hat nicht einen Parameter:" + e.getSimpleName() + " in " + e.getEnclosingElement().getSimpleName());
                return new HashSet<>();
            }

            if (e.getModifiers().contains(Modifier.STATIC) || !e.getModifiers().contains(Modifier.PUBLIC)) {
                if (e.getAnnotation(Builder.SetMethod.class) != null)
                    throw new AnnotationProcessingException("Methode mit SetMethod-Annotation hat falsche Modifier:" + e.getSimpleName() + " in " + e.getEnclosingElement().getSimpleName());
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
