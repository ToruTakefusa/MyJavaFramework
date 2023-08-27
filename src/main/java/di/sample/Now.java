package di.sample;

import di.annotation.RequestScoped;

import javax.inject.Named;
import java.time.LocalDateTime;

@RequestScoped
@Named
public class Now {
    private LocalDateTime time;

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
