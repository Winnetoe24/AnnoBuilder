package de.alexander.brand.annobuilder.prozessor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import de.alexander.brand.annobuilder.annotation.Builder;
import de.alexander.brand.annobuilder.prozessor.build.*;
import de.alexander.brand.annobuilder.prozessor.search.BuilderElementVisitor;
import de.alexander.brand.annobuilder.prozessor.search.SearchParameter;
import de.alexander.brand.annobuilder.prozessor.search.SearchVariable;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
        try {
        Set<SearchParameter> searchParameters = new HashSet<>();
        for (Element e : rootE) {
            searchParameters.addAll(e.accept(new BuilderElementVisitor(configProzessor, processingEnv.getTypeUtils(), processingEnv.getElementUtils()), null));
        }

            searchParameters.stream()
                    .filter(this::verify)
                    .map(this::toBuildPlan)
                    .forEach(buildPlan -> {
                        TypeSpec.Builder builder = TypeSpec.classBuilder(buildPlan.className());
                        builder.addModifiers(Modifier.PUBLIC);
                        buildPlan.arguments().forEach(variable -> {
                            builder.addField(WriteUtils.generateArgument(variable));
                            FieldSpec fieldSpec = WriteUtils.generateBooleanFields(variable);
                            if (fieldSpec != null)
                            builder.addField(fieldSpec);
                        });
                        buildPlan.withMethods().forEach(withMethod -> builder.addMethod(WriteUtils.generateWithMethod(withMethod)));
                        buildPlan.addMethods().forEach(addMethod -> builder.addMethod(WriteUtils.generateAddMethod(addMethod)));
                        builder.addMethod(WriteUtils.generateBuildMethod(buildPlan.buildMethod()));
                        TypeSpec typeSpec = builder.build();

                        try {
                            Filer filer = processingEnv.getFiler();
                            JavaFile build = JavaFile.builder(buildPlan.className().packageName().toString(), typeSpec).build();
                            build.writeTo(filer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

        }catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Verfizier das die SearchParameter keine falschen Eingaben enthalten. Fehler können beim Mapping trotzdem entstehen
     * @param searchParameter
     */
    private boolean verify(SearchParameter searchParameter) {
        searchParameter.getSearchVariables().stream()
                .filter(searchVariable -> searchVariable.getCollectionArgs() != null)
                .forEach(searchVariable -> {
                    if (searchVariable.getCollectionArgs().getImplementation() == null)
                        throw new AnnotationProcessingException("Für die Variable "+searchVariable.getVariableName()+" in "+searchParameter.getClassName().simpleName()+" wurde keine Implementation gefunden. Siehe @CollectionProperties");
                });

        searchParameter.getSearchVariables().stream().filter(searchVariable -> searchVariable.getVariableName() == null)
                .forEach(System.err::println);
        System.out.println("verify");
        System.out.println(searchParameter);
        return true;
    }

    /**
     * Mapped die SearchParameter zu einem BuildPlan.
     * @param searchParameter
     * @return
     */
    private BuildPlan toBuildPlan(SearchParameter searchParameter) {
        ClassName className = ClassName.get( searchParameter.getPackageString(), searchParameter.getClassName().simpleName()+"Builder");

        Set<Variable> variables = new HashSet<>();
        searchParameter.getSearchVariables().forEach(searchVariable -> variables.add(toVariable(searchVariable)));

        Set<WithMethod> withMethods = new HashSet<>();
        searchParameter.getSearchVariables().forEach(searchVariable -> {
            if (searchVariable.getCollectionArgs() != null && !searchVariable.getCollectionArgs().isHasWithMethod()) return;
            withMethods.add(new WithMethod(toVariable(searchVariable), className));
        });

        Set<AddMethod> addMethods = new HashSet<>();
        searchParameter.getSearchVariables()
                .stream()
                .filter(searchVariable -> searchVariable.getCollectionArgs() != null)
                .forEach(searchVariable -> {
                    SearchVariable.CollectionArgs collectionArgs = searchVariable.getCollectionArgs();
                    addMethods.add(new AddMethod(toVariable(searchVariable), collectionArgs.getMethodName(), collectionArgs.getParameterName(), collectionArgs.getImplementation(), collectionArgs.getType(), className));
                });



        Set<SearchVariable> konstruktorParameter = searchParameter.getSearchVariables().stream()
                .filter(SearchVariable::isIncludeInConstructor)
                .collect(Collectors.toSet());
        ExecutableElement constructorElement = searchParameter.getConstructors().stream()
                .filter(executableElement ->{
                    System.out.println(executableElement);
                    System.out.println(executableElement.isDefault());

                    return !executableElement.isDefault() || (searchParameter.getAllArgs() == null &&searchParameter.getRequiredArgs() == null);
                })
                .filter(executableElement -> {
                    for (SearchVariable searchVariable : konstruktorParameter) {
                        if (!containsVariable(searchVariable.getTypeName(), searchVariable.getVariableName(), executableElement.getParameters())) {
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
                //Debug
                .orElse(searchParameter.getConstructors().stream().findAny().get());
//                .orElseThrow(() -> new AnnotationProcessingException("Kein passender Konstruktor gefunden:" + searchParameter.getClassName().simpleName()));
        List<Variable> parameterList = new ArrayList<>();
        constructorElement.getParameters().forEach(variableElement ->{
            Variable variable = TypeUtils.getVariable(variableElement, variables);
            if (variable != null)
            parameterList.add(variable);
        });
        Constructor constructor = new Constructor(searchParameter.getClassName(), parameterList);

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
                .filter(searchVariable -> !parameterList.contains(toVariable(searchVariable)))
                .forEach(searchVariable -> {
                    if ("".equals(searchVariable.getSetMethod())) {
                        List<ExecutableElement> setters = searchParameter.getPossibleSetters().stream()
                                .filter(executableElement -> ClassName.get(executableElement.getParameters().get(0).asType()).equals(searchVariable.getTypeName()))
                                .filter(executableElement -> executableElement.getSimpleName().toString().equals("set"+TypeUtils.toUpperCaseCamelCase(searchVariable.getVariableName())))
                                .filter(executableElement -> executableElement.getAnnotation(Builder.SetMethod.class) == null)
                                .collect(Collectors.toList());
                        if (setters.size() == 0){
                                throw new AnnotationProcessingException("Es wurde kein Setter für "+searchVariable.getVariableName()+" in "+searchParameter.getClassName().simpleName()+" gefunden");
                        }
                        if(setters.size() > 1) System.err.println("Es wurden mehrere mögliche Setter für "+searchVariable.getVariableName()+" in "+searchParameter.getClassName().simpleName()+" gefunden");
                        searchVariable.setSetMethod(setters.get(0).getSimpleName().toString());
                    }
                    setter.add(new Setter(toVariable(searchVariable),searchVariable.getSetMethod()));
                });

        List<Variable> buildMethodParameter = new ArrayList<>();
        searchParameter.getSearchVariables().stream()
                .filter(SearchVariable::isIncludeInBuildMethod)
                .sorted(Comparator.comparing(SearchVariable::getVariableName))
                .forEachOrdered(searchVariable-> buildMethodParameter.add(toVariable(searchVariable)));

        BuildMethod buildMethod = new BuildMethod(constructor, buildMethodParameter, setter);

        System.out.println("map");
        return new BuildPlan(className, variables, withMethods, addMethods, buildMethod);
    }



    private Variable toVariable(SearchVariable searchVariable) {
        return new Variable(searchVariable.getTypeName(), searchVariable.getVariableName(), searchVariable.getValueHandling(), searchVariable.getProvider());
    }


}
