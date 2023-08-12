import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Bar {

    @Inject
    Foo foo;

    @InvokeLog
    void showMessage() {
        System.out.println( foo.getName() + foo.getMessage());
    }
}
