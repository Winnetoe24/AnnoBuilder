package de.alexander.brand.annobuilder.prozessor.build;

import com.squareup.javapoet.ClassName;

public record AddMethod(Variable variable, String methodName, String parameterName, ClassName implementation, ClassName type) {

}
