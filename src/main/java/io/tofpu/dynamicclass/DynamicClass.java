package io.tofpu.dynamicclass;

import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import io.tofpu.dynamicclass.exception.InvalidConstructorException;
import io.tofpu.dynamicclass.meta.AutoRegister;
import io.tofpu.dynamicclass.util.ClassFinder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public final class DynamicClass {
    private static final Map<Class<?>, Object> OBJECT_MAP = new HashMap<>();

    /**
     * This method will attempt to create a new instance of classes that are
     * annotated with {@link AutoRegister}.
     * <p>
     * If the class has a constructor, and the constructor's parameters are
     * registered with {@link #addParameter(Object)} method, or the parameter
     * class is annotated with {@link AutoRegister} annotation, then the parameter
     * will be created and passed to the constructor.
     * <p>
     * If the class has no constructor, then the instance will be created.
     * <p>
     * If the class has a constructor, but the constructor's parameters two
     * outlined steps failed, then an exception will be thrown.
     *
     * @param packageName your package directory
     *
     * @throws IllegalStateException when one of the classes has a non-suitable
     * constructor
     * @see #alternativeScan(ClassLoader, String)
     * @see #scan(Collection)
     */
    public static void scan(final String packageName) {
        try {
            scan(Lists.newArrayList(ClassFinder.getClasses(packageName)));
        } catch (ClassNotFoundException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * This method is preferred to use when you want to scan through a list of
     * specific classes by your own preferred way.
     * It does not use {@link ClassPath}(which are marked beta,
     * and are not guaranteed to work).
     *
     * @param classes Classes that you want to be scanned
     *
     * @throws IllegalStateException when one of the classes has a non-suitable
     * constructor
     */
    public static void scan(final Collection<Class<?>> classes) {
        try {
            final List<Class<?>> decoupledClasses = new ArrayList<>();
            for (final Class<?> clazz : classes) {
                if (!clazz.isAnnotationPresent(AutoRegister.class) ||
                    OBJECT_MAP.containsKey(clazz)) {
                    continue;
                }

                try {
                    final Object object = newInstance(clazz);
                    if (object == null) {
                        decoupledClasses.add(clazz);
                        continue;
                    }
                    addParameter(object);
                } catch (final IllegalStateException | InvalidConstructorException ex) {
                    // there's a possibility that there's a decoupled class involved here
                    decoupledClasses.add(clazz);
                }
            }

            // re-attempting to create an instance of the class
            for (final Class<?> clazz : decoupledClasses) {
                addParameter(newInstance(clazz));
            }
        } catch (InvalidConstructorException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * This method is to be used when {@link #scan(String)} method is not working.
     * It will scan through all the classes in the classpath, with
     * the given classloader, and pass the classes to {@link #scan(Collection)} method.
     *
     * @param classLoader Your classloader, see {@link Class#getClassLoader()}
     * @param packageName Your package directory
     *
     * @throws IllegalStateException when one of the classes has a non-suitable
     * constructor
     * @throws IOException if the attempt to read class path resources (jar files or
     * directories) failed.
     * @see #scan(String)
     * @see #scan(Collection)
     */
    public static void alternativeScan(final ClassLoader classLoader, final String packageName) throws IOException {
        final List<Class<?>> classes = new ArrayList<>();
        for (final ClassPath.ClassInfo clazz : ClassPath.from(classLoader)
                .getTopLevelClasses()) {
            if (!clazz.getPackageName()
                    .contains(packageName)) {
                continue;
            }

            try {
                classes.add(clazz.load());
            } catch (final IllegalStateException | NoClassDefFoundError ignored) {
                // ignore the exception
            }
        }

        scan(classes);
    }

    private static void addParameter(final Object object) {
        if (object == null) {
            return;
        }
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

            boolean complete = false;
            for (final Class<?> parameter : constructor.getParameterTypes()) {
                final Object object = getDeepObject(parameter);

                if (object == null) {
                    return null;
                }

                objects.add(object);
            }

            if (!complete) {
                try {
                    clazzInstance = constructor.newInstance(objects.toArray());
                } catch (InstantiationException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    throw new InvalidConstructorException(clazz);
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

    private static Object getDeepObject(final Class<?> clazz) {
        Object object = OBJECT_MAP.get(clazz);

        // if the object is found, simply return it
        if (object != null) {
            return object;
        }

        final Map<Class<?>, Object> OBJECT_MAP_COPY = new HashMap<>(OBJECT_MAP);
        for (final Map.Entry<Class<?>, Object> entry : OBJECT_MAP_COPY.entrySet()) {
            final Class<?> entryClass = entry.getKey();
            final Object entryObject = entry.getValue();

            // if the entryClass is not assignable to the given class
            if (!entryClass.isAssignableFrom(clazz)) {
                // then attempt to recurse through the class
                if (!recursionClassScan(clazz, entryObject, entryClass)) {
                    continue;
                }

                object = OBJECT_MAP.get(clazz);

                // if the object is found after the recursion process, simply return it
                if (object != null && clazz.isAssignableFrom(object.getClass())) {
                    return object;
                }
                break;
            }

            // if the entryClass is assignable to the given class
            // then simply return the entryObject
            return entry.getValue();
        }

        // nothing has to be found, return null
        return null;
    }

    private static boolean recursionClassScan(final Class<?> target, final Object object, final Class<?> clazz) {
        // reassign the entrySuperClass's superclass to entrySuperClass variable
        final Class<?> superClass = clazz.getSuperclass();
        Class<?> interfaceClass = null;

        if (superClass != null && !superClass.isInstance(Object.class)) {
            OBJECT_MAP.put(superClass, object);

            for (final Class<?> interfaceClazz : superClass.getInterfaces()) {
                if (!target.isAssignableFrom(interfaceClazz)) {
                    continue;
                }
                interfaceClass = interfaceClazz;
                OBJECT_MAP.put(interfaceClass, object);
            }
        }

        final boolean superAssignable = superClass != null && target.isAssignableFrom(superClass);
        final boolean interfaceAssignable = interfaceClass != null && target.isAssignableFrom(interfaceClass);

        // if the super class is not null & it's not assignable, and interface is not
        // assignable as well
        if ((superClass != null && !superAssignable) && !interfaceAssignable) {
            // then, continue the recursion scan!
            recursionClassScan(target, object, superClass);
        }

        return superAssignable || interfaceAssignable;
    }

    /**
     * Gets the class instance. if the given class instance is not found, it will
     * return null.
     *
     * @param clazz the class instance to get the instance of
     *
     * @return the class instance if found, otherwise null
     */
    public static <T> T getInstance(final Class<T> clazz) {
        return (T) OBJECT_MAP.get(clazz);
    }

    /**
     * Adds the given class instance(s) to the map. if the given class instance
     * is already in the object map, it will be replaced. If the instance is null, it
     * will be ignored.
     *
     * @param objects the class instance you want to register.
     */
    public static void addParameters(final Object... objects) {
        for (final Object object : objects) {
            addParameter(object);
        }
    }
}
