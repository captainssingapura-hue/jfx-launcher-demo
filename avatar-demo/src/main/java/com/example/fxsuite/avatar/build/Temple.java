package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Assembly.Placed;
import com.example.fxsuite.avatar.sim.Curve;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The main hall of a Chinese temple, its skeleton composed of parts ({@link Assembly}), as a hall of
 * the Qing is laid out: its bays wider toward its middle, its roof carried out over deep eaves on
 * bracket sets, its eaves swept up at its corners.
 *
 * <ul>
 *   <li>From plans: its platform, 1.2 m high, reaching 2.4 m beyond its columns; its hall, five bays
 *       across (4.4, 5.6, 6.4, 5.6, 4.4 m: the middle one widest) and three deep (3.2, 6.4, 3.2 m), its
 *       columns 4.5 m high.</li>
 *   <li>Drawn in 3D: a bracket set on every column round its edge and one in the middle of every bay
 *       between, each arms stepping out three tiers and in as many, a crosswise arm at each tier, those
 *       at its corners turned to the diagonal; their outer tips carrying the eave purlin. On that, a hip
 *       roof: its eaves swept, each side one bow between its corners, those turned up and out; its
 *       rafters bowed down, steeper toward its ridge; its hips bowed down from the ridge's ends to the
 *       corners; ornaments curled up on the ridge's ends. Steps down from the platform before the
 *       middle bay.</li>
 * </ul>
 *
 * <p>In centimetres: across x, along y (its front +y), up from the ground.
 */
public final class Temple {

    private Temple() {
    }

    /** Its bays across, the middle one widest; and deep. */
    private static final double[] ACROSS = {440, 560, 640, 560, 440};
    private static final double[] DEEP = {320, 640, 320};
    private static final double HALF_WIDE = sum(ACROSS) / 2;
    private static final double HALF_DEEP = sum(DEEP) / 2;
    private static final double PLATFORM = 120;
    private static final double MARGIN = 240;
    private static final double COLUMN = 450;
    private static final double HEAD = PLATFORM + COLUMN;
    /** A bracket set: how high it stands, how far out (and in) its arms reach. */
    private static final double BRACKET = 90;
    private static final double REACH = 90;
    private static final double PURLIN = HEAD + BRACKET;
    /** Its eaves: how far out beyond its columns, how high at their middle; its corners turned up and out. */
    private static final double OVERHANG = 270;
    private static final double EAVE = 600;
    private static final double UPTURN = 70;
    private static final double FLARE = 50;
    private static final double SWEEP = 3;
    /** Its ridge: how high, how far each way from the middle; how far its rafters and hips bow down. */
    private static final double RIDGE = 1375;
    private static final double RIDGE_HALF = 900;
    private static final double SAG = 60;
    private static final double HIP_SAG = 70;

    private static double sum(final double[] bays) {
        return java.util.Arrays.stream(bays).sum();
    }

    /** The plans its stacked parts are made from: its platform, its hall. */
    public static Map<String, Plan> plans() {
        final Map<String, Plan> plans = new LinkedHashMap<>();
        plans.put("platform", Plan.of("platform", new Grid("platform", -HALF_WIDE - MARGIN, -HALF_DEEP - MARGIN,
                with(MARGIN, ACROSS), with(MARGIN, DEEP), Map.of())));
        plans.put("hall", Plan.of("hall", new Grid("hall", -HALF_WIDE, -HALF_DEEP, ACROSS, DEEP, Map.of())));
        return plans;
    }

    /** {@code bays}, a bay {@code margin} wide added at each end. */
    private static double[] with(final double margin, final double[] bays) {
        final double[] all = new double[bays.length + 2];
        all[0] = margin;
        System.arraycopy(bays, 0, all, 1, bays.length);
        all[all.length - 1] = margin;
        return all;
    }

    /** Its stacked parts: its platform, its hall. */
    public static List<Tower> stacks() {
        return List.of(Tower.of("platform", plans().get("platform"), 1, PLATFORM),
                Tower.of("hall", plans().get("hall"), 1, COLUMN));
    }

    /** Its parts, placed. */
    public static List<Placed> parts() {
        final List<Tower> stacks = stacks();
        return List.of(
                new Placed("platform", stacks.get(0).skeleton(true), Vec3.ZERO, 0, List.of()),
                new Placed("hall", stacks.get(1).skeleton(true), new Vec3(0, 0, PLATFORM), 0, List.of()),
                brackets(), roof(), steps());
    }

    /** It all, put together. */
    public static Assembly.Result assembled() {
        return Assembly.of("temple hall", parts());
    }

    public static Form skeleton() {
        return assembled().form();
    }

    /** The lines of its columns across, west to east; and along, back to front. */
    private static double[] lines(final double[] bays, final double half) {
        final double[] at = new double[bays.length + 1];
        at[0] = -half;
        for (int k = 0; k < bays.length; k++) {
            at[k + 1] = at[k] + bays[k];
        }
        return at;
    }

    /**
     * Its bracket sets: on every column round its edge (turned to the diagonal at its corners, reaching
     * as far again, so its tip meets the purlin's corner), and in the middle of every bay between.
     */
    private static Placed brackets() {
        final Form.Builder form = new Form.Builder("brackets");
        final List<String> joints = new ArrayList<>();
        final double[] xs = lines(ACROSS, HALF_WIDE);
        final double[] ys = lines(DEEP, HALF_DEEP);
        // round its edge, anticlockwise from its back left corner: each column, then the middle of the bay after it
        final List<double[]> round = new ArrayList<>();
        for (int i = 0; i < xs.length - 1; i++) {
            round.add(new double[] {xs[i], -HALF_DEEP});
        }
        for (int j = 0; j < ys.length - 1; j++) {
            round.add(new double[] {HALF_WIDE, ys[j]});
        }
        for (int i = xs.length - 1; i > 0; i--) {
            round.add(new double[] {xs[i], HALF_DEEP});
        }
        for (int j = ys.length - 1; j > 0; j--) {
            round.add(new double[] {-HALF_WIDE, ys[j]});
        }
        for (int k = 0; k < round.size(); k++) {
            final double[] at = round.get(k);
            final double[] next = round.get((k + 1) % round.size());
            final boolean corner = Math.abs(at[0]) == HALF_WIDE && Math.abs(at[1]) == HALF_DEEP;
            final Vec3 out = corner ? new Vec3(Math.signum(at[0]), Math.signum(at[1]), 0).normalize() : outward(at);
            set(form, "column set " + k, new Vec3(at[0], at[1], HEAD), out, corner ? REACH * Math.sqrt(2) : REACH, joints);
            // and one in the middle of the bay on to the next column
            final double[] middle = {(at[0] + next[0]) / 2, (at[1] + next[1]) / 2};
            set(form, "bay set " + k, new Vec3(middle[0], middle[1], HEAD), outward(middle), REACH, joints);
        }
        return new Placed("brackets", form.build(), Vec3.ZERO, 0, joints);
    }

    /** Out from the side of the hall {@code at} is on. */
    private static Vec3 outward(final double[] at) {
        return Math.abs(Math.abs(at[1]) - HALF_DEEP) < 1e-6 ? new Vec3(0, Math.signum(at[1]), 0)
                : new Vec3(Math.signum(at[0]), 0, 0);
    }

    /**
     * A bracket set on {@code base}: its arms stepping out {@code out} in three tiers to {@code reach}
     * as they rise to its height, and in as far; a crosswise arm, longer the higher, through each tier
     * of the outer climb. Its base and its outer tip are where it joins: the column (or the beam) under
     * it, the purlin on it.
     */
    private static void set(final Form.Builder form, final String name, final Vec3 base, final Vec3 out,
                            final double reach, final List<String> joints) {
        final Vec3 up = new Vec3(0, 0, 1);
        final Vec3 across = new Vec3(-out.y(), out.x(), 0);
        final int foot = vertex(form, name + ", foot", base);
        joints.add(name + ", foot");
        for (final int side : new int[] {1, -1}) {
            int last = foot;
            for (int k = 1; k <= 3; k++) {
                final String tier = name + (side > 0 ? ", out " : ", in ") + k;
                // out along the tier below, then up to this one
                final int along = vertex(form, tier + ", arm", base.add(out.multiply(side * reach * k / 3))
                        .add(up.multiply(BRACKET * (k - 1) / 3)));
                final int step = vertex(form, tier, base.add(out.multiply(side * reach * k / 3))
                        .add(up.multiply(BRACKET * k / 3)));
                form.edge(last, along).edge(along, step);
                if (side > 0) {
                    final double arm = 30 + 10 * k;
                    final Vec3 at = base.add(out.multiply(reach * k / 3)).add(up.multiply(BRACKET * k / 3));
                    form.edge(vertex(form, tier + ", left", at.add(across.multiply(-arm))), step);
                    form.edge(step, vertex(form, tier + ", right", at.add(across.multiply(arm))));
                }
                last = step;
            }
            if (side > 0) {
                joints.add(name + ", out 3");
            }
        }
    }

    /**
     * Its hip roof: the eave purlin round the bracket sets' tips; from its corners, corner rafters out to
     * the eaves' corners, turned up and out; the eaves, each side one bow between its corners, cut where
     * its rafters start; its ridge; its rafters bowed down to the ridge from the long sides' eaves, and
     * from the middles of the short sides' to the ridge's ends; its hips bowed down from the ridge's ends
     * to the eaves' corners; ornaments curled up on the ridge's ends.
     */
    private static Placed roof() {
        final Form.Builder form = new Form.Builder("roof");
        final List<String> joints = new ArrayList<>();
        // the eave purlin, on the bracket sets' tips, and its corners, where the corner sets' tips meet it
        final double px = HALF_WIDE + REACH;
        final double py = HALF_DEEP + REACH;
        final int[] purlin = new int[4];
        final double[][] signs = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        final int[] eaves = new int[4];
        final Vec3[] corner = new Vec3[4];
        for (int c = 0; c < 4; c++) {
            final String name = "purlin corner " + c;
            purlin[c] = form.vertex(name, signs[c][0] * px, signs[c][1] * py, PURLIN);
            joints.add(name);
            corner[c] = new Vec3(signs[c][0] * (HALF_WIDE + OVERHANG + FLARE), signs[c][1] * (HALF_DEEP + OVERHANG + FLARE),
                    EAVE + UPTURN);
            eaves[c] = vertex(form, "eave corner " + c, corner[c]);
            form.edge(purlin[c], eaves[c]);
        }
        form.loop(purlin);
        // the ridge, cut where its rafters meet it
        final double[] xs = lines(ACROSS, HALF_WIDE);
        final List<Double> under = new ArrayList<>();
        for (final double x : xs) {
            if (Math.abs(x) < RIDGE_HALF) {
                under.add(x);
            }
        }
        final List<Double> ridgeAt = new ArrayList<>(List.of(-RIDGE_HALF));
        ridgeAt.addAll(under);
        ridgeAt.add(RIDGE_HALF);
        final List<Integer> ridge = new ArrayList<>();
        for (final double x : ridgeAt) {
            ridge.add(form.vertex("ridge at " + Math.round(x), x, 0, RIDGE));
        }
        for (int k = 0; k + 1 < ridge.size(); k++) {
            form.edge(ridge.get(k), ridge.get(k + 1));
        }
        final int west = ridge.getFirst();
        final int east = ridge.getLast();
        // each side's eave: one bow between its corners, turned up and out, cut where its rafters start
        for (int side = 0; side < 4; side++) {
            final Vec3 from = corner[side];
            final Vec3 to = corner[(side + 1) % 4];
            final boolean longSide = side % 2 == 0;
            final Vec3 inward = longSide ? new Vec3(0, -Math.signum(from.y()), 0) : new Vec3(-Math.signum(from.x()), 0, 0);
            final Vec3 bow = inward.multiply(FLARE).add(new Vec3(0, 0, -UPTURN));
            final List<Double> cuts = new ArrayList<>(List.of(0.0));
            if (longSide) {
                for (final double x : under) {
                    cuts.add((x - from.x()) / (to.x() - from.x()));
                }
            } else {
                cuts.add(0.5);
            }
            cuts.add(1.0);
            cuts.sort(Double::compare);
            int last = eaves[side];
            for (int k = 1; k < cuts.size(); k++) {
                final double t = cuts.get(k);
                final int here = k == cuts.size() - 1 ? eaves[(side + 1) % 4]
                        : vertex(form, "eave " + side + " at " + k, Curve.bowAt(bow, SWEEP, from, to, t));
                form.edge(last, here, Curve.bow(bow, SWEEP, from, to, cuts.get(k - 1), t));
                if (k < cuts.size() - 1) {
                    // a rafter up from it: to the ridge over it, on a long side; to the ridge's end, on a short one
                    final Vec3 foot = Curve.bowAt(bow, SWEEP, from, to, t);
                    final int top = longSide ? ridge.get(ridgeAt.indexOf(roundTo(foot.x(), ridgeAt)))
                            : (foot.x() < 0 ? west : east);
                    form.edge(here, top, Curve.bow(new Vec3(0, 0, -SAG), 2));
                }
                last = here;
            }
        }
        // its hips, from the ridge's ends down to the eaves' corners, bowed down
        for (int c = 0; c < 4; c++) {
            form.edge(signs[c][0] < 0 ? west : east, eaves[c], Curve.bow(new Vec3(0, 0, -HIP_SAG), 2));
        }
        // the ornaments on the ridge's ends: up, then curled in over the ridge
        for (final int end : new int[] {west, east}) {
            final double x = end == west ? -RIDGE_HALF : RIDGE_HALF;
            final int top = form.vertex("ornament top at " + Math.round(x), x, 0, RIDGE + 110);
            final int curl = form.vertex("ornament curl at " + Math.round(x), x - Math.signum(x) * 60, 0, RIDGE + 80);
            form.edge(end, top).edge(top, curl, Curve.arc(new Vec3(0, 0, 25)));
        }
        return new Placed("roof", form.build(), Vec3.ZERO, 0, joints);
    }

    /** Which of {@code at} {@code x} is (as near as a rounding allows). */
    private static double roundTo(final double x, final List<Double> at) {
        return at.stream().min((a, b) -> Double.compare(Math.abs(a - x), Math.abs(b - x))).orElseThrow();
    }

    /** The steps down from the platform before the middle bay: their sides, and the nosing of each step across. */
    private static Placed steps() {
        final Form.Builder form = new Form.Builder("steps");
        final List<String> joints = new ArrayList<>();
        final double edge = HALF_DEEP + MARGIN;
        final double half = ACROSS[ACROSS.length / 2] / 2;
        final int count = 4;
        final double rise = PLATFORM / count;
        final double going = 35;
        final int[][] nosing = new int[count + 1][2];
        for (int s = 0; s < 2; s++) {
            final double x = s == 0 ? -half : half;
            int last = form.vertex("top " + s, x, edge, PLATFORM);
            joints.add("top " + s);
            nosing[0][s] = last;
            for (int k = 0; k < count; k++) {
                // down the riser, then out along the tread
                final int riser = form.vertex("riser " + k + ", " + s, x, edge + going * k, PLATFORM - rise * (k + 1));
                final int tread = form.vertex("tread " + k + ", " + s, x, edge + going * (k + 1), PLATFORM - rise * (k + 1));
                form.edge(last, riser).edge(riser, tread);
                nosing[k + 1][s] = tread;
                last = tread;
            }
        }
        for (final int[] across : nosing) {
            form.edge(across[0], across[1]);
        }
        return new Placed("steps", form.build(), Vec3.ZERO, 0, joints);
    }

    private static int vertex(final Form.Builder form, final String name, final Vec3 at) {
        return form.vertex(name, at.x(), at.y(), at.z());
    }
}
