package de.alexander.brand.annobuilder.prozessor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.Map;
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

    public static ClassName toClassName(String fullName) {
        int l = 2;
        for (int i = fullName.length() - 2; i >= 0; i--) {
            if (fullName.charAt(i) == '.') {
                l = i + 1;
                break;
            }
        }
        if (l < 2 || l > fullName.length()) return null;
        return ClassName.get(fullName.substring(0,l-1), fullName.substring(l));
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
