package de.alexander.brand.annobuilder.prozessor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.sun.source.doctree.SerialTree;
import de.alexander.brand.annobuilder.annotation.Builder;
import de.alexander.brand.annobuilder.prozessor.build.*;
import de.alexander.brand.annobuilder.prozessor.search.BuilderElementVisitor;
import de.alexander.brand.annobuilder.prozessor.search.SearchParameter;
import de.alexander.brand.annobuilder.prozessor.search.SearchVariable;
import org.checkerframework.checker.units.qual.A;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static de.alexander.brand.annobuilder.annotation.Builder.CONFIG_ID;
import static de.alexander.brand.annobuilder.prozessor.TypeUtils.conatainsAllParamter;
import static de.alexander.brand.annobuilder.prozessor.TypeUtils.containsVariable;

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
        Set<SearchParameter> searchParameters = new HashSet<>();
        for (Element e : rootE) {
            searchParameters.addAll(e.accept(new BuilderElementVisitor(configProzessor, processingEnv.getTypeUtils()), null));
        }


        for (SearchParameter searchParameter : searchParameters) {
            if (searchParameter == null) continue;
            try {
                String packageString = searchParameter.getBuilder().packageString();
                if (packageString.equals(CONFIG_ID)) {
                    packageString = configProzessor.getPackageString();
                }

                Filer filer = processingEnv.getFiler();

                ClassName className = ClassName.get(packageString, searchParameter.getClassName());
                TypeSpec.Builder classBuilder = TypeSpec.classBuilder(searchParameter.getClassName());


                Set<VariableElement> variableElements = new HashSet<>();
                variableElements.addAll(searchParameter.getConstructorArgs());
                variableElements.addAll(searchParameter.getPublicArgs());
                variableElements.addAll(searchParameter.getArgsToSetter().keySet());

                for (VariableElement variableElement : variableElements) {
                    classBuilder.addField(WriteUtils.generateArgument(variableElement));
                }

                Set<VariableElement> elementsInBuildFunktion = new HashSet<>();
                elementsInBuildFunktion.addAll(searchParameter.getConstructorArgs());
                elementsInBuildFunktion.addAll(searchParameter.getArgsToSetter().entrySet().stream()
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
                printBuildMethod(classBuilder, className, elementsInBuildFunktion, searchParameter);

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

    /**
     * Verfizier das die SearchParameter keine falschen Eingaben enthalten. Fehler können beim Mapping trotzdem entstehen
     * @param searchParameter
     */
    private void verify(SearchParameter searchParameter) {
        searchParameter.getSearchVariables().stream()
                .filter(searchVariable -> searchVariable.getCollectionArgs() != null)
                .forEach(searchVariable -> {
                    if (searchVariable.getCollectionArgs().getImplementation() == null)
                        throw new AnnotationProcessingException("Für die Variable "+searchVariable.getVariableName()+" in "+searchParameter.getClassName().simpleName()+" wurde keine Implementation gefunden. Siehe @CollectionProperties");
                });
    }

    /**
     * Mapped die SearchParameter zu einem BuildPlan.
     * @param searchParameter
     * @return
     */
    private BuildPlan toBuildPlan(SearchParameter searchParameter) {

        Set<Variable> variables = new HashSet<>();
        searchParameter.getSearchVariables().forEach(searchVariable -> variables.add(toVariable(searchVariable)));

        Set<WithMethod> withMethods = new HashSet<>();
        searchParameter.getSearchVariables().forEach(searchVariable -> {
            if (searchVariable.getCollectionArgs() != null && !searchVariable.getCollectionArgs().isHasWithMethod()) return;
            withMethods.add(new WithMethod(toVariable(searchVariable)));
        });

        Set<AddMethod> addMethods = new HashSet<>();
        searchParameter.getSearchVariables()
                .stream()
                .filter(searchVariable -> searchVariable.getCollectionArgs() != null)
                .forEach(searchVariable -> {
                    SearchVariable.CollectionArgs collectionArgs = searchVariable.getCollectionArgs();
                    addMethods.add(new AddMethod(toVariable(searchVariable), collectionArgs.getMethodName(), collectionArgs.getParameterName(), collectionArgs.getImplementation(), collectionArgs.getType()));
                });



        Set<SearchVariable> konstruktorParameter = searchParameter.getSearchVariables().stream()
                .filter(SearchVariable::isIncludeInConstructor)
                .collect(Collectors.toSet());
        ExecutableElement constructor = searchParameter.getConstructors().stream()
                .filter(executableElement -> {
                    for (SearchVariable searchVariable : konstruktorParameter) {
                        if (!TypeUtils.containsVariable(searchVariable.getTypeName(), searchVariable.getVariableName(), executableElement.getParameters())) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(executableElement -> {
                    for (VariableElement parameter : executableElement.getParameters()) {
                        if (!containsVariable(parameter, variables))
                            return false;
                    }
                    return true;
                })
                .min(Comparator.comparingInt(value -> value.getParameters().size()))
                .orElseThrow(() -> new AnnotationProcessingException("Kein passender Konstruktor gefunden:" + searchParameter.getClassName().simpleName()));
        List<Variable> parameterList = new ArrayList<>();
        constructor.getParameters().forEach(variableElement -> parameterList.add(TypeUtils.getVariable(variableElement, variables)));

        Set<Setter> setter = new HashSet<>();
        searchParameter.getPossibleSetters().stream()
                .filter(executableElement -> executableElement.getAnnotation(Builder.SetMethod.class) != null)
                .forEach(executableElement -> {
                    Builder.SetMethod setMethod = executableElement.getAnnotation(Builder.SetMethod.class);
                    String link = setMethod.link();
                    Set<SearchVariable> collect = searchParameter.getSearchVariables().stream()
                            .filter(searchVariable -> searchVariable.getVariableName().equals(link))
                            .filter(searchVariable -> searchVariable.getTypeName().equals(ClassName.get(executableElement.getParameters().get(0).asType())))
                            .collect(Collectors.toSet());
                    if (collect.size() != 1) throw new AnnotationProcessingException("Name("+link+") für Variable wurde "+collect.size()+" mal vergeben. Konnte SetMethod.link() nicht verarbeiten: "+executableElement.getSimpleName()+" in "+executableElement.getEnclosingElement().getSimpleName());
                    collect.forEach(searchVariable -> searchVariable.setSetMethod(executableElement.getSimpleName().toString()));
                });
        searchParameter.getSearchVariables().stream()
                .filter(searchVariable -> !searchVariable.isIncludeInConstructor())
                .forEach(searchVariable -> {
                    if ("".equals(searchVariable.getSetMethod())) {
                        List<ExecutableElement> setters = searchParameter.getPossibleSetters().stream()
                                .filter(executableElement -> ClassName.get(executableElement.getParameters().get(0).asType()).equals(searchVariable.getTypeName()))
                                .filter(executableElement -> executableElement.getSimpleName().toString().equals("set"+TypeUtils.toUpperCaseCamelCase(searchVariable.getVariableName())))
                                .filter(executableElement -> executableElement.getAnnotation(Builder.SetMethod.class) == null)
                                .collect(Collectors.toList());
                        if (setters.size() == 0) throw new AnnotationProcessingException("Es wurde kein Setter für "+searchVariable.getVariableName()+" in "+searchParameter.getClassName().simpleName()+" gefunden");
                        if(setters.size() > 1) System.err.println("Es wurden mehrere mögliche Setter für "+searchVariable.getVariableName()+" in "+searchParameter.getClassName().simpleName()+" gefunden");
                        searchVariable.setSetMethod(setters.get(0).getSimpleName().toString());
                    }
                    setter.add(new Setter(toVariable(searchVariable),searchVariable.getSetMethod()));
                });
        return null;
    }



    private Variable toVariable(SearchVariable searchVariable) {
        return new Variable((ClassName) searchVariable.getTypeName(), searchVariable.getVariableName(), searchVariable.getValueHandling(), searchVariable.getProvider());
    }

    private void printBuildMethod(TypeSpec.Builder builder, ClassName className, Set<VariableElement> elementsInBuildFunktion, SearchParameter searchParameter) {
        ExecutableElement constructor = null;
        for (ExecutableElement lConstructor : searchParameter.getConstructors()) {
            if (conatainsAllParamter(lConstructor, elementsInBuildFunktion) && (constructor == null || lConstructor.getParameters().size() < constructor.getParameters().size())) {
                constructor = lConstructor;
            }
        }
        builder.addMethod(WriteUtils.generateBuildMethod(className, elementsInBuildFunktion, constructor, searchParameter.getArgsToSetter(), searchParameter.getPublicArgs()));
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
                if (constructor.equals(ClassName.get(Collection.class))) {

                }
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
