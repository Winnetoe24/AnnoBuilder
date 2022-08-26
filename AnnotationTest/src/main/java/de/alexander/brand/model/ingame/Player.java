package de.alexander.brand.model.ingame;

import de.alexander.brand.annobuilder.annotation.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Builder
@NoArgsConstructor
public class Player {
    private Location location;
    public Player(Location location) {

        this.location = location;
    }
}
