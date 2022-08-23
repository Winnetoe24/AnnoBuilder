package de.alexander.brand.model.ingame;

import de.alexander.brand.annobuilder.annotation.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Builder
public class Player {
    private Location location;
}
