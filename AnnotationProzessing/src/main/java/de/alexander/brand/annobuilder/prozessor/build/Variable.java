package de.alexander.brand.annobuilder.prozessor.build;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import de.alexander.brand.annobuilder.prozessor.ValueHandlingMode;

/**
 * Speichert die Informationen, um eine ein Argument eines Builders zu generieren
 */
public record Variable(TypeName typeName, String name, ValueHandlingMode valueHandlingMode, String provider) {
}
