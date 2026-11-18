package com.example.fxsuite.avatar.bench.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Where a bench's heavy work is done, away from the screen: what is asked of it ({@code A}, a record of the bench as
 * it is when asked) made into what is shown ({@code M}), one after another on a worker of its own. Only the newest
 * asked is done: one asked before another, not yet begun when the other comes, is passed over; what is made after
 * something newer was asked is not shown. Asked by message ({@code A} on the bus), and what it makes published on the
 * screen's thread ({@code M}), with whether it is at work ({@link Bus.Working}) and what went wrong
 * ({@link Bus.Problems}).
 *
 * <p>What it makes is made in {@link Stage}s, each kept and made again only when what it depends on (its key) has
 * changed: so a request passed over owes nothing to the next, which finds each stage still keyed by what it was last
 * made from.
 *
 * @param <A> what is asked
 * @param <M> what is made of it, to be shown
 */
public final class Workshop<A, M> {

    private final String name;
    private final Bus bus;
    private final Function<A, M> make;
    private final java.util.concurrent.Executor shown;
    private final AtomicLong asked = new AtomicLong();
    private final ExecutorService worker;
    private final List<Stage<?>> stages = new ArrayList<>();

    /**
     * A workshop named {@code name} (its worker's name), asked by {@code kind} on {@code bus}, making each into what
     * is shown by {@code make}, which may ask its stages for what they make.
     */
    public Workshop(final String name, final Bus bus, final Class<A> kind, final Function<A, M> make) {
        this(name, bus, kind, make, Bus.SCREEN);
    }

    /** The same, what it makes published on {@code shown}, not the screen's thread: for a bench with no screen, a test. */
    public Workshop(final String name, final Bus bus, final Class<A> kind, final Function<A, M> make,
                    final java.util.concurrent.Executor shown) {
        this.name = name;
        this.bus = bus;
        this.make = make;
        this.shown = shown;
        this.worker = Executors.newSingleThreadExecutor(r -> {
            final Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            return thread;
        });
        bus.subscribe(kind, this::ask, Bus.HERE);
    }

    /**
     * A stage of the work, named {@code name}: made by {@code make} from what is asked, kept, and made again only when
     * {@code key} of what is asked is not what it was last made from. Asked for only on the worker.
     */
    public <T> Stage<T> stage(final String name, final Function<A, ?> key, final Function<A, T> make) {
        final Stage<T> stage = new Stage<>(name, key, make);
        stages.add(stage);
        return stage;
    }

    /** {@code a} asked: done away from the screen, unless something newer is asked before it is begun. */
    public void ask(final A a) {
        final long ticket = asked.incrementAndGet();
        bus.publish(new Bus.Working(true, name));
        worker.submit(() -> work(ticket, a));
    }

    private void work(final long ticket, final A a) {
        if (ticket != asked.get()) {
            return;
        }
        try {
            final M made = make.apply(a);
            shown.execute(() -> {
                if (ticket == asked.get()) {
                    bus.publish(made);
                    bus.publish(new Bus.Working(false, name));
                }
            });
        } catch (final RuntimeException e) {
            shown.execute(() -> {
                if (ticket == asked.get()) {
                    bus.publish(new Bus.Problems(String.valueOf(e.getMessage())));
                    bus.publish(new Bus.Working(false, name));
                }
            });
        }
    }

    /** How long each stage last took to make, and how many times each has been made. */
    public String timings() {
        final StringBuilder text = new StringBuilder();
        for (final Stage<?> s : stages) {
            text.append(text.isEmpty() ? "" : "; ").append(String.format(Locale.ROOT, "%s %.1f ms (%d)", s.name,
                    s.millis, s.made));
        }
        return text.toString();
    }

    /** Stops its worker. */
    public void close() {
        worker.shutdownNow();
    }

    /**
     * A stage of the work: what it makes from what is asked, kept with the key it was made from; asked again with the
     * same key, the same. If making it fails, nothing is kept, so it is made again next time.
     */
    public final class Stage<T> {

        private final String name;
        private final Function<A, ?> key;
        private final Function<A, T> make;
        private boolean kept;
        private Object keptKey;
        private T value;
        private double millis;
        private long made;

        private Stage(final String name, final Function<A, ?> key, final Function<A, T> make) {
            this.name = name;
            this.key = key;
            this.make = make;
        }

        /** What it makes of {@code a}: as kept, if {@code a}'s key is what it was made from, else made again. */
        public T of(final A a) {
            final Object k = key.apply(a);
            if (kept && Objects.equals(k, keptKey)) {
                return value;
            }
            kept = false;
            final long started = System.nanoTime();
            value = make.apply(a);
            millis = (System.nanoTime() - started) / 1e6;
            made++;
            keptKey = k;
            kept = true;
            return value;
        }

        /** How long it last took to make, in ms. */
        public double millis() {
            return millis;
        }

        /** How many times it has been made. */
        public long made() {
            return made;
        }
    }
}
