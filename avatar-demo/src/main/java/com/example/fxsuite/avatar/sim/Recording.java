package com.example.fxsuite.avatar.sim;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.OptionalLong;
import java.util.TreeMap;

/**
 * A run kept as its inputs: where it started, and each step's {@link Controls}. Stepped again
 * from the same start by the same rules, they make the same world, step for step, so nothing
 * that follows from them (where the avatar went, what broke, what was heard) is kept at all.
 *
 * <p>It is plain text, a line at a time:
 * <pre>
 * avatar-inputs 1                 the format
 * rules 1                         the rules the world was stepped by ({@link Play#RULES})
 * start pack=false demo=AIRSHIP   where it started ({@link Play#start}): with the pack on or not, in
 *                                 which demo (the playground if it does not say)
 * 61 1.0 0.0 0.0 0.0 true false 0.0 false JUMP DRAW:RIFLES
 *                                 from step 61: ahead, sideways, turn, lift, run, trigger, aim,
 *                                 the other button (which a line kept before there was one leaves
 *                                 out), and what was done once, in that step only
 * check 120 8c1f03d2a5b6e7f9      the world after step 120, as its {@link Play#checksum}
 * end 678                         the last step
 * </pre>
 * A step's line is written only when its inputs differ from the step before's, or something
 * was done once in it, so a key held for a minute is one line. The numbers are written to the
 * bit. A checksum every {@link #CHECK_EVERY} steps lets a replay say whether it is still the
 * same run, and where it first went astray if not: under other rules, say.
 */
public final class Recording {

    /** The format this reads and writes. */
    public static final int FORMAT = 1;
    /** How often the world's checksum is kept: every second. */
    public static final long CHECK_EVERY = 120;
    private static final String MAGIC = "avatar-inputs";

    private final int rules;
    private final boolean pack;
    private final Demo demo;
    private final NavigableMap<Long, Controls> lines;
    private final Map<Long, Long> checks;
    private final long length;

    private Recording(final int rules, final boolean pack, final Demo demo, final NavigableMap<Long, Controls> lines,
                      final Map<Long, Long> checks, final long length) {
        this.rules = rules;
        this.pack = pack;
        this.demo = demo;
        this.lines = lines;
        this.checks = checks;
        this.length = length;
    }

    /** Reads a recording; refuses another format, or a line it cannot read. */
    public static Recording read(final Reader source) throws IOException {
        final BufferedReader in = new BufferedReader(source);
        Integer format = null;
        int rules = -1;
        Boolean pack = null;
        Demo demo = Demo.PLAYGROUND;
        final NavigableMap<Long, Controls> lines = new TreeMap<>();
        final Map<Long, Long> checks = new TreeMap<>();
        long end = 0;
        long last = 0;
        int number = 0;
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            number++;
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            final String[] words = line.split("\\s+");
            try {
                if (format == null) {
                    if (!words[0].equals(MAGIC) || words.length != 2) {
                        throw new IllegalArgumentException("not a recording of the avatar's inputs");
                    }
                    format = Integer.parseInt(words[1]);
                    if (format != FORMAT) {
                        throw new IllegalArgumentException("format " + format + ", but this reads format " + FORMAT);
                    }
                    continue;
                }
                switch (words[0]) {
                    case "rules" -> rules = Integer.parseInt(words[1]);
                    case "start" -> {
                        pack = Boolean.parseBoolean(words[1].substring("pack=".length()));
                        if (words.length > 2) {
                            demo = Demo.valueOf(words[2].substring("demo=".length()));
                        }
                    }
                    case "check" -> checks.put(Long.parseLong(words[1]), Long.parseUnsignedLong(words[2], 16));
                    case "end" -> end = Long.parseLong(words[1]);
                    default -> {
                        final long step = Long.parseLong(words[0]);
                        if (step <= last) {
                            throw new IllegalArgumentException("step " + step + " comes after step " + last);
                        }
                        last = step;
                        lines.put(step, controls(words));
                    }
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("line " + number + ": " + e.getMessage() + ": " + line, e);
            }
        }
        if (format == null || pack == null || rules < 0) {
            throw new IllegalArgumentException("a recording starts with its format, its rules and its start");
        }
        final long checked = checks.keySet().stream().mapToLong(Long::longValue).max().orElse(0);
        return new Recording(rules, pack, demo, lines, checks, Math.max(end, Math.max(last, checked)));
    }

    /** The rules it was stepped by; another than {@link Play#RULES} may not replay the same. */
    public int rules() {
        return rules;
    }

    /** Whether it started with the pack on. */
    public boolean pack() {
        return pack;
    }

    /** Which demo it was made in. */
    public Demo demo() {
        return demo;
    }

    /** The world it started from. */
    public Play start() {
        return Play.start(demo, pack);
    }

    /** The last step it covers. */
    public long length() {
        return length;
    }

    /** How many checksums it kept. */
    public int checks() {
        return checks.size();
    }

    /** Step {@code step}'s inputs: those of the latest line, with what was done once only in its own step. */
    public Controls controls(final long step) {
        final Map.Entry<Long, Controls> line = lines.floorEntry(step);
        if (line == null) {
            return Controls.NONE;
        }
        return line.getKey() == step ? line.getValue() : line.getValue().with(List.of());
    }

    /** The checksum kept for the world after step {@code step}, if one was. */
    public OptionalLong check(final long step) {
        final Long sum = checks.get(step);
        return sum == null ? OptionalLong.empty() : OptionalLong.of(sum);
    }

    /** Whether {@code after}, the world after step {@code step}, is as it was: true where no checksum was kept. */
    public boolean matches(final long step, final Play after) {
        final OptionalLong sum = check(step);
        return sum.isEmpty() || sum.getAsLong() == after.checksum();
    }

    /** What a replay came to: the world at the end, what happened, and the first step found astray (0 for none). */
    public record Replayed(Play play, List<Event> events, long astray) {

        public Replayed {
            events = List.copyOf(events);
        }
    }

    /** Steps it all again from the start, checking as it goes. */
    public Replayed replay() {
        Play play = start();
        final List<Event> events = new ArrayList<>();
        long astray = 0;
        for (long step = 1; step <= length; step++) {
            final Play.Stepped stepped = play.step(controls(step), Clock.STEP);
            play = stepped.play();
            events.addAll(stepped.events());
            if (astray == 0 && !matches(step, play)) {
                astray = step;
            }
        }
        return new Replayed(play, events, astray);
    }

    /**
     * Writes a recording as the run goes: a step at a time, in order, from step 1. Closing it
     * writes the end; a recording cut short still reads, up to its last line.
     */
    public static final class Recorder implements Closeable {
        private final Writer out;
        private Controls held;
        private long last;

        /** Starts a recording, in {@code out}, of a run from {@link Play#start} in the playground with the pack on or not. */
        public Recorder(final Writer out, final boolean pack) {
            this(out, pack, Demo.PLAYGROUND);
        }

        /** Starts a recording, in {@code out}, of a run from {@link Play#start} in {@code demo} with the pack on or not. */
        public Recorder(final Writer out, final boolean pack, final Demo demo) {
            this.out = out;
            write(MAGIC + " " + FORMAT);
            write("rules " + Play.RULES);
            write("start pack=" + pack + " demo=" + demo.name());
        }

        /** Keeps step {@code step}'s inputs, and the world after it, {@code after}, when it is time for a check. */
        public void step(final long step, final Controls controls, final Play after) {
            if (step != last + 1) {
                throw new IllegalArgumentException("step " + step + " after step " + last);
            }
            last = step;
            final Controls steady = controls.with(List.of());
            if (!steady.equals(held) || !controls.actions().isEmpty()) {
                write(step + " " + line(controls));
                held = steady;
            }
            if (step % CHECK_EVERY == 0) {
                write("check " + step + " " + Long.toHexString(after.checksum()));
            }
        }

        /** Writes what is not yet written, so far. */
        public void flush() {
            try {
                out.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void close() {
            write("end " + last);
            try {
                out.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void write(final String line) {
            try {
                out.write(line);
                out.write('\n');
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static String line(final Controls c) {
        final StringBuilder line = new StringBuilder();
        // Double.toString reads back to the same bits
        line.append(c.forward()).append(' ').append(c.sideways()).append(' ').append(c.turn()).append(' ')
                .append(c.lift()).append(' ').append(c.run()).append(' ').append(c.trigger()).append(' ')
                .append(c.aim()).append(' ').append(c.bomb());
        for (final Action action : c.actions()) {
            line.append(' ').append(name(action));
        }
        return line.toString();
    }

    private static Controls controls(final String[] words) {
        if (words.length < 8) {
            throw new IllegalArgumentException("a step's line has its seven inputs");
        }
        // the other button comes after the seven; a recording made before there was one has none
        final boolean other = words.length > 8 && (words[8].equals("true") || words[8].equals("false"));
        final List<Action> actions = new ArrayList<>();
        for (int i = other ? 9 : 8; i < words.length; i++) {
            actions.add(action(words[i]));
        }
        return new Controls(Double.parseDouble(words[1]), Double.parseDouble(words[2]), Double.parseDouble(words[3]),
                Double.parseDouble(words[4]), bool(words[5]), bool(words[6]), Double.parseDouble(words[7]), actions,
                other && bool(words[8]));
    }

    private static boolean bool(final String word) {
        return switch (word) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("true or false, not " + word);
        };
    }

    static String name(final Action action) {
        return switch (action) {
            case Action.Jump j -> "JUMP";
            case Action.Fly f -> "FLY";
            case Action.Pack p -> "PACK";
            case Action.Take t -> "TAKE";
            case Action.Gun g -> "GUN";
            case Action.Draw d -> "DRAW:" + d.weapon().name();
        };
    }

    static Action action(final String name) {
        return switch (name) {
            case "JUMP" -> Action.JUMP;
            case "FLY" -> Action.FLY;
            case "PACK" -> Action.PACK;
            case "TAKE" -> Action.TAKE;
            case "GUN" -> Action.GUN;
            default -> {
                if (!name.startsWith("DRAW:")) {
                    throw new IllegalArgumentException("no such action: " + name);
                }
                yield new Action.Draw(Weapon.valueOf(name.substring("DRAW:".length())));
            }
        };
    }
}
