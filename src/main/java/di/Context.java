package di;

import di.annotation.InvokeLog;
import di.annotation.RequestScoped;
import javassist.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Context {

    static Map<String, Class> types = new HashMap<>();

    static Map<String, Object> beans = new HashMap<>();

    static ThreadLocal<Map<String, Object>> requestBeans = new ThreadLocal<>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void autoRegister() {
        try {
            URL res = Context.class.getResource(
                    "/" + Context.class.getName().replace('.', '/') + ".class");
            Path classPath = new File(res.toURI()).toPath().getParent().getParent();
            Files.walk(classPath)
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.toString().endsWith(".class"))
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

    @SuppressWarnings("rawtypes")
    public static void register(String name, Class type) {
        types.put(name, type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object getBean(String name) {
        Class type = types.get(name);
        Objects.requireNonNull(type, name + " not found.");

        Map<String, Object> scope;
        if (type.isAnnotationPresent(RequestScoped.class)) {
            scope = requestBeans.get();
            // if (Objects.isNull(scope)) {
            if (null == scope) {
                scope = new HashMap<>();
                requestBeans.set(scope);
            }
        } else {
            scope = beans;
        }

        return scope.computeIfAbsent(name, (java.lang.String key) -> {
            try {
                return createObject(type);
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new RuntimeException(name + " can not instanciate", ex);
            }
        });
    }

    private static <T> T createObject(Class<T> type) throws InstantiationException, IllegalAccessException {
        T object;
        if (Stream.of(type.getDeclaredMethods()).anyMatch(m -> m.isAnnotationPresent(InvokeLog.class))) {
            object = wrap(type).newInstance();
        } else {
            object = type.newInstance();
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
                CtMethod newMethod = new CtMethod(
                        method.getReturnType(), method.getName(), method.getParameterTypes(), cls);
                newMethod.setExceptionTypes(method.getExceptionTypes());
                newMethod.setBody(
                        "{"
                                + "  System.out.println(java.time.LocalDateTime.now() + "
                                + "\":" + method.getName() + " invoked.\"); "
                                + "  return super." + method.getName() + "($$);"
                                + "}");
                cls.addMethod(newMethod);
            }
            return cls.toClass();
        } catch (NotFoundException | CannotCompileException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <T> void inject(Class<T> type, T object) throws IllegalArgumentException, IllegalAccessException {
        for (Field field : type.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            field.setAccessible(true);
            Object bean;
            if (!type.isAnnotationPresent(RequestScoped.class) && field.getType().isAnnotationPresent(RequestScoped.class)) {
                bean = scopeWrapper(field.getType(), field.getName());
            } else {
                bean = getBean(field.getName());
            }
            field.set(object, bean);
        }
    }

    private static final Set<String> cannotOverrides = Stream.of("finalize", "clone").collect(Collectors.toSet());

    @SuppressWarnings("unchecked")
    private static <T> T scopeWrapper(Class<T> type, String name) {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass cls = pool.getOrNull(type.getName() + "$$_");
            if (null == cls) {
                CtClass orgCls = pool.get(type.getName());
                cls = pool.makeClass(type.getName() + "$$_");
                cls.setSuperclass(orgCls);

                CtClass tl = pool.get(LocalCache.class.getName());
                CtField org = new CtField(tl, "org", cls);
                cls.addField(org, "new " + LocalCache.class.getName() + "(\"" + name + "\");");

                for (CtMethod method : orgCls.getMethods()) {
                    if (Modifier.isFinal(method.getModifiers()) | cannotOverrides.contains(method.getName())) {
                        continue;
                    }
                    CtMethod override = new CtMethod(method.getReturnType(), method.getName(), method.getParameterTypes(), cls);
                    override.setExceptionTypes(method.getExceptionTypes());
                    override.setBody(
                            "{"
                                    + "  return ((" + type.getName() + ")org.get())." + method.getName() + "($$);"
                                    + "}");
                    cls.addMethod(override);
                }
            }
            return (T) cls.toClass().newInstance();
        } catch (NotFoundException | CannotCompileException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
