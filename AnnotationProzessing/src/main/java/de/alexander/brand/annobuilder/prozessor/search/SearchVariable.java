package de.alexander.brand.annobuilder.prozessor.search;

import com.squareup.javapoet.ClassName;
import de.alexander.brand.annobuilder.prozessor.ValueHandlingMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchVariable {
    private ClassName className;
    private String variableName;

    /* Konfigurationen */

    /**
     * Hält die Parameter für die AddMethode. {@code null} wenn es keine Collection ist.
     */
    private CollectionArgs collectionArgs = null;

    private boolean includeInBuildMethod = false;

    private boolean includeInConstructor = false;

    private boolean onlySetWhenSet = false;

    private ValueHandlingMode valueHandling = ValueHandlingMode.ALWAYS_SET;

    private String provider = null;

    private String setMethod = null;

    /**
     * Alle Parameter die zu einer Collection gesammelt werden.
     * Beim Erzeugen sollen, bei fehlenden Werten die Werte aus der Konfiguration geladen werden.
     */
    @AllArgsConstructor
    @Getter
    public static class CollectionArgs {
        private String methodName;
        private String parameterName;
        private ClassName implementation;
    }

}
