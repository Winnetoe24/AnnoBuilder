package de.alexander.brand.annobuilder.prozessor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import de.alexander.brand.annobuilder.annotation.Builder;
import de.alexander.brand.annobuilder.prozessor.search.BuilderElementVisitor;
import de.alexander.brand.annobuilder.prozessor.search.BuilderParameter;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.alexander.brand.annobuilder.annotation.Builder.CONFIG_ID;
import static de.alexander.brand.annobuilder.prozessor.TypeUtils.conatainsAllParamter;

@SupportedAnnotationTypes("de.alexander.brand.annobuilder.annotation.Builder")
@AutoService(Processor.class)
public class BuilderProzessor extends AbstractProcessor {

    private ConfigProzessor configProzessor;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        configProzessor = new ConfigProzessor(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> rootE = roundEnv.getRootElements();
        Set<BuilderParameter> builderParameters = new HashSet<>();
        for (Element e : rootE) {
            builderParameters.addAll(e.accept(new BuilderElementVisitor(), null));
        }


        for (BuilderParameter builderParameter : builderParameters) {
            if (builderParameter == null) continue;
            try {
                String packageString = builderParameter.getBuilder().packageString();
                if (packageString.equals(CONFIG_ID)) {
                    packageString = configProzessor.getPackageString();
                }

                Filer filer = processingEnv.getFiler();

                ClassName className = ClassName.get(packageString, builderParameter.getClassName());
                TypeSpec.Builder classBuilder = TypeSpec.classBuilder(builderParameter.getClassName());


                Set<VariableElement> variableElements = new HashSet<>();
                variableElements.addAll(builderParameter.getConstructorArgs());
                variableElements.addAll(builderParameter.getPublicArgs());
                variableElements.addAll(builderParameter.getArgsToSetter().keySet());

                for (VariableElement variableElement : variableElements) {
                    classBuilder.addField(WriteUtils.generateArgument(variableElement));
                }

                Set<VariableElement> elementsInBuildFunktion = new HashSet<>();
                elementsInBuildFunktion.addAll(builderParameter.getConstructorArgs());
                elementsInBuildFunktion.addAll(builderParameter.getArgsToSetter().entrySet().stream()
                        .filter(variableElementExecutableElementEntry -> variableElementExecutableElementEntry.getValue() == null)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()));

                variableElements.removeAll(elementsInBuildFunktion);

                //Add and WithMethods
                for (VariableElement variableElement : variableElements) {
                    boolean isCollection = false;
                    boolean isAbstract = false;
                    Builder.CollectionProperties annotation = variableElement.getAnnotation(Builder.CollectionProperties.class);
                    TypeName constructor = null;
                    if (annotation == null || annotation.implementation() == Collection.class) {
                        Set<TypeMirror> typeMirrors = new HashSet<>();
                        typeMirrors.add(variableElement.asType());
                        TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(variableElement.asType());
                        constructor = TypeUtils.getCollectionName(typeElement, processingEnv, configProzessor.getCollectionConstructorMap());
                        if (constructor != null) {
                            isCollection = true;
                        }
                        if (typeElement != null) {
                            isAbstract = typeElement.getModifiers().contains(Modifier.ABSTRACT);
                            if (!isAbstract) {
//                                constructor = ClassName.get(typeElement);
                            }
                        }

                    }

                    if (isCollection || annotation != null) {
                        try {
                            printAddMethod(classBuilder, variableElement, className, annotation, constructor, isAbstract);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        printWithMethod(classBuilder, className, variableElement);
                    }
                }

                //Build Method
                printBuildMethod(classBuilder, className, elementsInBuildFunktion, builderParameter);

                JavaFile build = JavaFile
                        .builder(packageString, classBuilder
                                .addModifiers(Modifier.PUBLIC)
                                .build()
                        ).build();
                build.writeTo(filer);


            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return true;
    }

    private void printBuildMethod(TypeSpec.Builder builder, ClassName className, Set<VariableElement> elementsInBuildFunktion, BuilderParameter builderParameter) {
        ExecutableElement constructor = null;
        for (ExecutableElement lConstructor : builderParameter.getConstructors()) {
            if (conatainsAllParamter(lConstructor, elementsInBuildFunktion) && (constructor == null || lConstructor.getParameters().size() < constructor.getParameters().size())) {
                constructor = lConstructor;
            }
        }
        builder.addMethod(WriteUtils.generateBuildMethod(className, elementsInBuildFunktion, constructor, builderParameter.getArgsToSetter(), builderParameter.getPublicArgs()));
    }

    private void printAddMethod(TypeSpec.Builder classBuilder, VariableElement variableElement, ClassName className, Builder.CollectionProperties annotation, TypeName constructor, boolean isAbstract) throws IOException {
        String name = variableElement.getSimpleName().toString();

        String typeName = variableElement.asType().toString();
        int i = typeName.indexOf('<');
        typeName = typeName.substring(i + 1, typeName.length() - 1);

        if (constructor == null || isAbstract) {
            try {
                if (annotation != null && annotation.implementation() != Collection.class) {
                    constructor = ClassName.get(annotation.implementation());
                } else {
                    constructor = getConfiguredClassName((ClassName) constructor);
                }
            } catch (MirroredTypeException mte) {
                DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
                TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
                constructor = ClassName.get(classTypeElement);
            }
        }
        if (constructor == null) {
            throw new Error("Kein Constructor für Collection gefunden");
        }
        String methodSuffix;
        if (annotation == null || CONFIG_ID.equals(annotation.addMethodSuffix())) {
            methodSuffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        } else {
            methodSuffix = annotation.addMethodSuffix();
        }

        String paramterName;
        if (annotation == null || CONFIG_ID.equals(annotation.parameterName())) {
            paramterName = name;
        } else {
            paramterName = annotation.parameterName();
        }

        MethodSpec methodSpec = WriteUtils.generateAddMethod(methodSuffix, name,
                processingEnv.getElementUtils().getTypeElement(typeName).asType(), constructor, paramterName, className);
        classBuilder.addMethod(methodSpec);

    }


    private void printWithMethod(TypeSpec.Builder classBuilder, ClassName className, VariableElement variableElement) {
        classBuilder.addMethod(WriteUtils.generateWithMethod(Character.toUpperCase(variableElement.getSimpleName().charAt(0)) + variableElement.getSimpleName().toString().substring(1), variableElement.getSimpleName().toString(), variableElement.asType(), className));
    }

    private ClassName getConfiguredClassName(ClassName className) {
        ClassName ret = configProzessor.collectionConstructorMap.get(className);
        while (configProzessor.collectionConstructorMap.containsKey(ret))
            ret = configProzessor.collectionConstructorMap.get(ret);
        return ret;
    }


}
