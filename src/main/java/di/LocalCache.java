package di;

import di.Context;

import java.util.Objects;

public class LocalCache<T> {
    private ThreadLocal<T> local = new ThreadLocal<>();

    private String name;

    public LocalCache(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        T obj = local.get();
        if (!Objects.isNull(obj)) {
            return obj;
        }
        obj = (T) Context.getBean(name);
        local.set(obj);
        return obj;
    }
}
