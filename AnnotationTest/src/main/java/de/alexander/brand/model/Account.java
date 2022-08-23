package de.alexander.brand.model;



import de.alexander.brand.annobuilder.annotation.Builder;
import lombok.Getter;

@Getter
@Builder
public class Account {
    private final String email;
    private String anzeigename;
    private int passwortHash;
    public Account(String email, String anzeigename, int passwortHash) {
        this.email = email;
        this.anzeigename = anzeigename;
        this.passwortHash = passwortHash;
    }
}
