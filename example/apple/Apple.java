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
