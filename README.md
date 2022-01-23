<div align="center">
  <h1>DynamicClass</h1>
  <p>DynamicClass is a simple, lightweight & easy to use utility which helps you remove bloat from your code.</p>
</div>

# How to use
It's extremely simple! All you need to do is, annotate the class that you'd want to be automatically created with @AutoRegister, 
and in your application, run DynamicClass#scan method. that's it!

## Maven
```xml
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>

        <dependency>
            <groupId>com.github.Tofpu</groupId>
            <artifactId>DynamicClass</artifactId>
            <version>7f47fbf403</version>
        </dependency>
```

## Example:
```java
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
```

```java
package example.apple;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class Apple {
    private final @NotNull Integer id;
    private final @NotNull Map<String, Object> metaDataMap;

    public Apple(final @NotNull Integer id) {
        this.id = id;
        this.metaDataMap = new HashMap<>();
    }

    public @NotNull Object store(final @NotNull String key, final @NotNull Object data) {
        metaDataMap.put(key, data);
        return data;
    }

    public @Nullable Object findBy(final @NotNull String key) {
        return metaDataMap.get(key);
    }

    public @Nullable Object remove(final @NotNull String key) {
        return metaDataMap.remove(key);
    }

    public Map<String, Object> getMetaDataMap() {
        return new HashMap<>(metaDataMap);
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Apple{");
        sb.append("id=").append(id);
        sb.append(", metaDataMap=").append(metaDataMap);
        sb.append('}');
        return sb.toString();
    }
}
```

```java
package example.apple.registry;

import example.apple.Apple;
import example.apple.handler.AppleHandler;
import io.tofpu.dynamicclass.meta.AutoRegister;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AutoRegister
public final class AppleRegistry {
    private final @NotNull AppleHandler appleHandler;
    private final @NotNull Map<Integer, Apple> appleMap;

    // DynamicClass works with a constructor as well, so long the parameters
    // have the AutoRegister annotation, or they were manually
    // added with DynamicClass#addParameters method
    //
    // DynamicClass also works with multiple constructors!
    public AppleRegistry(final @NotNull AppleHandler appleHandler) {
        this.appleHandler = appleHandler;
        this.appleMap = new HashMap<>();
    }

    public Apple createApple(final @NotNull Integer appleId) {
        final Apple apple = new Apple(appleId);
        appleMap.put(appleId, apple);

        return apple;
    }

    public Optional<Apple> findAppleBy(final @NotNull Integer appleId) {
        return Optional.ofNullable(appleMap.get(appleId));
    }
}
```

```java
package example.apple.handler;

import io.tofpu.dynamicclass.meta.AutoRegister;

@AutoRegister
public final class AppleHandler {
    // demo class! it's just for testing purposes
}
``` 
