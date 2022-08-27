package de.alexander.brand.annobuilder.prozessor.build;

import com.squareup.javapoet.ClassName;
import org.checkerframework.checker.units.qual.C;

import java.util.Set;


public record BuildPlan(ClassName className, Set<Variable> arguments, Set<WithMethod> withMethods, Set<AddMethod> addMethods, BuildMethod buildMethod) {
}
