package de.alexander.brand.annobuilder.prozessor.build;

import com.squareup.javapoet.ClassName;

/**
 * Baut eine with-Method
 */
public record WithMethod(Variable variable, ClassName returnType) {
}
