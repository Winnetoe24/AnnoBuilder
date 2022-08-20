package de.alexander.brand.annobuilder.prozessor;

import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static de.alexander.brand.annobuilder.prozessor.TypeUtils.conatainsAllParamter;

public class WriteUtils {

    public static FieldSpec generateArgument(VariableElement variableElement) {
        return FieldSpec.builder(TypeName.get(variableElement.asType()),variableElement.getSimpleName().toString())
                .addModifiers(Modifier.PRIVATE)
                .build();
    }

    public static MethodSpec generateWithMethod(String methodSuffix,String name,TypeMirror typeMirror, ClassName returnType) {
        return MethodSpec
                .methodBuilder("with"+methodSuffix)
                .addParameter(TypeName.get(typeMirror), name)
                .addModifiers(Modifier.PUBLIC)
                .addCode("this."+name+" = "+name+";\n")
                .addCode("return this;")
                .returns(returnType)
                .build();
    }

    public static MethodSpec generateAddMethod(String methodSuffix, String name, TypeMirror typeMirror, TypeName constructor, String parameterName, ClassName returnType) {
        return MethodSpec.methodBuilder("add"+methodSuffix)
                .addParameter(TypeName.get(typeMirror),parameterName)
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("if (this."+name+" == null)")
                .addCode("this."+name+" = new $T();\n",constructor)
                .endControlFlow()
                .addCode("this."+name+".add("+parameterName+");\n")
                .addCode("return this;")
                .returns(returnType)
                .build();
    }

    public static MethodSpec generateBuildMethod(ClassName className, Set<VariableElement> elementsInBuildFunktion,  ExecutableElement constructor, Map<VariableElement, ExecutableElement> privateArgsToSetter, Set<VariableElement> publicArgs) {
        MethodSpec.Builder build = MethodSpec.methodBuilder("build");
        int i = 0;
        String parameterString = "";
        for (VariableElement variableElement : elementsInBuildFunktion) {
            build.addParameter( TypeName.get(variableElement.asType()), variableElement.getSimpleName().toString());
            if (i != 0) parameterString += ", ";
            parameterString += variableElement.getSimpleName().toString();
            i++;
        }
        build.addModifiers(Modifier.PUBLIC);
        TypeName objectName = TypeName.get(constructor.getEnclosingElement().asType());


        build.addCode("$T object = new $T("+parameterString+");\n", objectName, objectName);

        for (VariableElement variableElement : privateArgsToSetter.keySet()) {
            if (constructor != null && conatainsAllParamter(constructor, Set.of(variableElement))) continue;
            ExecutableElement executableElement = privateArgsToSetter.get(variableElement);
            if (executableElement == null) {
                System.err.println("Konnte Element nicht zuweisen:"+variableElement.getSimpleName());
                continue;
            }
            build.addCode("object."+ executableElement.getSimpleName()+"("+variableElement.getSimpleName()+");\n");
        }

        for (VariableElement variableElement : publicArgs) {
            build.addCode("object."+variableElement.getSimpleName()+" = this."+variableElement.getSimpleName()+";\n");
        }

        build.addCode("return object;");
        build.returns(objectName);
        return build.build();

    }

    public static void get(ProcessingEnvironment processingEnvironment, String name) {
        processingEnvironment.getElementUtils().getTypeElement(name);
    }

}
