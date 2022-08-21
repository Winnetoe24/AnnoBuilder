package de.alexander.brand.annobuilder.prozessor.search;

import com.squareup.javapoet.ClassName;
import de.alexander.brand.annobuilder.prozessor.ValueHandlingMode;
import lombok.Getter;
import lombok.ToString;

import javax.lang.model.element.ExecutableElement;
import java.util.HashSet;
import java.util.Set;

/**
 * enthält alle wichtigen Informationen zum Generieren eines Builders
 */
@Getter
@ToString
public class SearchParameter {

    /**
     *  Der ClassName von der Klasse, zu der ein Builder erstellt werden soll.
     */
    private final ClassName className;

    private final boolean finalInBuildFunktion;

    private final String packageString;

    private final Set<SearchVariable> searchVariables = new HashSet<>();

    private final ValueHandlingMode valueHandlingMode;
    /**
     * Alle Methoden die als Setter geeignet sind → nur ein Parameter
     */
    private final Set<ExecutableElement> possibleSetters = new HashSet<>();

    /**
     * Alle Konstruktoren die gefunden wurden
     */
    private final Set<ExecutableElement> constructors = new HashSet<>();

    public SearchParameter(ClassName className, boolean finalInBuildFunktion, String packageString,  ValueHandlingMode valueHandlingMode) {
        this.className = className;
        this.finalInBuildFunktion = finalInBuildFunktion;
        this.packageString = packageString;
        this.valueHandlingMode = valueHandlingMode;
    }


}
