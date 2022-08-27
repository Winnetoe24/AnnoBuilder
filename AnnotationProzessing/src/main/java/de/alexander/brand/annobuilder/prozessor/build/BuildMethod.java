package de.alexander.brand.annobuilder.prozessor.build;

import java.util.List;
import java.util.Set;

public record BuildMethod(Constructor constructor, List<Variable> parameter, Set<Setter> setter) {
}
