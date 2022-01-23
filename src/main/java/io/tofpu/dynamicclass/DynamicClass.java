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
import java.util.stream.Collectors;

public final class DynamicClass {
    private static final Map<Class<?>, Object> OBJECT_MAP = new HashMap<>();

    /**
     * This method will scan through the given package for classes that are
     * annotated with {@link AutoRegister}. then, it'll attempt to create
     * a new instance of said class with {@link #scan(Collection)} method.
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
            e.printStackTrace();
        }
    }

    /**
     * This method can be used when both of the scan method doesn't work properly.
     * This method scan through the given classes, which then attempts to create
     * a new instance of said class.
     *
     * @param classes classes that is to be scanned
     *
     * @throws IllegalStateException when one of the classes has a non-suitable
     * constructor
     */
    public static void scan(final Collection<Class<?>> classes) {
        try {
            final List<Class<?>> decoupledClasses = new ArrayList<>();
            for (final Class<?> clazz : classes) {
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
        } catch (InvalidConstructorException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * This method is to be used when the primary {@link #scan(String)} doesn't work
     * properly. this alternated method basically scans through the given package for
     * classes with {@link ClassPath} (which are marked beta, as of Guice 17.0).
     * <p>
     * Once that's done, it'll attempt to create new instance of said class with
     * {@link #scan(Collection)} method.
     *
     * @param classLoader your classloader, see {@link Class#getClassLoader()}
     * @param packageName your package directory
     *
     * @throws IllegalStateException when one of the classes has a non-suitable
     * constructor
     * @throws IOException if the attempt to read class path resources (jar files or
     * directories) failed.
     * @see #scan(String)
     * @see #scan(Collection)
     */
    public static void alternativeScan(final ClassLoader classLoader, final String packageName) throws IOException {
        final List<Class<?>> classes = ClassPath.from(classLoader)
                .getTopLevelClasses()
                .stream()
                .filter(classInfo -> classInfo.getPackageName().contains(packageName))
                .map(ClassPath.ClassInfo::load)
                .collect(Collectors.toList());

        scan(classes);
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
                final Object object = getDeepObject(parameter);

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

    private static Object getDeepObject(final Class<?> clazz) {
        Object object = null;
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
            }
            object = entry.getValue();
        }
        return object;
    }

    private static boolean recursionClassScan(final Class<?> target, final Object object, final Class<?> clazz) {
        // reassign the entrySuperClass's superclass to entrySuperClass variable
        final Class<?> superClass = clazz.getSuperclass();
        Class<?> interfaceClass = null;

        if (superClass != null && !superClass.isInstance(Object.class)) {
            System.out.println(superClass);
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
