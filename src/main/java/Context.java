import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;

public class Context {
    @SuppressWarnings("rawtypes")
    static Map<String, Class> types = new HashMap<>();

    static Map<String, Object> beans = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
    static void register(String name, Class type) {
        types.put(name, type);
    }

    @SuppressWarnings({"rawtypes", "uncheked"})
    public static Object getBean(String name) {
        return beans.computeIfAbsent(name, (java.lang.String key) -> {
            Class type = types.get(name);
            Objects.requireNonNull(type, name + " not found.");
            try {
                return createObject(type);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException ex) {
                throw new RuntimeException(name + " can not instanciate", ex);
            }
        });
    }

    private static <T> T createObject(Class<T> type) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        T object = type.getDeclaredConstructor().newInstance();
        for (Field field : type.getDeclaredFields()) {
            Inject inject = field.getAnnotation(Inject.class);
            if (inject == null) {
                continue;
            }
            field.setAccessible(true);
            field.set(object, getBean(field.getName()));
        }
        return object;
    }
}
