package com.example.fxsuite.avatar.bench.kit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A bench's workshop: its stages made again only when what they are made from changes, a failed one made again next
 * time; only what was asked last shown, however fast it is asked, nothing asked left undone; and its messages heard by
 * whoever subscribed to them.
 */
class WorkshopTest {

    private record Asked(int design, int look) {
    }

    private record Made(Asked asked, String design, String look) {
    }

    @Test
    void aStageIsMadeAgainOnlyWhenWhatItIsMadeFromChanges() throws Exception {
        final Bus bus = new Bus();
        final AtomicInteger designs = new AtomicInteger();
        final List<Made> shown = Collections.synchronizedList(new ArrayList<>());
        final Holder<Workshop<Asked, Made>.Stage<String>> design = new Holder<>();
        final Workshop<Asked, Made> workshop = new Workshop<>("test", bus, Asked.class,
                a -> new Made(a, design.value.of(a), "look " + a.look()), Bus.HERE);
        design.value = workshop.stage("design", Asked::design, a -> "design " + a.design() + " #"
                + designs.incrementAndGet());
        bus.subscribe(Made.class, shown::add, Bus.HERE);
        for (final Asked a : List.of(new Asked(1, 1), new Asked(1, 2), new Asked(2, 2), new Asked(2, 3))) {
            final CountDownLatch done = done(bus);
            bus.publish(a);
            assertTrue(done.await(5, TimeUnit.SECONDS));
        }
        assertEquals(List.of("design 1 #1", "design 1 #1", "design 2 #2", "design 2 #2"),
                shown.stream().map(Made::design).toList());
        assertEquals(2, design.value.made());
        workshop.close();
    }

    @Test
    void onlyWhatWasAskedLastIsShownAndNothingAskedIsLeftUndone() throws Exception {
        final Bus bus = new Bus();
        final CountDownLatch hold = new CountDownLatch(1);
        final List<Made> shown = Collections.synchronizedList(new ArrayList<>());
        final Holder<Workshop<Asked, Made>.Stage<String>> design = new Holder<>();
        final Workshop<Asked, Made> workshop = new Workshop<>("test", bus, Asked.class, a -> {
            try {
                hold.await(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new Made(a, design.value.of(a), "look " + a.look());
        }, Bus.HERE);
        design.value = workshop.stage("design", Asked::design, a -> "design " + a.design());
        bus.subscribe(Made.class, shown::add, Bus.HERE);
        final CountDownLatch done = done(bus);
        // the first held at work; the rest asked meanwhile, each passing over the one before
        bus.publish(new Asked(1, 1));
        for (int k = 2; k <= 20; k++) {
            bus.publish(new Asked(k, k));
        }
        hold.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(1, shown.size(), "the first, made after more was asked, not shown; the last shown: " + shown);
        assertEquals(new Made(new Asked(20, 20), "design 20", "look 20"), shown.getFirst());
        workshop.close();
    }

    @Test
    void aFailureIsAProblemShownAndTheStageMadeAgainNextTime() throws Exception {
        final Bus bus = new Bus();
        final AtomicInteger tries = new AtomicInteger();
        final List<String> problems = Collections.synchronizedList(new ArrayList<>());
        final List<Made> shown = Collections.synchronizedList(new ArrayList<>());
        final Holder<Workshop<Asked, Made>.Stage<String>> design = new Holder<>();
        final Workshop<Asked, Made> workshop = new Workshop<>("test", bus, Asked.class,
                a -> new Made(a, design.value.of(a), ""), Bus.HERE);
        design.value = workshop.stage("design", Asked::design, a -> {
            if (tries.incrementAndGet() == 1) {
                throw new IllegalStateException("not yet");
            }
            return "design " + a.design();
        });
        bus.subscribe(Bus.Problems.class, p -> problems.add(p.text()), Bus.HERE);
        bus.subscribe(Made.class, shown::add, Bus.HERE);
        for (int k = 0; k < 2; k++) {
            final CountDownLatch done = done(bus);
            bus.publish(new Asked(1, 1));
            assertTrue(done.await(5, TimeUnit.SECONDS));
        }
        assertEquals(List.of("not yet"), problems);
        assertEquals(List.of("design 1"), shown.stream().map(Made::design).toList());
        workshop.close();
    }

    @Test
    void aMessageReachesWhoeverSubscribedToItsKindInTheOrderTheySubscribed() {
        final Bus bus = new Bus();
        final List<String> heard = new ArrayList<>();
        bus.subscribe(Bus.Figures.class, f -> heard.add("a " + f.text()), Bus.HERE);
        bus.subscribe(Object.class, m -> heard.add("all " + m.getClass().getSimpleName()), Bus.HERE);
        bus.subscribe(Bus.Problems.class, p -> heard.add("b " + p.text()), Bus.HERE);
        bus.publish(new Bus.Figures("1 m"));
        bus.publish(new Bus.Problems("none"));
        assertEquals(List.of("a 1 m", "all Figures", "all Problems", "b none"), heard);
    }

    /** Counted down when the workshop is next done with what it was asked, shown or failed. */
    private static CountDownLatch done(final Bus bus) {
        final CountDownLatch done = new CountDownLatch(1);
        bus.subscribe(Bus.Working.class, w -> {
            if (!w.busy()) {
                done.countDown();
            }
        }, Bus.HERE);
        return done;
    }

    private static final class Holder<T> {
        T value;
    }
}
