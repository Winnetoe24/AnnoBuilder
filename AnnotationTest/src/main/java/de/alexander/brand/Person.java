package de.alexander.brand;

import de.alexander.brand.annobuilder.annotation.Builder;
import de.alexander.brand.annobuilder.prozessor.ValueHandlingMode;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Builder(packageString = "org.builder")
@ToString
//TODO @Setter support
public class Person {
    private int zahl;
    //TODO @Builder.Default(Object)
    private final String finalerName;
    @Builder.SetMethod(link = "autoSetzen")
    @Builder.ValueHandling(mode =  ValueHandlingMode.ONLY_SET_WHEN_SET)
    public Auto auto = new Auto();


    public List<String> bekannte;
    public ArrayList<Integer> glückszahlen;
    private long lang;
    public TestListe<Person> verwandte;

    public Person(int zahl, String finalerName) {
        this.zahl = zahl;
        this.finalerName = finalerName;
    }


    public Person(String finalerName) {
        this.finalerName = finalerName;
    }


    public void setZahl(int zahl) {
        this.zahl = zahl;
    }
    public void setZahl(String zahl) {

    }

    public void setLang(long lang) {

    }

    public void autoSetzen(Auto auto) {

    }


}
