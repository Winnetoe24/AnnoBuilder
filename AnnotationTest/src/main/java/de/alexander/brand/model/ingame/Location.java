package de.alexander.brand.model.ingame;

import de.alexander.brand.annobuilder.annotation.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class Location {
    private int x;
    private int y;
    private int z;
    public Location() {

    }
}
