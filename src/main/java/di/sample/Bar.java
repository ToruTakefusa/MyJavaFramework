package di.sample;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.LocalDateTime;
import di.annotation.InvokeLog;

@Named
public class Bar {

    @Inject
    Foo foo;

    @Inject
    Now now;

    @InvokeLog
    void showMessage() {
        System.out.println( foo.getName() + foo.getMessage());
    }

    void longProcess() {
        now.setTime(LocalDateTime.now());
        System.out.println("start:" + now.getTime());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        System.out.println("end    :" + now.getTime());
    }
}
