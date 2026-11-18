package com.example.fxsuite.avatar.bench.kit;

import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * A bench's messages, between what is changed on it and what serves it: what is asked (a knob set, a kind changed, a
 * layer shown), what is made of it (a stage's result, figures, problems, whether it is at work), and what a world played
 * from it sees. Each message a small record; whoever subscribes to its kind is given it, on the thread it wants
 * ({@link #SCREEN}, {@link #HERE}): so the controls never call what builds, nor does what builds touch the controls.
 *
 * <p>Published from any thread; subscribed to before it is published to, as a bench is put together.
 */
public final class Bus {

    /** On the screen's thread: at once if on it already, else as soon as it is free. */
    public static final Executor SCREEN = r -> {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    };
    /** On whatever thread it is published on. */
    public static final Executor HERE = Runnable::run;

    private record Subscription<M>(Class<M> kind, Consumer<? super M> to, Executor on) {

        void offer(final Object message) {
            if (kind.isInstance(message)) {
                final M m = kind.cast(message);
                on.execute(() -> to.accept(m));
            }
        }
    }

    private final List<Subscription<?>> subscriptions = new CopyOnWriteArrayList<>();

    /** Every message of kind {@code kind} (or a kind of it) published from now on given to {@code to}, on {@code on}. */
    public <M> void subscribe(final Class<M> kind, final Consumer<? super M> to, final Executor on) {
        subscriptions.add(new Subscription<>(kind, to, on));
    }

    /** {@code message} given to everyone subscribed to its kind, in the order they subscribed. */
    public void publish(final Object message) {
        for (final Subscription<?> s : subscriptions) {
            s.offer(message);
        }
    }

    /** Whether the bench is at work, and on what, or done: from its workshop. */
    public record Working(boolean busy, String what) {
    }

    /** What is wrong, as text; nothing wrong if blank. */
    public record Problems(String text) {
    }

    /** What what is shown comes to, as text. */
    public record Figures(String text) {
    }
}
