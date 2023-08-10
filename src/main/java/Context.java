import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Context {
    @SuppressWarnings("rawtypes")
    static Map<String, Class> types = new HashMap<>();

    static Map<String, Object> beans = new HashMap<>();

    @SuppressWarnings("rawtypes")
    static void register(String name, Class type) {
        types.put(name, type);
    }

    static Object getBean(String name) {
        return beans.computeIfAbsent(name, key -> {
            try {
                return newInstance(name);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object newInstance(String name) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class type = types.get(name);
        Objects.requireNonNull(type, name + "not found");
        return type.getDeclaredConstructor().newInstance();
    }
}
