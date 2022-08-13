package de.alexander.brand.annobuilder.prozessor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Set;

public class TypeUtils {
    public static  boolean conatainsAllParamter(ExecutableElement element, Set<VariableElement> parameter) {
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

    public static  String getClassName(TypeElement e) {
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
}
