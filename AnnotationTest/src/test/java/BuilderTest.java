import de.alexander.brand.Person;
import de.alexander.brand.PersonBuilder;
import de.alexander.brand.builder.AutoBuilder;

public class BuilderTest {
    public static void main(String[] args) {
        Person person = new PersonBuilder()
                .withAuto(new AutoBuilder()
                        .withLangeNummer(1)
                        .build())
                .build("Name");

    }
}
