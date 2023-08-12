import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

import javassist.*;

public class Context {
    @SuppressWarnings("rawtypes")
    static Map<String, Class> types = new HashMap<>();

    static Map<String, Object> beans = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
    static void register(String name, Class type) {
        types.put(name, type);
    }

    @SuppressWarnings({"uncheked", "rawtypes"})
    public static void autoRegister() {
        try {
            URL res = Context.class.getResource(
                    "/" + Context.class.getName().replace(".", "/") + ".class");
            Path classPath = new File(res.toURI()).toPath().getParent();
            Files.walk(classPath)
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.toString().endsWith("class"))
                    .map(classPath::relativize)
                    .map(p -> p.toString().replace(File.separatorChar, '.'))
                    .map(n -> n.substring(0, n.length() - 6))
                    .forEach(n -> {
                        try {
                            Class c = Class.forName(n);
                            if (c.isAnnotationPresent(Named.class)) {
                                String simpleName = c.getSimpleName();
                                register(simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1), c);
                            }
                        } catch (ClassNotFoundException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
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
        T object;
        if (Stream.of(type.getDeclaredMethods()).anyMatch(m -> m.isAnnotationPresent(InvokeLog.class))) {
            object = wrap(type).getDeclaredConstructor().newInstance();
        } else {
            object = type.getDeclaredConstructor().newInstance();
        }
        inject(type, object);
        return object;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> wrap(Class<T> type) {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass orgCls = pool.get(type.getName());

            CtClass cls = pool.makeClass(type.getName() + "$$");
            cls.setSuperclass(orgCls);

            for (CtMethod method : orgCls.getDeclaredMethods()) {
                if (!method.hasAnnotation(InvokeLog.class)) {
                    continue;
                }
                CtMethod newMethod = new CtMethod(method.getReturnType(), method.getName(), method.getParameterTypes(), cls);
                newMethod.setExceptionTypes(method.getExceptionTypes());
                newMethod.setBody(
                        "{"
                        + "    System.out.println(java.time.LocalDateTime.now() + "
                        + "\":" + method.getName() + " invoked.\"); "
                        + "    return super." + method.getName() + "($$);"
                        + "}");
                cls.addMethod(newMethod);
            }
            return cls.toClass();
        } catch (NotFoundException | CannotCompileException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static<T> void inject(Class<T> type, T object) throws IllegalArgumentException, IllegalAccessException {
        for (Field field : type.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            field.setAccessible(true);
            field.set(object,getBean(field.getName()));
        }
    }

}
