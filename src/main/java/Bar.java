import javax.inject.Inject;

public class Bar {

    @Inject
    Foo foo;

    void showMessage() {
        System.out.println(foo.getMessage());
    }
}
