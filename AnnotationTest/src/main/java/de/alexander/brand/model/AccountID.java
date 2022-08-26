package de.alexander.brand.model;

import de.alexander.brand.annobuilder.annotation.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Der Identifier für einen {@link Account}
 */
@AllArgsConstructor
@Getter
@Builder
public class AccountID {
    private final String email;
}
