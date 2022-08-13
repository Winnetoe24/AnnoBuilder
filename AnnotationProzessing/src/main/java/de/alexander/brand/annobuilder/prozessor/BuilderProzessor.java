package de.alexander.brand.annobuilder.prozessor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import de.alexander.brand.annobuilder.annotation.Builder;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static de.alexander.brand.annobuilder.annotation.Builder.CONFIG_ID;
import static de.alexander.brand.annobuilder.prozessor.TypeUtils.conatainsAllParamter;

@SupportedAnnotationTypes("de.alexander.brand.annobuilder.annotation.Builder")
@AutoService(Processor.class)
public class BuilderProzessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        new ArrayList<>();
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
                String packageString = builderParameter.builder.packageString();
                if (packageString.equals(CONFIG_ID)) {
                    packageString = "de.annoBuilder";
                }

                Filer filer = processingEnv.getFiler();

                ClassName className = ClassName.get(packageString, builderParameter.className);
                TypeSpec.Builder classBuilder = TypeSpec.classBuilder(builderParameter.className);


                Set<VariableElement> variableElements = new HashSet<>();
                variableElements.addAll(builderParameter.finalArgs);
                variableElements.addAll(builderParameter.publicArgs);
                variableElements.addAll(builderParameter.privateArgsToSetter.keySet());

                for (VariableElement variableElement : variableElements) {
                    classBuilder.addField(WriteUtils.generateArgument(variableElement));
                }

                Set<VariableElement> elementsInBuildFunktion = new HashSet<>();
                elementsInBuildFunktion.addAll(builderParameter.finalArgs);
                elementsInBuildFunktion.addAll(builderParameter.privateArgsToSetter.entrySet().stream()
                        .filter(variableElementExecutableElementEntry -> variableElementExecutableElementEntry.getValue() == null)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()));

                variableElements.removeAll(elementsInBuildFunktion);

                //Add and WithMethods
                for (VariableElement variableElement : variableElements) {
                    boolean isCollection = false;
                    Builder.CollectionProperties annotation = variableElement.getAnnotation(Builder.CollectionProperties.class);
                    if (annotation == null) {
                        Set<TypeMirror> typeMirrors = new HashSet<>();
                        typeMirrors.add(variableElement.asType());
                        while (!typeMirrors.isEmpty()) {
                            TypeMirror t = typeMirrors.stream().findAny().orElse(null);
                            if (t == null) {
                                break;
                            }
                            if (t.toString().startsWith(Collection.class.getCanonicalName())) {
                                isCollection = true;
                                break;
                            }
                            TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(t);
                            if (typeElement == null) break;
                            typeMirrors.addAll(typeElement.getInterfaces());
                            typeMirrors.remove(t);

                        }
                    }

                    if (isCollection || annotation != null) {
                        try {
                            printAddMethod(classBuilder, variableElement, className, annotation);
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
        for (ExecutableElement lConstructor : builderParameter.constructors) {
            if (conatainsAllParamter(lConstructor, elementsInBuildFunktion) && (constructor == null || lConstructor.getParameters().size() < constructor.getParameters().size())) {
                constructor = lConstructor;
            }
        }
        builder.addMethod(WriteUtils.generateBuildMethod(className, elementsInBuildFunktion, constructor, builderParameter.privateArgsToSetter, builderParameter.publicArgs));
    }

    private void printAddMethod(TypeSpec.Builder classBuilder, VariableElement variableElement, ClassName className, Builder.CollectionProperties annotation) throws IOException {
        String name = variableElement.getSimpleName().toString();

        String typeName = variableElement.asType().toString();
        int i = typeName.indexOf('<');
        typeName = typeName.substring(i + 1, typeName.length() - 1);

        ClassName contructor;
        try {
            if (annotation == null || annotation.implementation() == Collection.class) {
                contructor = ClassName.get("java.util", "ArrayList");
            } else {
                contructor = ClassName.get(annotation.implementation());
            }
        } catch (MirroredTypeException mte) {
            DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
            contructor = ClassName.get(classTypeElement);
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
                processingEnv.getElementUtils().getTypeElement(typeName).asType(), contructor, paramterName, className);
        classBuilder.addMethod(methodSpec);

    }


    private void printWithMethod(TypeSpec.Builder classBuilder, ClassName className, VariableElement variableElement) {
        classBuilder.addMethod(WriteUtils.generateWithMethod(Character.toUpperCase(variableElement.getSimpleName().charAt(0)) + variableElement.getSimpleName().toString().substring(1), variableElement.getSimpleName().toString(), variableElement.asType(), className));
    }


}
