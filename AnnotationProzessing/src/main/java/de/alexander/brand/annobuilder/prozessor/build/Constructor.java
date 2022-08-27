package de.alexander.brand.annobuilder.prozessor.build;

import com.squareup.javapoet.ClassName;

import java.util.List;

/**
 * @param className Der ClassName der Klasse, die erzeugt werden soll
 * @param parameter Die Parameter die genutzt werden sollen
 */
public record Constructor(ClassName className,List<Variable> parameter) {
}
