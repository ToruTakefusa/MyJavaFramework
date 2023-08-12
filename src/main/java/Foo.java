import javax.inject.Named;

@Named
public class Foo {
    String getMessage() {
        return "Foo";
    }

    @InvokeLog
    String getName() {
        return "foo!";
    }
}
