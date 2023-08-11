import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Bar {

    @Inject
    Foo foo;

    void showMessage() {
        System.out.println(foo.getMessage());
    }
}
