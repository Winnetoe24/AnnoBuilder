package de.alexander.brand.annobuilder.prozessor;

import com.squareup.javapoet.*;
import de.alexander.brand.annobuilder.prozessor.build.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;

public class WriteUtils {



    public static FieldSpec generateArgument(Variable variable) {
        FieldSpec.Builder builder = FieldSpec.builder(variable.typeName(), variable.name())
                .addModifiers(Modifier.PRIVATE);
        if (variable.valueHandlingMode().equals(ValueHandlingMode.PROVIDE_ON_INIT)) {
            builder.initializer(variable.provider());
        }
        return builder.build();
    }

    public static FieldSpec generateBooleanFields(Variable variable) {
        if (variable.valueHandlingMode().equals(ValueHandlingMode.ALWAYS_SET) || variable.valueHandlingMode().equals(ValueHandlingMode.PROVIDE_ON_INIT)) return null;
        return FieldSpec.builder(TypeName.BOOLEAN, getIsSetName(variable),Modifier.PRIVATE).initializer("false").build();
    }

    private static String getIsSetName(Variable variable) {
        return "isSet"+ TypeUtils.toUpperCaseCamelCase(variable.name());
    }

    public static MethodSpec generateWithMethod(WithMethod withMethod) {
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder("with" + TypeUtils.toUpperCaseCamelCase(withMethod.variable().name()))
                .addParameter(withMethod.variable().typeName(), withMethod.variable().name())
                .addModifiers(Modifier.PUBLIC)
                .addCode("this." + withMethod.variable().name() + " = " + withMethod.variable().name() + ";\n");
        if (withMethod.variable().valueHandlingMode().equals(ValueHandlingMode.ONLY_SET_WHEN_SET) || withMethod.variable().valueHandlingMode().equals(ValueHandlingMode.ONLY_PROVIDE_WHEN_NOT_SET)){
            builder.addCode("this."+getIsSetName(withMethod.variable())+" = true;\n");
        }
        return builder
                .addCode("return this;")
                .returns(withMethod.returnType())
                .build();
    }

    public static MethodSpec generateAddMethod(AddMethod addMethod) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(addMethod.methodName())
                .addParameter(addMethod.type(), addMethod.parameterName())
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("if (this." + addMethod.variable().name() + " == null)")
                .addCode("this." + addMethod.variable().name() + " = new $T();\n", addMethod.implementation())
                .endControlFlow()
                .addCode("this." + addMethod.variable().name() + ".add(" + addMethod.parameterName() + ");\n");
        if (addMethod.variable().valueHandlingMode().equals(ValueHandlingMode.ONLY_SET_WHEN_SET) || addMethod.variable().valueHandlingMode().equals(ValueHandlingMode.ONLY_PROVIDE_WHEN_NOT_SET)){
            builder.addCode("this."+getIsSetName(addMethod.variable())+" = true;\n");
        }
        return builder
                .addCode("return this;")
                .returns(addMethod.returnType())
                .build();
    }

    private static CodeBlock generateConstructor(Constructor constructor) {
        StringBuilder parameterString = new StringBuilder();
        for (Variable variable : constructor.parameter()) {
            if (!parameterString.toString().equals("")) parameterString.append(", ");
            parameterString.append(variable.name());
        }
        return CodeBlock.of("$T object = new $T("+parameterString+");\n", constructor.className(), constructor.className());
    }
    private static CodeBlock generateSetter(Setter setter) {
        CodeBlock.Builder builder = CodeBlock.builder();
        if (setter.variable().valueHandlingMode().equals(ValueHandlingMode.ONLY_PROVIDE_WHEN_NOT_SET) || setter.variable().valueHandlingMode().equals(ValueHandlingMode.ONLY_SET_WHEN_SET)){
            builder.beginControlFlow("if (this."+getIsSetName(setter.variable())+")");
        }
        if (setter.method() == null) {

            builder.add("object."+setter.variable().name()+ " = this."+setter.variable().name()+";\n");
        }else {
            builder.add("object."+setter.method()+"(this."+setter.variable().name()+");\n");
        }
        if (setter.variable().valueHandlingMode().equals(ValueHandlingMode.ONLY_PROVIDE_WHEN_NOT_SET) || setter.variable().valueHandlingMode().equals(ValueHandlingMode.ONLY_SET_WHEN_SET)) {
            builder.endControlFlow();
        }
        return builder.build();
    }

    public static MethodSpec generateBuildMethod(BuildMethod buildMethod) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("build");

        builder.addModifiers(Modifier.PUBLIC);

        buildMethod.parameter().forEach(variable -> builder.addParameter(variable.typeName(), variable.name()));

        builder.addCode(generateConstructor(buildMethod.constructor()));

        buildMethod.setter().forEach(setter -> builder.addCode(generateSetter(setter)));

        builder.addCode("return object;");
        builder.returns(buildMethod.constructor().className());
        return builder.build();

    }

    public static void get(ProcessingEnvironment processingEnvironment, String name) {
        processingEnvironment.getElementUtils().getTypeElement(name);
    }

}
