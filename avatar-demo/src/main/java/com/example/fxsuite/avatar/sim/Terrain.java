package com.example.fxsuite.avatar.sim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lie of the land, as a topographical map: contour loops, each at its height, nesting one inside
 * another, with spot heights marked where the ground tops out (or bottoms out) between them, and a fine
 * roughness laid over all of it. What it is drawn as and walked on is worked out from it: its
 * {@link Survey}, heights on a grid. Pure data, the same land from the same map every time.
 *
 * <p>It is written as text, in metres:
 * <pre>
 * terrain "meadow" reach=5000m every=10m base=0m seed=7
 * noise height=1.5m apart=60m octaves=3
 * contour 5m -1200,300 -800,620 -300,700 ...
 * contour 20m flat -100,-100 100,-100 100,100 -100,100
 * spot 34m at=900,-200
 * </pre>
 * {@code reach=5000m} for a square round the middle; or, for any rectangle, {@code x=-15000..5000m z=-5000..5000m}.
 *
 * @param name     what it is called
 * @param x0       its west edge, in centimetres: the world's edge
 * @param z0       its south edge
 * @param x1       its east edge
 * @param z1       its north edge
 * @param every    how far apart its survey's heights are, in centimetres
 * @param base     the ground's height outside every loop, met at the world's edge
 * @param seed     what its roughness is worked from
 * @param noise    its roughness
 * @param contours its loops, in no particular order
 * @param spots    its spot heights
 */
public record Terrain(String name, double x0, double z0, double x1, double z1, double every, double base, long seed,
                      Noise noise, List<Contour> contours, List<Spot> spots) {

    /** The most heights along each side of its survey. */
    public static final int MOST = 4001;
    /** Steeper than this, rise over run (60°), and it is a cliff: reported, if not wrong. */
    public static final double CLIFF = Math.tan(Math.toRadians(60));

    /** A spot height: the ground's {@code height} at a point. */
    public record Spot(Vec2 at, double height) {
    }

    /**
     * A fine roughness over the ground, none on level pads: up and down by about {@code height}, its
     * bumps about {@code apart}, finer bumps on bumps for each of its {@code octaves}.
     */
    public record Noise(double height, double apart, int octaves) {

        public static final Noise NONE = new Noise(0, 0, 0);

        public Noise {
            if (height < 0 || height > 0 && (!(apart > 0) || octaves < 1)) {
                throw new IllegalArgumentException("roughness has its bumps some way apart, at least one size of them");
            }
        }

        public boolean any() {
            return height > 0;
        }
    }

    public Terrain {
        contours = List.copyOf(contours);
        spots = List.copyOf(spots);
        if (name == null || noise == null) {
            throw new IllegalArgumentException("a terrain has a name and a roughness, if none");
        }
        if (!(x1 > x0) || !(z1 > z0) || !(every > 0)) {
            throw new IllegalArgumentException("a terrain goes some way, its heights some way apart");
        }
        for (final double steps : new double[] {(x1 - x0) / every, (z1 - z0) / every}) {
            if (Math.abs(steps - Math.round(steps)) > 1e-6 || steps + 1 > MOST) {
                throw new IllegalArgumentException("its heights go evenly across it, at most " + MOST + " of them each way");
            }
        }
    }

    /** A square, {@code reach} from the middle each way. */
    public Terrain(final String name, final double reach, final double every, final double base, final long seed,
                   final Noise noise, final List<Contour> contours, final List<Spot> spots) {
        this(name, -reach, -reach, reach, reach, every, base, seed, noise, contours, spots);
    }

    /** Level ground everywhere, at 0, a square {@code reach} from the middle each way. */
    public static Terrain flat(final String name, final double reach, final double every) {
        return new Terrain(name, reach, every, 0, 0, Noise.NONE, List.of(), List.of());
    }

    /** How many heights across its survey, west to east; and along it, south to north. */
    public int across() {
        return (int) Math.round((x1 - x0) / every) + 1;
    }

    public int along() {
        return (int) Math.round((z1 - z0) / every) + 1;
    }

    /** The same, over the same ground, its loops {@code loops} and its spot heights {@code marked}. */
    public Terrain with(final List<Contour> loops, final List<Spot> marked) {
        return new Terrain(name, x0, z0, x1, z1, every, base, seed, noise, loops, marked);
    }

    /** Whether it goes over the same ground as {@code other}. */
    public boolean sameGround(final Terrain other) {
        return x0 == other.x0 && z0 == other.z0 && x1 == other.x1 && z1 == other.z1;
    }

    /** Whether it is a square round the middle: written {@code reach=}. */
    private boolean square() {
        return x0 == z0 && x1 == z1 && x0 == -x1;
    }

    /** Works out its heights. */
    public Survey survey() {
        return new Survey(this);
    }

    /** For each loop, the loop it lies directly inside; -1 for those inside none. */
    public int[] parents() {
        final int n = contours.size();
        final int[] parents = new int[n];
        for (int i = 0; i < n; i++) {
            parents[i] = -1;
            final Contour one = contours.get(i);
            final Vec2 on = one.loop().getFirst();
            double least = Double.POSITIVE_INFINITY;
            for (int j = 0; j < n; j++) {
                final Contour other = contours.get(j);
                if (j != i && other.area() > one.area() && other.area() < least && other.inside(on.x(), on.z())) {
                    least = other.area();
                    parents[i] = j;
                }
            }
        }
        return parents;
    }

    /** For each loop, how many loops it lies inside. */
    public int[] depths() {
        final int[] parents = parents();
        final int[] depths = new int[parents.length];
        for (int i = 0; i < parents.length; i++) {
            for (int p = parents[i]; p >= 0; p = parents[p]) {
                depths[i]++;
            }
        }
        return depths;
    }

    /** The innermost loop ({@code x}, {@code z}) is inside; -1 if none. */
    public int within(final double x, final double z) {
        final int[] depths = depths();
        int innermost = -1;
        for (int i = 0; i < contours.size(); i++) {
            if (contours.get(i).inside(x, z) && (innermost < 0 || depths[i] > depths[innermost])) {
                innermost = i;
            }
        }
        return innermost;
    }

    /** The height of the loop round loop {@code i}, or of the ground outside every loop. */
    double heightRound(final int i) {
        final int parent = parents()[i];
        return parent < 0 ? base : contours.get(parent).height();
    }

    /** What is wrong with it, if anything: what it cannot be made into land from, or should be looked at. */
    public List<String> problems() {
        final List<String> problems = new ArrayList<>();
        final int n = contours.size();
        final int[] parents = parents();
        for (int i = 0; i < n; i++) {
            final Contour one = contours.get(i);
            if (one.crossesItself()) {
                problems.add(named(i) + " crosses itself");
            }
            for (int j = i + 1; j < n; j++) {
                if (one.crosses(contours.get(j))) {
                    problems.add(named(i) + " and " + named(j) + " cross");
                }
            }
            if (one.loop().stream().noneMatch(this::onTheMap)) {
                problems.add(named(i) + " is off the map");
            }
            final int parent = parents[i];
            if (parent >= 0) {
                final Contour round = contours.get(parent);
                if (round.flat()) {
                    problems.add(named(parent) + " is flat, yet " + named(i) + " is inside it");
                } else if (!one.flat() && one.height() == round.height()) {
                    problems.add(named(i) + " is at the height of " + named(parent)
                            + " round it, so the ground goes neither up nor down: make it flat, or another height");
                }
                final double rise = Math.abs(one.height() - round.height());
                for (final Vec2 p : one.loop()) {
                    if (rise > CLIFF * round.distance(p.x(), p.z())) {
                        problems.add("between " + named(i) + " and " + named(parent) + " is a cliff");
                        break;
                    }
                }
            }
        }
        for (int s = 0; s < spots.size(); s++) {
            final Spot spot = spots.get(s);
            final String called = "the spot height at " + metres(spot.at().x()) + "," + metres(spot.at().z());
            if (!onTheMap(spot.at())) {
                problems.add(called + " is off the map");
            }
            final int in = within(spot.at().x(), spot.at().z());
            if (in >= 0 && contours.get(in).flat()) {
                problems.add(called + " is inside " + named(in) + ", which is flat");
            }
        }
        return problems;
    }

    private boolean onTheMap(final Vec2 p) {
        return p.x() >= x0 && p.x() <= x1 && p.z() >= z0 && p.z() <= z1;
    }

    private String named(final int i) {
        return "contour " + (i + 1) + " (" + metres(contours.get(i).height()) + " m)";
    }

    // --- as text ---------------------------------------------------------------------------------------

    private static final Pattern HEAD = Pattern.compile("terrain\\s+\"([^\"]*)\"\\s*(.*)");

    /** It written as text, in metres, that {@link #parse} reads back. */
    public String text() {
        final StringBuilder out = new StringBuilder();
        out.append("terrain \"").append(name).append(square() ? "\" reach=" + metres(x1) + "m"
                : "\" x=" + metres(x0) + ".." + metres(x1) + "m z=" + metres(z0) + ".." + metres(z1) + "m").append(" every=")
                .append(metres(every)).append("m base=").append(metres(base)).append("m seed=").append(seed).append('\n');
        if (noise.any()) {
            out.append("noise height=").append(metres(noise.height())).append("m apart=").append(metres(noise.apart()))
                    .append("m octaves=").append(noise.octaves()).append('\n');
        }
        for (final Contour contour : contours) {
            out.append("contour ").append(metres(contour.height())).append('m');
            if (contour.flat()) {
                out.append(" flat");
            }
            for (final Vec2 p : contour.loop()) {
                out.append(' ').append(metres(p.x())).append(',').append(metres(p.z()));
            }
            out.append('\n');
        }
        for (final Spot spot : spots) {
            out.append("spot ").append(metres(spot.height())).append("m at=").append(metres(spot.at().x())).append(',')
                    .append(metres(spot.at().z())).append('\n');
        }
        return out.toString();
    }

    /** Reads a terrain from its {@link #text}; lines may be blank, and {@code #} starts a remark. */
    public static Terrain parse(final String text) {
        String name = null;
        double reach = 0;
        double[] xs = null;
        double[] zs = null;
        double every = 0;
        double base = 0;
        long seed = 0;
        Noise noise = Noise.NONE;
        final List<Contour> contours = new ArrayList<>();
        final List<Spot> spots = new ArrayList<>();
        final String[] lines = text.split("\\R");
        for (int number = 1; number <= lines.length; number++) {
            final String line = lines[number - 1].replaceFirst("#.*", "").strip();
            if (line.isEmpty()) {
                continue;
            }
            try {
                final String[] words = line.split("\\s+");
                if (name == null) {
                    final Matcher head = HEAD.matcher(line);
                    if (!head.matches()) {
                        throw new IllegalArgumentException("it starts terrain \"its name\"");
                    }
                    name = head.group(1);
                    for (final String word : head.group(2).isBlank() ? new String[0] : head.group(2).split("\\s+")) {
                        final String value = value(word);
                        switch (key(word)) {
                            case "reach" -> reach = centimetres(value);
                            case "x" -> xs = span(value);
                            case "z" -> zs = span(value);
                            case "every" -> every = centimetres(value);
                            case "base" -> base = centimetres(value);
                            case "seed" -> seed = Long.parseLong(value);
                            default -> throw new IllegalArgumentException("a terrain has no " + key(word));
                        }
                    }
                    continue;
                }
                switch (words[0]) {
                    case "noise" -> {
                        double height = 0;
                        double apart = 0;
                        int octaves = 1;
                        for (int w = 1; w < words.length; w++) {
                            final String value = value(words[w]);
                            switch (key(words[w])) {
                                case "height" -> height = centimetres(value);
                                case "apart" -> apart = centimetres(value);
                                case "octaves" -> octaves = Integer.parseInt(value);
                                default -> throw new IllegalArgumentException("noise has no " + key(words[w]));
                            }
                        }
                        noise = new Noise(height, apart, octaves);
                    }
                    case "contour" -> {
                        if (words.length < 2) {
                            throw new IllegalArgumentException("a contour has a height");
                        }
                        final double height = centimetres(words[1]);
                        boolean flat = false;
                        final List<Vec2> loop = new ArrayList<>();
                        for (int w = 2; w < words.length; w++) {
                            if (words[w].equals("flat")) {
                                flat = true;
                            } else {
                                loop.add(point(words[w]));
                            }
                        }
                        contours.add(new Contour(height, loop, flat));
                    }
                    case "spot" -> {
                        if (words.length != 3 || !key(words[2]).equals("at")) {
                            throw new IllegalArgumentException("a spot height is written spot 34m at=x,z");
                        }
                        spots.add(new Spot(point(value(words[2])), centimetres(words[1])));
                    }
                    default -> throw new IllegalArgumentException("no " + words[0] + " on a terrain");
                }
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException("line " + number + ": " + e.getMessage(), e);
            }
        }
        if (name == null) {
            throw new IllegalArgumentException("no terrain in it");
        }
        try {
            if ((xs == null) != (zs == null) || xs != null && reach != 0) {
                throw new IllegalArgumentException("a terrain is reach= round the middle, or x= and z= both");
            }
            return xs == null ? new Terrain(name, reach, every, base, seed, noise, contours, spots)
                    : new Terrain(name, xs[0], zs[0], xs[1], zs[1], every, base, seed, noise, contours, spots);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("line 1: " + e.getMessage(), e);
        }
    }

    private static String key(final String word) {
        final int at = word.indexOf('=');
        if (at < 0) {
            throw new IllegalArgumentException("expected name=value, not " + word);
        }
        return word.substring(0, at).toLowerCase(Locale.ROOT);
    }

    private static String value(final String word) {
        key(word);
        return word.substring(word.indexOf('=') + 1);
    }

    /** {@code "12.5m"} or {@code "12.5"} metres, in centimetres, exactly as written. */
    private static double centimetres(final String metres) {
        final String number = metres.endsWith("m") ? metres.substring(0, metres.length() - 1) : metres;
        try {
            return new BigDecimal(number).movePointRight(2).doubleValue();
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("not a measure in metres: " + metres, e);
        }
    }

    /** {@code "-15000..5000m"}: from and to, in centimetres. */
    private static double[] span(final String word) {
        final String[] ends = (word.endsWith("m") ? word.substring(0, word.length() - 1) : word).split("\\.\\.");
        if (ends.length != 2) {
            throw new IllegalArgumentException("a span is from..to in metres, not " + word);
        }
        return new double[] {centimetres(ends[0]), centimetres(ends[1])};
    }

    private static Vec2 point(final String word) {
        final String[] xz = word.split(",");
        if (xz.length != 2) {
            throw new IllegalArgumentException("a point is x,z in metres, not " + word);
        }
        return new Vec2(centimetres(xz[0]), centimetres(xz[1]));
    }

    /** Centimetres in metres, to a tenth of a millimetre, without trailing noughts. */
    static String metres(final double centimetres) {
        final BigDecimal m = BigDecimal.valueOf(centimetres).movePointLeft(2).setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return m.signum() == 0 ? "0" : m.toPlainString();
    }
}
