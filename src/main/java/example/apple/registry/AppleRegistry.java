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
