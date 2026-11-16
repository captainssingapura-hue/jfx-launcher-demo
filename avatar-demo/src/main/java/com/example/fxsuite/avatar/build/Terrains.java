package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Contour;
import com.example.fxsuite.avatar.sim.Terrain;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.List;

/** Terrains drawn so far: as their maps' text ({@link Terrain#parse}), or worked out from what stands on them. */
public final class Terrains {

    private Terrains() {
    }

    /**
     * A valley 2 km across: hills all round rising to 40 m at the edge, its floor level at 0, a knoll
     * on it topping out at 18 m, and a level pad for a village beside it.
     */
    public static final String VALLEY = """
            terrain "valley" reach=1000m every=5m base=40m seed=7
            noise height=1m apart=50m octaves=3
            contour 30m 953,0 932,131 864,251 769,355 663,445 549,524 425,589 289,635 145,660 0,670 -148,672 -303,667 -464,643 -615,587 -734,493 -803,371 -824,240 -817,115 -807,0 -801,-113 -790,-230 -756,-349 -686,-460 -582,-555 -455,-631 -313,-688 -160,-727 0,-738 157,-715 299,-656 416,-576 516,-492 614,-412 721,-333 830,-242 916,-129
            contour 20m 813,0 794,112 732,213 647,299 555,373 459,438 355,492 241,529 121,549 0,558 -124,562 -255,562 -394,546 -525,501 -627,421 -681,315 -692,202 -679,96 -667,0 -663,-94 -659,-192 -634,-293 -578,-388 -492,-469 -385,-534 -265,-583 -136,-617 0,-626 133,-604 251,-551 346,-479 426,-406 507,-340 600,-277 699,-203 778,-110
            contour 10m 673,0 651,104 583,193 497,266 409,327 317,380 217,418 109,438 0,446 -112,452 -236,456 -368,440 -482,386 -550,294 -563,187 -544,87 -527,0 -526,-84 -526,-174 -501,-268 -439,-352 -349,-418 -243,-469 -125,-504 0,-514 122,-489 223,-431 299,-358 366,-293 447,-239 545,-181 633,-101
            contour 0m 523,0 504,80 444,147 372,199 303,242 234,280 159,307 80,320 0,326 -83,335 -179,345 -284,340 -376,301 -426,228 -425,141 -397,63 -377,0 -379,-60 -387,-128 -376,-201 -333,-267 -266,-318 -185,-358 -96,-386 0,-394 92,-372 166,-320 216,-258 260,-208 323,-172 407,-135 486,-77
            contour 5m 320,-120 300,-82 266,-56 232,-34 191,-25 149,-24 99,-22 54,-42 49,-85 60,-120 56,-153 67,-189 102,-214 146,-230 194,-229 229,-202 253,-176 293,-156
            contour 12m 255,-115 237,-91 213,-76 185,-72 152,-69 127,-88 135,-115 133,-139 153,-159 185,-168 212,-152 231,-136
            spot 18m at=190,-118
            contour 0m flat -240,60 -140,60 -140,160 -240,160
            """;

    public static Terrain valley() {
        return Terrain.parse(VALLEY);
    }

    // --- the meadow ---------------------------------------------------------------------------------------

    /** How far apart its banks' lines have a point; how far past the world's ends east and west the first runs, each after it so much further, so they nest. */
    private static final double ALONG = 10_000;
    private static final double PAST = 20_000;

    /**
     * A band along the river, the land within it lower than {@code height}: its line so far {@code north}
     * of the river and so far {@code south}, each widened and narrowed along its length; its lines joined
     * past the world's ends, the further the wider the band (its {@code outward}), so the valley runs out of the world east and west, low.
     */
    private record Band(double height, double north, double south, boolean flat, double bulge, double bulgeWide) {

        double northAt(final double x) {
            final double wave = 1 + 0.18 * Math.sin(x / 90_000 + 0.7) + 0.08 * Math.sin(x / 37_000 + 2.1);
            final double round = Math.max(0, 1 - (x / bulgeWide) * (x / bulgeWide));
            return north * wave + bulge * round;
        }

        double southAt(final double x) {
            return south * (1 + 0.2 * Math.sin(x / 70_000 + 2.6) + 0.07 * Math.sin(x / 29_000 + 0.4));
        }

        Contour contour(final int outward) {
            final double reach = Body.EDGE + PAST * (outward + 1);
            final List<Vec2> loop = new ArrayList<>();
            for (double x = -reach; x <= reach; x += ALONG) {
                loop.add(metre(x, Village.riverAt(x) + northAt(x)));
            }
            for (double x = reach; x >= -reach; x -= ALONG) {
                loop.add(metre(x, Village.riverAt(x) - southAt(x)));
            }
            return new Contour(height, loop, flat);
        }
    }

    /**
     * The river's valley, its bands from the water out: its floor, the meadows by the river and the
     * village on them, level; then up gently northward, through rolling ground to the hills, and more
     * steeply southward; at last up to the moors at the world's edge.
     */
    private static final List<Band> BANDS = List.of(
            new Band(0, 9_000, 6_000, true, 10_000, 25_000),
            new Band(300, 30_000, 15_000, false, 8_000, 40_000),
            new Band(1_000, 70_000, 30_000, false, 0, 1),
            new Band(2_500, 140_000, 60_000, false, 0, 1),
            new Band(5_000, 250_000, 120_000, false, 0, 1),
            new Band(8_000, 380_000, 260_000, false, 0, 1));
    /** The moors at the world's edge, past every band. */
    private static final double MOORS = 10_000;

    /**
     * Where a hill (or a hollow) on the northern slope stands: so far east, between band {@code inner}'s
     * line and the next one's, as far between them as {@code between} (0 at the inner, 1 at the outer).
     */
    private static Vec2 between(final int inner, final double x, final double between) {
        final double near = BANDS.get(inner).northAt(x);
        final double far = BANDS.get(inner + 1).northAt(x);
        return metre(x, Village.riverAt(x) + near + (far - near) * between);
    }

    /** The room between band {@code inner}'s line and the next one's, northward, at {@code x}. */
    private static double room(final int inner, final double x) {
        return BANDS.get(inner + 1).northAt(x) - BANDS.get(inner).northAt(x);
    }

    /** A loop round {@code at}, {@code r} out, a little out of round, to the metre. */
    private static Contour round(final double height, final Vec2 at, final double r, final int points, final double turn) {
        final List<Vec2> loop = new ArrayList<>();
        for (int k = 0; k < points; k++) {
            final double a = 2 * Math.PI * k / points;
            final double out = r * (1 + 0.12 * Math.sin(3 * a + turn) + 0.06 * Math.sin(5 * a + 2 * turn));
            loop.add(metre(at.x() + out * Math.cos(a), at.z() + out * Math.sin(a) * 0.8));
        }
        return new Contour(height, loop);
    }

    private static Vec2 metre(final double x, final double z) {
        return new Vec2(Math.round(x / 100) * 100.0, Math.round(z / 100) * 100.0);
    }

    /** Where the meadow's knoll tops out, its hill's beacon stands, and its hollow (a pond, in time) is deepest. */
    public static final Vec2 KNOLL = between(2, -90_000, 0.5);
    public static final Vec2 BEACON = between(3, 150_000, 0.5);
    public static final Vec2 HOLLOW = between(1, 45_000, 0.45);

    /**
     * The meadow round the village ({@link Village}), 5 km each way: the river's valley across it west
     * to east, its floor level at 0 where the river runs and the village stands, 80 to 190 m wide; the
     * land rising gently northward, 3 m 300 m out, 10 m at 700 m, 25 m at 1.4 km, past a knoll (34 m)
     * and a hollow for a pond, to a beacon hill (66 m) 2 km out; southward more steeply, to a ridge;
     * moors at 100 m round the edge, framing the sky.
     */
    public static Terrain meadow() {
        final List<Contour> loops = new ArrayList<>();
        for (int k = 0; k < BANDS.size(); k++) {
            loops.add(BANDS.get(k).contour(k));
        }
        // the knoll, between the 10 m and 25 m lines
        loops.add(round(3_000, KNOLL, Math.min(15_000, room(2, KNOLL.x()) * 0.3), 18, 0.4));
        // the beacon hill, between the 25 m and 50 m lines
        final double beacon = Math.min(40_000, room(3, BEACON.x()) * 0.32);
        loops.add(round(5_500, BEACON, beacon, 24, 1.7));
        loops.add(round(6_000, BEACON, beacon * 0.45, 16, 2.3));
        // the hollow, between the 3 m and 10 m lines, sinking to 1 m
        loops.add(round(400, HOLLOW, Math.min(6_000, room(1, HOLLOW.x()) * 0.25), 14, 0.9));
        return new Terrain("meadow", Body.EDGE, 1_000, MOORS, 11, new Terrain.Noise(60, 4_000, 3), loops,
                List.of(new Terrain.Spot(KNOLL, 3_400), new Terrain.Spot(BEACON, 6_600)));
    }
}
