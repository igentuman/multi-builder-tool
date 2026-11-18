package igentuman.mbtool.client.render;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

/** LRU ownership of preview resources, bounded by both entry count and GPU allocation size. */
final class PreviewCache<K, V> {
    private record Entry<V>(V value, long bytes) {}

    private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(16, 0.75f, true);
    private final long maxBytes;
    private final Consumer<V> disposer;
    private int capacity;
    private long bytes;

    PreviewCache(int capacity, long maxBytes, Consumer<V> disposer) {
        this.capacity = Math.max(1, capacity);
        this.maxBytes = maxBytes;
        this.disposer = disposer;
    }

    V get(K key) {
        Entry<V> entry = entries.get(key);
        return entry == null ? null : entry.value();
    }

    void put(K key, V value, long allocationBytes) {
        if (allocationBytes < 0 || allocationBytes > maxBytes) {
            throw new IllegalArgumentException("Preview allocation exceeds cache budget");
        }
        Entry<V> previous = entries.remove(key);
        if (previous != null) dispose(previous);
        while (!entries.isEmpty() && (entries.size() >= capacity || bytes + allocationBytes > maxBytes)) {
            evictOldest();
        }
        entries.put(key, new Entry<>(value, allocationBytes));
        bytes += allocationBytes;
    }

    void setCapacity(int capacity) {
        this.capacity = Math.max(1, capacity);
        while (entries.size() > this.capacity) evictOldest();
    }

    void clear() {
        entries.values().forEach(this::dispose);
        entries.clear();
    }

    private void evictOldest() {
        var iterator = entries.values().iterator();
        Entry<V> entry = iterator.next();
        iterator.remove();
        dispose(entry);
    }

    private void dispose(Entry<V> entry) {
        bytes -= entry.bytes();
        disposer.accept(entry.value());
    }
}
