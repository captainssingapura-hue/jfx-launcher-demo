package com.example.fxsuite.avatar.sim;

import java.util.Arrays;

/**
 * What is kept in each cell of a grid on the ground, by the cell's two indices ({@link #key}): asked many times a
 * frame, so asked without making anything. Its keys spread before they are placed, so a row, a column or a diagonal
 * of cells does not crowd into a few places, as a key whose halves are only put together would.
 *
 * <p>Filled as it is made, then asked: from any number of threads at once, once it is no longer filled.
 *
 * @param <V> what each cell keeps; never null
 */
public final class CellMap<V> {

    private long[] keys = new long[16];
    private Object[] values = new Object[16];
    private int size;

    /** The key of the cell {@code i} across, {@code j} along. */
    public static long key(final long i, final long j) {
        return (i << 32) ^ (j & 0xffff_ffffL);
    }

    /** What cell {@code key} keeps; null if nothing. */
    @SuppressWarnings("unchecked")
    public V get(final long key) {
        final int mask = keys.length - 1;
        for (int at = spread(key) & mask; ; at = (at + 1) & mask) {
            final Object v = values[at];
            if (v == null) {
                return null;
            }
            if (keys[at] == key) {
                return (V) v;
            }
        }
    }

    /** Keeps {@code value} in cell {@code key}, in place of what it kept. */
    public void put(final long key, final V value) {
        if (value == null) {
            throw new IllegalArgumentException("a cell keeps something");
        }
        if ((size + 1) * 4 > keys.length * 3) {
            grow();
        }
        final int mask = keys.length - 1;
        for (int at = spread(key) & mask; ; at = (at + 1) & mask) {
            if (values[at] == null) {
                keys[at] = key;
                values[at] = value;
                size++;
                return;
            }
            if (keys[at] == key) {
                values[at] = value;
                return;
            }
        }
    }

    /** How many cells keep something. */
    public int size() {
        return size;
    }

    /** Each cell's key and what it keeps, in no order. */
    public void forEach(final Each<? super V> each) {
        for (int k = 0; k < keys.length; k++) {
            if (values[k] != null) {
                @SuppressWarnings("unchecked") final V v = (V) values[k];
                each.accept(keys[k], v);
            }
        }
    }

    /** What is done with each cell's key and what it keeps. */
    @FunctionalInterface
    public interface Each<V> {
        void accept(long key, V value);
    }

    /** What is kept in cell {@code key}, kept there first by {@code make} if it is empty. */
    public V computeIfAbsent(final long key, final java.util.function.Supplier<V> make) {
        V v = get(key);
        if (v == null) {
            v = make.get();
            put(key, v);
        }
        return v;
    }

    private void grow() {
        final long[] oldKeys = keys;
        final Object[] oldValues = values;
        keys = new long[oldKeys.length * 2];
        values = new Object[oldValues.length * 2];
        size = 0;
        for (int k = 0; k < oldKeys.length; k++) {
            if (oldValues[k] != null) {
                @SuppressWarnings("unchecked") final V v = (V) oldValues[k];
                put(oldKeys[k], v);
            }
        }
    }

    // every bit of the key bearing on every bit of where it goes
    private static int spread(final long key) {
        long h = key;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (int) h;
    }

    /** A list of numbers kept while a map is filled, each cell's added to one at a time. */
    public static final class Ints {

        private int[] items = new int[4];
        private int size;

        public void add(final int i) {
            if (size == items.length) {
                items = Arrays.copyOf(items, size * 2);
            }
            items[size++] = i;
        }

        public int size() {
            return size;
        }

        public int last() {
            return items[size - 1];
        }

        public int[] toArray() {
            return Arrays.copyOf(items, size);
        }
    }
}
