package de.alexander.brand.annobuilder.prozessor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import de.alexander.brand.annobuilder.prozessor.build.Variable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;

public class TypeUtils {
    public static boolean conatainsAllParamter(ExecutableElement element, Set<VariableElement> parameter) {
        for (VariableElement variableElement : parameter) {
            boolean found = false;
            for (VariableElement param : element.getParameters()) {
                if (param.asType().equals(variableElement.asType()) && param.getSimpleName().contentEquals(variableElement.getSimpleName())) {
                    found = true;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static String getClassName(TypeElement e) {
        Name simpleName = e.getSimpleName();
        int l = 0;
        for (int i = simpleName.length() - 2; i >= 0; i--) {
            if (simpleName.charAt(i) == '.') {
                l = i + 1;
                break;
            }
        }
        return simpleName.toString().substring(l, simpleName.length());
    }

    public static ClassName toClassName(String fullName) {
        int l = 2;
        for (int i = fullName.length() - 2; i >= 0; i--) {
            if (fullName.charAt(i) == '.') {
                l = i + 1;
                break;
            }
        }
        if (l < 2 || l > fullName.length()) return null;
        return ClassName.get(fullName.substring(0, l - 1), fullName.substring(l));
    }

    public static String toLowerCaseCamelCase(String string) {
        if (string.length() < 2) return string.toLowerCase(Locale.ROOT);
        return Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }

    public static String toUpperCaseCamelCase(String string) {
        if (string.length() < 2) return string.toUpperCase(Locale.ROOT);
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    public static boolean containsVariable(TypeName className, String name, List<? extends VariableElement> variableElements) {
        for (VariableElement variableElement : variableElements) {
            if (!className.equals(ClassName.get(variableElement.asType()))) continue;
            if (!name.equals(variableElement.getSimpleName().toString())) continue;
            return true;
        }
        return false;
    }
    public static boolean containsVariable(VariableElement variableElement, Set<Variable> variables) {
       return getVariable(variableElement, variables) != null;
    }

    public static Variable getVariable(VariableElement variableElement, Set<Variable> variables) {
        for (Variable variable : variables) {
            if (!variable.className().equals(ClassName.get(variableElement.asType()))) continue;
            if (variable.name().equals(variableElement.getSimpleName().toString())) return variable;
        }
        return null;
    }

    public static TypeName getCollectionName(TypeElement typeElement, ProcessingEnvironment processingEnvironment, Map<ClassName, ClassName> collectionConstructorMap) {
        if (typeElement == null) return null;
        if (typeElement.toString().startsWith(Collection.class.getCanonicalName())) {
            return ClassName.get(typeElement);
        }
        for (TypeMirror typeMirror : typeElement.getInterfaces()) {
            TypeName collectionName = getCollectionName((TypeElement) processingEnvironment.getTypeUtils().asElement(typeMirror), processingEnvironment, collectionConstructorMap);
            if (collectionName != null) {

                ClassName className = ClassName.get(typeElement);
                if (collectionConstructorMap.containsKey(className)) {

                }
                return className;

            }
        }
        return null;
    }
}
