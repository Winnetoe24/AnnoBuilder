import de.alexander.brand.Person;
import de.alexander.brand.annobuilder.prozessor.WriteUtils;
import de.hello.main.AutoBuilder;
import org.builder.PersonBuilder;

import javax.lang.model.element.TypeElement;

public class BuilderTest {
    public static void main(String[] args) {
        Person person = new PersonBuilder()
                .withAuto(new AutoBuilder()
                        .withLangeNummer(1)
                        .build())
                .build("Name");
        System.out.println(person);

    }
}
