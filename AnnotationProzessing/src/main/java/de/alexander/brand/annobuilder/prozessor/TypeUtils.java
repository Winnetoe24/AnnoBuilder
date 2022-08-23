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
import javax.lang.model.util.Types;
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
        int genericIndex = fullName.indexOf('<');
        if (genericIndex > -1) {
            fullName = fullName.substring(0,genericIndex);
        }
        int l = 2;
        for (int i = fullName.length() - 2; i >= 0; i--) {
            if (fullName.charAt(i) == '.') {
                l = i + 1;
                break;
            }
        }
        if (l < 2 || l > fullName.length()) return null;
        System.out.println("fullname:"+fullName);
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
            if (!variable.typeName().equals(ClassName.get(variableElement.asType()))) continue;
            if (variable.name().equals(variableElement.getSimpleName().toString())) return variable;
        }
        return null;
    }

    public static boolean isCollection(TypeElement typeElement, Types types, Map<ClassName, ClassName> collectionConstructorMap) {
        if (typeElement == null) return false;
        if (typeElement.toString().startsWith(Collection.class.getCanonicalName())) {
            return true;
        }
        if (isCollection((TypeElement) types.asElement(typeElement.getSuperclass()), types, collectionConstructorMap)) {
            return true;
        }
        for (TypeMirror typeMirror : typeElement.getInterfaces()) {
            boolean isCollection = isCollection((TypeElement) types.asElement(typeMirror), types, collectionConstructorMap);
            if (isCollection) return true;
        }

        return false;
    }
}
