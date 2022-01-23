package example;

import example.apple.Apple;
import example.apple.registry.AppleRegistry;
import io.tofpu.dynamicclass.DynamicClass;

import java.util.Optional;

public final class Main {
    private static final String PACKAGE_NAME = "io.tofpu.example";

    public static void main(String[] args) {
        // scans through the given package. classes
        // that are annotated with @AutoRegister
        // shall be invoked
        DynamicClass.scan(PACKAGE_NAME);

        // retrieves an instance of our AppleRegistry from DynamicClass
        final AppleRegistry appleRegistry = DynamicClass.getInstance(AppleRegistry.class);

        // checking if the apple registry is null
        // if the scan method worked properly, it shouldn't return null
        // otherwise, try the alternative method; at worse case scenario
        // retrieve the classes, and use scan(Collection) method.
        if (appleRegistry == null) {
            throw new RuntimeException("AppleRegistry is null");
        }

        // creating an instance of apple, and stores it to the appleRegistry's map
        final Apple apple = appleRegistry.createApple(69);
        // storing a meta data to our apple object
        apple.store("secret", "this is a secret!!!");
        System.out.println(apple.getMetaDataMap());

        // retrieving our apple object from the apple registry & printing out the
        // secret key data
        final Optional<Apple> optionalApple = appleRegistry.findAppleBy(69);
        optionalApple.ifPresent(result -> System.out.println(apple.findBy("secret")));
    }
}
