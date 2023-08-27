package di.sample;

import javax.inject.Named;
import di.annotation.InvokeLog;

@Named
public class Foo {
    String getMessage() {
        return "sample.Foo";
    }

    @InvokeLog
    String getName() {
        return "foo!";
    }
}
