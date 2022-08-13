package de.alexander.brand;

import de.alexander.brand.annobuilder.annotation.Builder;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Builder
@ToString
public class Auto {

    public int nummer = 1;
    public String name = "Auto";
    private long langeNummer;

    @Builder.CollectionProperties(
            addMethodSuffix = "String",
            parameterName = "string",
            implementation = HashSet.class
    )
    public Set<String> strings;

    public void setLangeNummer(long langeNummer) {
        this.langeNummer = langeNummer;
    }
}
