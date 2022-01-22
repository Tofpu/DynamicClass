package io.tofpu.dynamicclass;

import io.tofpu.dynamicclass.exception.InvalidConstructorException;
import io.tofpu.dynamicclass.meta.AutoRegister;
import io.tofpu.dynamicclass.util.ClassFinder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DynamicClass {
    private static final Map<Class<?>, Object> OBJECT_MAP = new HashMap<>();

    /**
     * This method will scan through the given package for classes that are
     * annotated with {@link AutoRegister}. then, it'll attempt to create
     * a new instance of said class.
     *
     * @param packageName your package directory
     *
     * @throws IllegalStateException when one of the classes has a non-suitable
     * constructor
     */
    public static void scan(final String packageName) {
        try {
            final List<Class<?>> decoupledClasses = new ArrayList<>();
            for (final Class<?> clazz : ClassFinder.getClasses(packageName)) {
                if (!clazz.isAnnotationPresent(AutoRegister.class) || OBJECT_MAP.containsKey(clazz)) {
                    continue;
                }

                try {
                    addParameter(newInstance(clazz));
                } catch (final IllegalStateException ex) {
                    // there's a possibility that there's a decoupled class involved here
                    decoupledClasses.add(clazz);
                }
            }

            // re-attempting to create an instance of the class
            for (final Class<?> clazz : decoupledClasses) {
                addParameter(newInstance(clazz));
            }
        } catch (ClassNotFoundException | IOException | InvalidConstructorException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void addParameter(final Object object) {
        OBJECT_MAP.put(object.getClass(), object);
    }

    /**
     * @param clazz the class you want to create an instance of
     *
     * @return a new instance of said class
     *
     * @throws InvalidConstructorException when given class has a
     * non-suitable constructor
     */
    private static Object newInstance(final Class<?> clazz) throws InvalidConstructorException {
        Object clazzInstance = null;
        for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            final List<Object> objects = new ArrayList<>();

            boolean pause = false;
            for (final Class<?> parameter : constructor.getParameterTypes()) {
                final Object object = OBJECT_MAP.get(parameter);
                if (object == null) {
                    pause = true;
                    break;
                } else {
                    objects.add(object);
                }
            }

            if (!pause) {
                try {
                    clazzInstance = constructor.newInstance(objects.toArray());
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        if (clazzInstance == null) {
            try {
                clazzInstance = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new InvalidConstructorException(clazz);
            }
        }
        return clazzInstance;
    }

    /**
     * This method will return an instance of the given class, if applicable, of course.
     *
     * @param clazz the class instance you want to receive
     *
     * @return an instance of the class, or null if it doesn't exist
     */
    public static <T> T getInstance(final Class<T> clazz) {
        return (T) OBJECT_MAP.get(clazz);
    }

    /**
     * This method can be incredibly useful to use when one of the classes that is
     * annotated with {@link AutoRegister} has a constructor, and the
     * constructor's parameters, for whatever reason, cannot be annotated with
     * {@link AutoRegister}.
     *
     * @param objects an instances of classes that you want to manually register
     */
    public static void addParameters(final Object... objects) {
        for (final Object object : objects) {
            addParameter(object);
        }
    }
}
