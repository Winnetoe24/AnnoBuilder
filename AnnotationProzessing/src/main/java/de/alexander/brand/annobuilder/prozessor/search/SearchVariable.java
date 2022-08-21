package de.alexander.brand.annobuilder.prozessor.search;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import de.alexander.brand.annobuilder.prozessor.ValueHandlingMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchVariable {
    private TypeName typeName;
    private String variableName;

    /* Konfigurationen */

    /**
     * Hält die Parameter für die AddMethode. {@code null} wenn es keine Collection ist.
     */
    private CollectionArgs collectionArgs = null;

    /**
     * Wenn true wird der Wert als Parameter in der Build-Methode hinzugefügt, ansonsten wird eine with-Methode hinzugefügt.
     * Die With-Methode wird bei Collections nur generiert, wenn es extra angegeben wird.
     */
    private boolean includeInBuildMethod = false;

    private boolean includeInConstructor = false;

    private ValueHandlingMode valueHandling = ValueHandlingMode.ALWAYS_SET;

    private String provider = null;

    /**
     * Enthält den Namen der Methode, die zum Setzen benutzt werden soll.
     * null bedeutet keine Set-Methode, ein leerer String bedeutet noch keine gefunden wurde aber es eine geben muss
      */
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
        private boolean hasWithMethod;
        private ClassName type;
    }

    public SearchVariable(TypeName typeName, String variableName) {
        this.typeName = typeName;
        this.variableName = variableName;
    }

}
