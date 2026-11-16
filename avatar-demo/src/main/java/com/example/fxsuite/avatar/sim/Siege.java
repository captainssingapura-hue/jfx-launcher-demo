package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.IntPredicate;

/**
 * The temple under siege: airships come in from the edge of the world without end, each from
 * somewhere of its own, and make for the temple; over its main hall they circle and bomb it until
 * it falls or they are shot down. On their way in, passing over another building, now and then
 * one lets a few bombs go at it.
 *
 * <p>So many fly at once ({@code ships}), each in a slot of its own (one of the airships built into
 * the course for it, {@link Course#Course(Demo, Siege)}): one shot down, its slot is free for the
 * next once its wreck has fallen ({@link #SETTLE}). They come {@code every} so many seconds, each
 * that much {@code faster} than the last; as fast as {@code speed} (metres a second), as big as
 * {@code size} times the airship to fly; each as tough as {@code shipToughness} times an airship, the
 * temple as {@code templeToughness} times its own. Over their target each drops a bomb every
 * {@code bombEvery} seconds; passing over another building, a stick of {@link #STICK} at it, by
 * {@code stray} chance. When the main hall falls ({@code fallen}): the siege is lost, or the hall is
 * rebuilt {@code rebuildAfter} seconds on, or they go for the next building nearest. Which way each
 * comes, how high, how fast, by {@code seed}: the same seed, the same siege.
 */
public record Siege(int ships, double every, double faster, double speed, double size, double bombEvery,
                    double stray, double templeToughness, double shipToughness, Fallen fallen, double rebuildAfter,
                    long seed) {

    /** What comes of the main hall falling. */
    public enum Fallen {
        /** The siege is lost: no more come, no more bombs fall. */
        LOST("The siege is lost"),
        /** The hall is rebuilt, and the siege goes on. */
        REBUILT("The hall is rebuilt"),
        /** They go for the next building, nearest the last; lost when none stands. */
        NEXT("They bomb the next building");

        private final String title;

        Fallen(final String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    /** How many may fly at once, at most; how often, at most and least, seconds; how much faster each, at most. */
    public static final int MOST_SHIPS = 16;
    public static final double SOONEST = 3;
    public static final double LATEST = 300;
    public static final double FASTEST = 0.2;
    /** Their speed, metres a second, and their size, times the airship to fly, from and to. */
    public static final double SLOWEST = 2;
    public static final double QUICKEST = 30;
    public static final double SMALLEST = 0.5;
    public static final double LARGEST = 3;
    /** How often each bombs, seconds, from and to; how tough, times, from and to. */
    public static final double QUICKEST_BOMBS = 0.25;
    public static final double SLOWEST_BOMBS = 10;
    public static final double WEAKEST = 0.1;
    public static final double TOUGHEST = 10;

    /** As it starts, unless told otherwise: six at a time, one every twenty seconds; the temple lost when its hall falls. */
    public static final Siege STANDARD = new Siege(6, 20, 0, 8, 2, 1, 0.25, 1, 1, Fallen.LOST, 60, 1);

    public Siege {
        check(ships, 1, MOST_SHIPS, "ships");
        check(every, SOONEST, LATEST, "every");
        check(faster, 0, FASTEST, "faster");
        check(speed, SLOWEST, QUICKEST, "speed");
        check(size, SMALLEST, LARGEST, "size");
        check(bombEvery, QUICKEST_BOMBS, SLOWEST_BOMBS, "bombEvery");
        check(stray, 0, 1, "stray");
        check(templeToughness, WEAKEST, TOUGHEST, "templeToughness");
        check(shipToughness, WEAKEST, TOUGHEST, "shipToughness");
        check(rebuildAfter, 1, LATEST, "rebuildAfter");
        if (fallen == null) {
            throw new IllegalArgumentException("fallen: what comes of the hall falling");
        }
    }

    private static void check(final double value, final double least, final double most, final String name) {
        if (!(value >= least && value <= most)) {
            throw new IllegalArgumentException(name + " " + value + ": from " + least + " to " + most);
        }
    }

    /** The same, from a seed of its own. */
    public Siege seeded(final long another) {
        return new Siege(ships, every, faster, speed, size, bombEvery, stray, templeToughness, shipToughness, fallen,
                rebuildAfter, another);
    }

    /** Written as one word, to be read back ({@link #read}): for a recording. */
    public String written() {
        return String.format(Locale.ROOT, "%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d", ships, every, faster, speed, size,
                bombEvery, stray, templeToughness, shipToughness, fallen, rebuildAfter, seed);
    }

    /** Read back from {@link #written}. */
    public static Siege read(final String word) {
        final String[] p = word.split(",");
        if (p.length != 12) {
            throw new IllegalArgumentException("a siege of " + p.length + " settings, not 12: " + word);
        }
        return new Siege(Integer.parseInt(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]), Double.parseDouble(p[6]),
                Double.parseDouble(p[7]), Double.parseDouble(p[8]), Fallen.valueOf(p[9]), Double.parseDouble(p[10]),
                Long.parseLong(p[11]));
    }

    /** When the first comes, seconds after the start. */
    public static final double FIRST = 5;
    /** How long a slot stands empty once its airship is shot down: its wreck falling. */
    public static final double SETTLE = 8;
    /** How many bombs a stick dropped on a building passed over. */
    public static final int STICK = 3;
    /** How far in from the edge of the world each comes from. */
    public static final double ENTRY = Body.EDGE - 3000;
    /** How far over the tallest building they come in, at least, and how much higher, at most, besides. */
    public static final double CLEAR = 2500;
    public static final double HIGHER = 3000;
    /** How far over its target the lowest circles; how far round it, from and to. */
    public static final double OVER = 2500;
    public static final double ORBIT_LEAST = 600;
    public static final double ORBIT_MOST = 1500;

    /** The airships it is built with: one for each slot, the last {@code ships} pieces of the course, all of them broken until they come. */
    public List<Integer> slots(final int pieces) {
        final List<Integer> slots = new ArrayList<>();
        for (int k = 0; k < ships; k++) {
            slots.add(pieces - ships + k);
        }
        return slots;
    }

    /**
     * The world at the start of the siege, from {@code course}: every slot's airship broken, there
     * until it comes; each airship as tough as it is told, the temple as tough as it is told.
     */
    public Damage damage(final Course course) {
        Damage damage = Damage.of(course);
        final List<Integer> slots = slots(course.pieces().size());
        damage = damage.toughened(slots::contains, shipToughness).toughened(i -> Course.temple(course.pieces().get(i)),
                templeToughness);
        for (final int slot : slots) {
            damage = damage.retired(slot);
        }
        return damage;
    }

    /** The sky at the start of the siege, over {@code damage} as it starts. */
    public Sky sky(final Damage damage) {
        int hall = -1;
        double tallest = 0;
        for (int i = 0; i < damage.size(); i++) {
            final Course.Piece piece = damage.piece(i);
            if (piece.kind() == Course.Kind.BUILDING) {
                tallest = Math.max(tallest, piece.block().top());
                if (piece.tint() == Course.MAIN_HALL) {
                    hall = i;
                }
            }
        }
        if (hall < 0) {
            throw new IllegalStateException("no temple to besiege");
        }
        final List<Raider> raiders = new ArrayList<>();
        for (final int slot : slots(damage.size())) {
            raiders.add(new Raider(slot, false, Double.NEGATIVE_INFINITY, -1, -1, 0, Double.NEGATIVE_INFINITY));
        }
        return new Sky(0, List.of(), 0, Map.of(), new State(this, hall, hall, tallest, 0, FIRST, 0, Double.NaN, false,
                raiders));
    }

    /**
     * One slot's airship: which piece; whether it is in the air ({@code flying}), and when it was last
     * shot down; which arrival it is; which building its bombs would fall on, as it last saw
     * ({@code sighted}, -1 none); how many of a stick it has still to drop on it; and when it last dropped one.
     */
    public record Raider(int piece, boolean flying, double downed, long arrival, int sighted, int stick,
                         double dropped) {
    }

    /**
     * The siege as it stands: as it was set ({@code siege}); the main hall ({@code hall}); what they go
     * for now ({@code target}); how high the tallest building is; how many have come and how many have
     * been shot down; when the next is due; when the hall fell (NaN, standing); whether the siege is
     * lost; and each slot.
     */
    public record State(Siege siege, int hall, int target, double tallest, long arrivals, double next, int downed,
                        double fell, boolean lost, List<Raider> raiders) {

        public State {
            raiders = List.copyOf(raiders);
        }

        /** Whether piece {@code piece} is one of its airships. */
        public boolean raider(final int piece) {
            return raiders.stream().anyMatch(r -> r.piece() == piece);
        }

        /** How many are in the air. */
        public int flying() {
            return (int) raiders.stream().filter(Raider::flying).count();
        }

        /** What came of a step: the siege then; airships come, or the hall rebuilt, made whole; airships sent another way; each slot's bay asked open or shut; which drop a bomb. */
        public record Stepped(State state, Map<Integer, Course.Piece> revived, Map<Integer, Course.Piece> rerouted,
                              Map<Integer, Boolean> bays, List<Integer> drops, List<Event> events) {
        }

        /**
         * {@code seconds} on from {@code time}, over {@code damage}: those shot down noted, their slots
         * freed once their wrecks have fallen; the next come, if one is due and a slot is free; the hall
         * fallen, the siege lost, the hall rebuilt, or the next building made the target; and each in the
         * air, its bay asked open while its bombs would fall on its target, or on a building it chanced
         * to bomb, and a bomb let go each time its bay is open ({@code open}) and it is time.
         */
        public Stepped step(final double time, final double seconds, final Damage damage, final IntPredicate open) {
            final double to = time + seconds;
            final Map<Integer, Course.Piece> revived = new HashMap<>();
            final Map<Integer, Course.Piece> rerouted = new HashMap<>();
            final Map<Integer, Boolean> bays = new HashMap<>();
            final List<Integer> drops = new ArrayList<>();
            final List<Event> events = new ArrayList<>();
            final List<Raider> now = new ArrayList<>(raiders);
            int shot = downed;
            // those shot down, out of the air
            for (int k = 0; k < now.size(); k++) {
                final Raider r = now.get(k);
                if (r.flying() && damage.broken(r.piece())) {
                    now.set(k, new Raider(r.piece(), false, time, r.arrival(), -1, 0, r.dropped()));
                    shot++;
                }
            }
            // the hall fallen: lost, rebuilt in time, or the next building their target
            int aim = target;
            double fallen = fell;
            boolean over = lost;
            if (!over && damage.broken(aim)) {
                if (Double.isNaN(fallen)) {
                    fallen = time;
                }
                switch (siege.fallen()) {
                    case LOST -> over = true;
                    case REBUILT -> {
                        if (to - fallen >= siege.rebuildAfter()) {
                            revived.put(aim, damage.piece(aim));
                            fallen = Double.NaN;
                        }
                    }
                    case NEXT -> {
                        final int next = nearest(damage, aim);
                        if (next < 0) {
                            over = true;
                        } else {
                            aim = next;
                            fallen = Double.NaN;
                            for (int k = 0; k < now.size(); k++) {
                                final Raider r = now.get(k);
                                if (r.flying()) {
                                    final Course.Piece ship = damage.piece(r.piece());
                                    rerouted.put(r.piece(), ship.routed(route(ship, k, r.arrival(), ship.at(to).place(),
                                            to, damage.piece(aim), tallest)));
                                }
                            }
                        }
                    }
                }
            }
            // the next, due, into the first slot free
            long came = arrivals;
            double due = next;
            if (!over && to >= due) {
                for (int k = 0; k < now.size(); k++) {
                    final Raider r = now.get(k);
                    if (!r.flying() && to - r.downed() >= SETTLE) {
                        final Course.Piece ship = damage.piece(r.piece());
                        final Raid raid = route(ship, k, came, entry(came), to, damage.piece(aim), tallest);
                        revived.put(r.piece(), ship.routed(raid));
                        now.set(k, new Raider(r.piece(), true, r.downed(), came, -1, 0, Double.NEGATIVE_INFINITY));
                        events.add(new Event.Arrived(r.piece(), new Vec3(raid.start().x(), -raid.start().height(),
                                raid.start().z())));
                        came++;
                        due += Math.max(SOONEST, siege.every() * Math.pow(1 - siege.faster(), came));
                        break;
                    }
                }
            }
            // each in the air: where its bombs would fall; its bay asked open, and a bomb dropped when it is time
            for (int k = 0; k < now.size(); k++) {
                final Raider r = now.get(k);
                if (!r.flying() || revived.containsKey(r.piece())) {
                    continue;
                }
                final Course.Piece ship = rerouted.getOrDefault(r.piece(), damage.piece(r.piece()));
                final int under = over ? -1 : under(ship, time, to, damage, aim);
                int sighted = r.sighted();
                int stick = r.stick();
                // a building passed over, newly: by chance, a stick dropped on it
                if (under >= 0 && under != aim && under != sighted && stick == 0
                        && Num.hash(siege.seed(), r.arrival(), under) < siege.stray()) {
                    stick = STICK;
                }
                sighted = under;
                final boolean bombing = !over && (under == aim && aim >= 0 || stick > 0);
                bays.put(r.piece(), bombing);
                double dropped = r.dropped();
                if (bombing && open.test(r.piece()) && to - dropped >= siege.bombEvery()) {
                    drops.add(r.piece());
                    dropped = to;
                    if (under != aim && stick > 0) {
                        stick--;
                    }
                }
                now.set(k, new Raider(r.piece(), true, r.downed(), r.arrival(), sighted, stick, dropped));
            }
            if (over && !lost) {
                events.add(new Event.Besieged(damage.piece(hall).block().x(), damage.piece(hall).block().z()));
            }
            return new Stepped(new State(siege, hall, aim, tallest, came, due, shot, fallen, over, now), revived,
                    rerouted, bays, drops, events);
        }

        /** Where arrival {@code n} comes from: a heading of its own from the target, as far as the edge of the world, at the height it comes in at. */
        private Place entry(final long n) {
            final SplittableRandom random = new SplittableRandom(Num.mix(siege.seed(), n));
            final double heading = random.nextDouble() * 360;
            final Vec3 way = Num.heading(heading);
            // out from the world's middle to the square's edge that way
            final double reach = ENTRY / Math.max(Math.abs(way.x()), Math.abs(way.z()));
            // (how high, not yet known: as high as it comes in)
            return new Place(way.x() * reach, way.z() * reach, Double.NaN, Num.wrap(heading + 180));
        }

        /**
         * The way arrival {@code n}, in slot {@code k}, goes, from {@code from} at {@code time}: in high over
         * everything, a little higher by chance; straight to {@code target}; round it, the lower the slot the
         * lower, each slot clear of the next, as far round as it chances.
         */
        private Raid route(final Course.Piece ship, final int k, final long n, final Place from, final double time,
                           final Course.Piece target, final double tallest) {
            final SplittableRandom random = new SplittableRandom(Num.mix(siege.seed() + 1, n));
            final double tall = 2 * ship.shape().box().halfUp();
            final double cruise = tallest + CLEAR + tall + random.nextDouble() * HIGHER;
            final double hover = target.block().top() + OVER + k * tall * 1.2;
            final double orbit = ORBIT_LEAST + random.nextDouble() * (ORBIT_MOST - ORBIT_LEAST);
            final double fast = siege.speed() * 100 * (0.85 + 0.3 * random.nextDouble());
            // in from the edge, as high as it comes in; sent another way, from where it is, up to that if lower
            final Place start = Double.isNaN(from.height()) ? new Place(from.x(), from.z(), cruise, from.yaw()) : from;
            return new Raid(time, start, Math.max(cruise, start.height()), target.block().x(), target.block().z(), hover,
                    orbit, Math.max(Raid.CRAWL, fast));
        }

        /**
         * Which building a bomb let go from {@code ship} now would fall on, going as it goes: its target, if
         * there; another building, if one; -1 if none.
         */
        private static int under(final Course.Piece ship, final double time, final double to, final Damage damage,
                                 final int target) {
            final Place at = ship.at(to).place();
            final Place was = ship.at(time).place();
            final Block gondola = Course.part(ship, Course.GONDOLA).placed(at);
            final double seconds = to - time;
            final double vx = seconds > 0 ? (at.x() - was.x()) / seconds : 0;
            final double vz = seconds > 0 ? (at.z() - was.z()) / seconds : 0;
            int other = -1;
            for (int i = 0; i < damage.size(); i++) {
                final Course.Piece piece = damage.piece(i);
                if (piece.kind() != Course.Kind.BUILDING || damage.broken(i)) {
                    continue;
                }
                final double drop = gondola.bottom() - Sky.BELOW - piece.block().top();
                if (drop <= 0) {
                    continue;
                }
                final double fall = Math.sqrt(2 * drop / Body.GRAVITY);
                if (piece.block().covers(gondola.x() + vx * fall, gondola.z() + vz * fall, 0)) {
                    if (i == target) {
                        return i;
                    }
                    other = i;
                }
            }
            return other;
        }

        /** The standing building nearest building {@code from}, not the temple's: -1 if none. */
        private static int nearest(final Damage damage, final int from) {
            final Block was = damage.piece(from).block();
            int best = -1;
            double nearest = Double.POSITIVE_INFINITY;
            for (int i = 0; i < damage.size(); i++) {
                final Course.Piece piece = damage.piece(i);
                if (piece.kind() != Course.Kind.BUILDING || damage.broken(i) || Course.temple(piece)) {
                    continue;
                }
                final double d = Math.hypot(piece.block().x() - was.x(), piece.block().z() - was.z());
                if (d < nearest) {
                    nearest = d;
                    best = i;
                }
            }
            return best;
        }
    }
}
