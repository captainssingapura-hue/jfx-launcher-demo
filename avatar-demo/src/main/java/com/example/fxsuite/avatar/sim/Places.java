package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The world's level of the index: things, each once, by their boxes ({@link Box}, in the world), the ground cut into
 * square cells seen from above, each listing the things whose boxes reach into it; one reaching over very many cells
 * listed apart, as near everywhere. A tower is one thing here, not its thousands of walls: its own inside
 * ({@link Placed}) answers for those. Asked what is near a box, it says which things meet it, in the order they were
 * added, in a few cells' lookups however many things there are. Moved, a thing is filed again only when its box
 * reaches other cells.
 */
public final class Places<T> {

    /** How big a cell is by default, each way: 20 m, about a house or two. */
    public static final double CELL = 2000;
    /** A thing over more cells than this is listed as everywhere. */
    private static final int WIDE = 256;

    /** A thing, its box, and when it was added: what its order is. */
    private static final class Entry<T> {
        final T thing;
        final long order;
        Box box;
        long x0;
        long x1;
        long z0;
        long z1;

        Entry(final T thing, final long order) {
            this.thing = thing;
            this.order = order;
        }

        boolean wide() {
            return (x1 - x0 + 1) * (z1 - z0 + 1) > WIDE;
        }
    }

    private final double cell;
    private final Map<Long, List<Entry<T>>> cells = new HashMap<>();
    private final List<Entry<T>> everywhere = new ArrayList<>();
    private final Map<T, Entry<T>> entries = new IdentityHashMap<>();
    private long added;

    public Places() {
        this(CELL);
    }

    public Places(final double cell) {
        if (!(cell > 0)) {
            throw new IllegalArgumentException("cells some size: " + cell);
        }
        this.cell = cell;
    }

    private long index(final double at) {
        return (long) Math.floor(at / cell);
    }

    private static long key(final long cx, final long cz) {
        return (cx << 32) ^ (cz & 0xffffffffL);
    }

    /** Files a thing by its box; a thing already here is an error: it is moved, not added again. */
    public void add(final T thing, final Box box) {
        if (entries.containsKey(thing)) {
            throw new IllegalArgumentException("already placed: " + thing);
        }
        final Entry<T> entry = new Entry<>(thing, added++);
        entries.put(thing, entry);
        file(entry, box);
    }

    /** Its box now: filed again only where the cells it reaches change. */
    public void move(final T thing, final Box box) {
        final Entry<T> entry = entry(thing);
        if (index(box.minX()) == entry.x0 && index(box.maxX()) == entry.x1 && index(box.minZ()) == entry.z0
                && index(box.maxZ()) == entry.z1) {
            entry.box = box;
            return;
        }
        unfile(entry);
        file(entry, box);
    }

    /** Takes a thing away; one not here is an error. */
    public void remove(final T thing) {
        unfile(entry(thing));
        entries.remove(thing);
    }

    public boolean has(final T thing) {
        return entries.containsKey(thing);
    }

    /** Its box, as last filed. */
    public Box box(final T thing) {
        return entry(thing).box;
    }

    public int size() {
        return entries.size();
    }

    /** The things whose boxes meet a box: each once, in the order they were added. */
    public List<T> near(final Box box) {
        final List<Entry<T>> found = new ArrayList<>();
        for (final Entry<T> e : everywhere) {
            if (e.box.meets(box)) {
                found.add(e);
            }
        }
        for (long cx = index(box.minX()); cx <= index(box.maxX()); cx++) {
            for (long cz = index(box.minZ()); cz <= index(box.maxZ()); cz++) {
                final List<Entry<T>> in = cells.get(key(cx, cz));
                if (in == null) {
                    continue;
                }
                for (final Entry<T> e : in) {
                    if (e.box.meets(box)) {
                        found.add(e);
                    }
                }
            }
        }
        // in order, each once (one in several cells is listed in each)
        found.sort((a, b) -> Long.compare(a.order, b.order));
        final List<T> things = new ArrayList<>(found.size());
        Entry<T> last = null;
        for (final Entry<T> e : found) {
            if (e != last) {
                things.add(e.thing);
            }
            last = e;
        }
        return things;
    }

    private Entry<T> entry(final T thing) {
        final Entry<T> entry = entries.get(thing);
        if (entry == null) {
            throw new IllegalArgumentException("not placed: " + thing);
        }
        return entry;
    }

    private void file(final Entry<T> entry, final Box box) {
        entry.box = box;
        entry.x0 = index(box.minX());
        entry.x1 = index(box.maxX());
        entry.z0 = index(box.minZ());
        entry.z1 = index(box.maxZ());
        if (entry.wide()) {
            everywhere.add(entry);
            return;
        }
        for (long cx = entry.x0; cx <= entry.x1; cx++) {
            for (long cz = entry.z0; cz <= entry.z1; cz++) {
                cells.computeIfAbsent(key(cx, cz), k -> new ArrayList<>()).add(entry);
            }
        }
    }

    private void unfile(final Entry<T> entry) {
        if (entry.wide()) {
            everywhere.remove(entry);
            return;
        }
        for (long cx = entry.x0; cx <= entry.x1; cx++) {
            for (long cz = entry.z0; cz <= entry.z1; cz++) {
                final long key = key(cx, cz);
                final List<Entry<T>> in = cells.get(key);
                in.remove(entry);
                if (in.isEmpty()) {
                    cells.remove(key);
                }
            }
        }
    }
}
