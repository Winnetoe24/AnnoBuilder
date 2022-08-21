package de.alexander.brand.annobuilder.prozessor.build;

import com.squareup.javapoet.ClassName;
import de.alexander.brand.annobuilder.prozessor.ValueHandlingMode;

/**
 * Speichert die Informationen, um eine ein Argument eines Builders zu generieren
 */
public record Variable(ClassName className, String name, ValueHandlingMode valueHandlingMode, String provider) {
}
