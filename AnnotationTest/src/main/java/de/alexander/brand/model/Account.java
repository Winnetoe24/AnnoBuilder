package de.alexander.brand.model;


import de.alexander.brand.annobuilder.annotation.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
public class Account {
    private final AccountID identifier = new AccountID("");
    private String anzeigename;
    private int passwortHash;

    public Account(AccountID accountID, String anzeigename, int passwortHash) {

        //this.identifier = accountID;
        this.anzeigename = anzeigename;
        this.passwortHash = passwortHash;

    }
}
