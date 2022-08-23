package de.alexander.brand.model.ingame;

import de.alexander.brand.annobuilder.annotation.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class Location {
    private int x,y,z;
}
