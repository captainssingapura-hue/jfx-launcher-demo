package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Assembly.Placed;
import com.example.fxsuite.avatar.sim.Crossing;
import com.example.fxsuite.avatar.sim.Curve;
import com.example.fxsuite.avatar.sim.Extrusion;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.Vec2;
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
 * <p>So it is as first drawn ({@link Size#STANDARD}); built to any size it can stand at
 * ({@link #of(Size)}): its bays across and deep, its middle bay, its columns and platform.
 *
 * <p>In centimetres: across x, along y (its front +y), up from the ground.
 */
public final class Temple {

    /**
     * How big it is built, in centimetres: {@code bays} bays across (an odd number, so one is in the
     * middle), the middle one {@code middle} wide and each further out narrower, as a Qing hall's are
     * (seven eighths as wide, then eleven sixteenths, then a sixteenth less again, to half); {@code deep}
     * bays deep, the outer two each half the middle bay (its front and back aisles), those between as
     * wide as it; its columns {@code column} high, on a platform {@code platform} high. The bracket sets
     * as they are; the eaves as far out; the roof as steep, its ridge as much shorter than the hall
     * is wide as a Qing roof's (lengthened, so its hips are steeper than its slopes).
     */
    public record Size(int bays, double middle, int deep, double column, double platform) {

        /** The hall as it was first drawn: five bays across, the middle 6.4 m, three deep, its columns 4.5 m on a 1.2 m platform. */
        public static final Size STANDARD = new Size(5, 640, 3, 450, 120);

        public Size {
            final List<String> wrong = new ArrayList<>();
            if (bays < 3 || bays > 11 || bays % 2 == 0) {
                wrong.add("an odd number of bays across, from 3 to 11, not " + bays);
            }
            if (middle < 400 || middle > 900) {
                wrong.add("its middle bay from 4 m to 9 m wide, not " + middle / 100);
            }
            if (deep < 3 || deep > 5) {
                wrong.add("from 3 to 5 bays deep, not " + deep);
            }
            if (deep > bays) {
                wrong.add("no deeper in bays than it is wide");
            }
            if (column < 300 || column > 800) {
                wrong.add("its columns from 3 m to 8 m high, not " + column / 100);
            }
            if (platform < 30 || platform > 300) {
                wrong.add("its platform from 0.3 m to 3 m high, not " + platform / 100);
            }
            if (!wrong.isEmpty()) {
                throw new IllegalArgumentException("a temple hall " + String.join("; ", wrong));
            }
        }

        /** Its bays across, west to east: the middle one widest. */
        public double[] across() {
            final double[] widths = new double[bays];
            for (int b = 0; b < bays; b++) {
                final int out = Math.abs(b - bays / 2);
                final double share = out == 0 ? 1 : out == 1 ? 0.875 : Math.max(0.5, 0.6875 - 0.0625 * (out - 2));
                widths[b] = middle * share;
            }
            return widths;
        }

        /** Its bays deep, back to front: an aisle each side, half the middle bay, and as wide as it between. */
        public double[] depths() {
            final double[] depths = new double[deep];
            java.util.Arrays.fill(depths, middle);
            depths[0] = middle / 2;
            depths[deep - 1] = middle / 2;
            return depths;
        }
    }

    /** A bracket set: how high it stands, how far out (and in) its arms reach. */
    private static final double BRACKET = 90;
    private static final double REACH = 90;
    /** Its eaves: how far out beyond its columns; its corners turned up and out. */
    private static final double OVERHANG = 270;
    private static final double UPTURN = 70;
    private static final double FLARE = 50;
    private static final double SWEEP = 3;
    /** How far its rafters and hips bow down. */
    private static final double SAG = 60;
    private static final double HIP_SAG = 70;
    /** Its steps: how far each goes out, and about how high each rises. */
    private static final double GOING = 35;
    private static final double STEP = 30;

    private final Size size;
    /** Its bays across, the middle one widest; and deep. */
    private final double[] bayWidths;
    private final double[] bayDepths;
    private final double halfWide;
    private final double halfDeep;
    private final double platformHeight;
    private final double marginWidth;
    private final double headHeight;
    private final double purlinHeight;
    /** Its eaves: how high at their middle. */
    private final double eaveHeight;
    /** Its ridge: how high, how far each way from the middle. */
    private final double ridgeHeight;
    private final double ridgeHalf;
    /** How many steps up to its platform. */
    private final int stepCount;

    private Temple(final Size size) {
        this.size = size;
        bayWidths = size.across();
        bayDepths = size.depths();
        halfWide = sum(bayWidths) / 2;
        halfDeep = sum(bayDepths) / 2;
        platformHeight = size.platform();
        // its platform reaching out beyond its columns three eighths of its middle bay
        marginWidth = size.middle() * 3 / 8;
        headHeight = platformHeight + size.column();
        purlinHeight = headHeight + BRACKET;
        eaveHeight = purlinHeight - 60;
        // as steep as the first: 7.75 m up from its eaves over 9.1 m in from them to the ridge
        ridgeHeight = eaveHeight + 775 * (halfDeep + OVERHANG) / 910;
        // lengthened, as a Qing roof's is: its ridge's ends drawn out beyond where its hips would meet at 45
        ridgeHalf = Math.max(halfWide - halfDeep * 21 / 32, halfWide / 4);
        stepCount = Math.max(2, (int) Math.round(platformHeight / STEP));
    }

    /** The hall built {@code size}. */
    public static Temple of(final Size size) {
        return new Temple(size);
    }

    /** The hall as it was first drawn ({@link Size#STANDARD}). */
    public static Temple standard() {
        return new Temple(Size.STANDARD);
    }

    /** How big it is built. */
    public Size size() {
        return size;
    }

    /** A rectangle stood up, {@code across} each way of {@code x}, {@code along} each way of {@code z}, from {@code low} to {@code high}. */
    private static Prism rect(final double x, final double across, final double z, final double along, final double low,
                              final double high) {
        return new Prism(List.of(new Vec2(x - across, z - along), new Vec2(x + across, z - along),
                new Vec2(x + across, z + along), new Vec2(x - across, z + along)), low, high);
    }

    private double sum(final double[] bays) {
        return java.util.Arrays.stream(bays).sum();
    }

    /**
     * The hall in the way as it is shaped, in its own frame (x across, z along, its front +z): its
     * platform and its hall stood up, the hall to its eave purlin; its roof a hip roof, a gable along
     * it crossed with one across it (its upturned corners and its rafters' sag left out); its steps.
     * Under its eaves, open: walked round on its platform, flown under.
     */
    public List<com.example.fxsuite.avatar.sim.Volume> shape() {
        final List<com.example.fxsuite.avatar.sim.Volume> shape = new ArrayList<>();
        final double pw = halfWide + marginWidth;
        final double pd = halfDeep + marginWidth;
        shape.add(rect(0, pw, 0, pd, 0, platformHeight));
        shape.add(rect(0, halfWide, 0, halfDeep, platformHeight, purlinHeight));
        final double wide = halfWide + OVERHANG + FLARE;
        final double deep = halfDeep + OVERHANG + FLARE;
        shape.add(new Crossing(
                new Extrusion(0, 0, 90, wide, List.of(new Vec2(-deep, eaveHeight), new Vec2(deep, eaveHeight),
                        new Vec2(0, ridgeHeight))),
                new Extrusion(0, 0, 0, deep, List.of(new Vec2(-wide, eaveHeight), new Vec2(wide, eaveHeight),
                        new Vec2(ridgeHalf, ridgeHeight), new Vec2(-ridgeHalf, ridgeHeight)))));
        // its steps down before its middle bay, each a block to step (or jump) down
        final double half = bayWidths[bayWidths.length / 2] / 2;
        final double rise = platformHeight / stepCount;
        for (int k = 0; k + 1 < stepCount; k++) {
            shape.add(rect(0, half, pd + GOING * (k + 0.5), GOING / 2, 0, platformHeight - rise * (k + 1)));
        }
        return shape;
    }

    /** The plans its stacked parts are made from: its platform, its hall. */
    public Map<String, Plan> plans() {
        final Map<String, Plan> plans = new LinkedHashMap<>();
        plans.put("platform", Plan.of("platform", new Grid("platform", -halfWide - marginWidth, -halfDeep - marginWidth,
                with(marginWidth, bayWidths), with(marginWidth, bayDepths), Map.of())));
        plans.put("hall", Plan.of("hall", new Grid("hall", -halfWide, -halfDeep, bayWidths, bayDepths, Map.of())));
        return plans;
    }

    /** {@code bays}, a bay {@code margin} wide added at each end. */
    private double[] with(final double margin, final double[] bays) {
        final double[] all = new double[bays.length + 2];
        all[0] = margin;
        System.arraycopy(bays, 0, all, 1, bays.length);
        all[all.length - 1] = margin;
        return all;
    }

    /** Its stacked parts: its platform, its hall. */
    public List<Tower> stacks() {
        return List.of(Tower.of("platform", plans().get("platform"), 1, platformHeight),
                Tower.of("hall", plans().get("hall"), 1, size.column()));
    }

    /** Its parts, placed. */
    public List<Placed> parts() {
        final List<Tower> stacks = stacks();
        return List.of(
                new Placed("platform", stacks.get(0).skeleton(true), Vec3.ZERO, 0, List.of()),
                new Placed("hall", stacks.get(1).skeleton(true), new Vec3(0, 0, platformHeight), 0, List.of()),
                brackets(), roof(), steps());
    }

    /** It all, put together. */
    public Assembly.Result assembled() {
        return Assembly.of("temple hall", parts());
    }

    public Form skeleton() {
        return assembled().form();
    }

    /** The lines of its columns across, west to east; and along, back to front. */
    private double[] lines(final double[] bays, final double half) {
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
    private Placed brackets() {
        final Form.Builder form = new Form.Builder("brackets");
        final List<String> joints = new ArrayList<>();
        final double[] xs = lines(bayWidths, halfWide);
        final double[] ys = lines(bayDepths, halfDeep);
        // round its edge, anticlockwise from its back left corner: each column, then the middle of the bay after it
        final List<double[]> round = new ArrayList<>();
        for (int i = 0; i < xs.length - 1; i++) {
            round.add(new double[] {xs[i], -halfDeep});
        }
        for (int j = 0; j < ys.length - 1; j++) {
            round.add(new double[] {halfWide, ys[j]});
        }
        for (int i = xs.length - 1; i > 0; i--) {
            round.add(new double[] {xs[i], halfDeep});
        }
        for (int j = ys.length - 1; j > 0; j--) {
            round.add(new double[] {-halfWide, ys[j]});
        }
        for (int k = 0; k < round.size(); k++) {
            final double[] at = round.get(k);
            final double[] next = round.get((k + 1) % round.size());
            final boolean corner = Math.abs(at[0]) == halfWide && Math.abs(at[1]) == halfDeep;
            final Vec3 out = corner ? new Vec3(Math.signum(at[0]), Math.signum(at[1]), 0).normalize() : outward(at);
            set(form, "column set " + k, new Vec3(at[0], at[1], headHeight), out, corner ? REACH * Math.sqrt(2) : REACH, joints);
            // and one in the middle of the bay on to the next column
            final double[] middle = {(at[0] + next[0]) / 2, (at[1] + next[1]) / 2};
            set(form, "bay set " + k, new Vec3(middle[0], middle[1], headHeight), outward(middle), REACH, joints);
        }
        return new Placed("brackets", form.build(), Vec3.ZERO, 0, joints);
    }

    /** Out from the side of the hall {@code at} is on. */
    private Vec3 outward(final double[] at) {
        return Math.abs(Math.abs(at[1]) - halfDeep) < 1e-6 ? new Vec3(0, Math.signum(at[1]), 0)
                : new Vec3(Math.signum(at[0]), 0, 0);
    }

    /**
     * A bracket set on {@code base}: its arms stepping out {@code out} in three tiers to {@code reach}
     * as they rise to its height, and in as far; a crosswise arm, longer the higher, through each tier
     * of the outer climb. Its base and its outer tip are where it joins: the column (or the beam) under
     * it, the purlin on it.
     */
    private void set(final Form.Builder form, final String name, final Vec3 base, final Vec3 out,
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
    private Placed roof() {
        final Form.Builder form = new Form.Builder("roof");
        final List<String> joints = new ArrayList<>();
        // the eave purlin, on the bracket sets' tips, and its corners, where the corner sets' tips meet it
        final double px = halfWide + REACH;
        final double py = halfDeep + REACH;
        final int[] purlin = new int[4];
        final double[][] signs = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        final int[] eaves = new int[4];
        final Vec3[] corner = new Vec3[4];
        for (int c = 0; c < 4; c++) {
            final String name = "purlin corner " + c;
            purlin[c] = form.vertex(name, signs[c][0] * px, signs[c][1] * py, purlinHeight);
            joints.add(name);
            corner[c] = new Vec3(signs[c][0] * (halfWide + OVERHANG + FLARE), signs[c][1] * (halfDeep + OVERHANG + FLARE),
                    eaveHeight + UPTURN);
            eaves[c] = vertex(form, "eave corner " + c, corner[c]);
            form.edge(purlin[c], eaves[c]);
        }
        form.loop(purlin);
        // the ridge, cut where its rafters meet it
        final double[] xs = lines(bayWidths, halfWide);
        final List<Double> under = new ArrayList<>();
        for (final double x : xs) {
            if (Math.abs(x) < ridgeHalf) {
                under.add(x);
            }
        }
        final List<Double> ridgeAt = new ArrayList<>(List.of(-ridgeHalf));
        ridgeAt.addAll(under);
        ridgeAt.add(ridgeHalf);
        final List<Integer> ridge = new ArrayList<>();
        for (final double x : ridgeAt) {
            ridge.add(form.vertex("ridge at " + Math.round(x), x, 0, ridgeHeight));
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
            final double x = end == west ? -ridgeHalf : ridgeHalf;
            final int top = form.vertex("ornament top at " + Math.round(x), x, 0, ridgeHeight + 110);
            final int curl = form.vertex("ornament curl at " + Math.round(x), x - Math.signum(x) * 60, 0, ridgeHeight + 80);
            form.edge(end, top).edge(top, curl, Curve.arc(new Vec3(0, 0, 25)));
        }
        return new Placed("roof", form.build(), Vec3.ZERO, 0, joints);
    }

    /** Which of {@code at} {@code x} is (as near as a rounding allows). */
    private double roundTo(final double x, final List<Double> at) {
        return at.stream().min((a, b) -> Double.compare(Math.abs(a - x), Math.abs(b - x))).orElseThrow();
    }

    /** The steps down from the platform before the middle bay: their sides, and the nosing of each step across. */
    private Placed steps() {
        final Form.Builder form = new Form.Builder("steps");
        final List<String> joints = new ArrayList<>();
        final double edge = halfDeep + marginWidth;
        final double half = bayWidths[bayWidths.length / 2] / 2;
        final int count = stepCount;
        final double rise = platformHeight / count;
        final double going = GOING;
        final int[][] nosing = new int[count + 1][2];
        for (int s = 0; s < 2; s++) {
            final double x = s == 0 ? -half : half;
            int last = form.vertex("top " + s, x, edge, platformHeight);
            joints.add("top " + s);
            nosing[0][s] = last;
            for (int k = 0; k < count; k++) {
                // down the riser, then out along the tread
                final int riser = form.vertex("riser " + k + ", " + s, x, edge + going * k, platformHeight - rise * (k + 1));
                final int tread = form.vertex("tread " + k + ", " + s, x, edge + going * (k + 1), platformHeight - rise * (k + 1));
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

    /** The eave from corner {@code from} to corner {@code to}: one bow, turned up and out at its ends, as points. */
    private List<Vec3> eave(final Vec3 from, final Vec3 to) {
        final boolean longSide = Math.abs(from.y() - to.y()) < 1e-6;
        final Vec3 inward = longSide ? new Vec3(0, -Math.signum(from.y()), 0) : new Vec3(-Math.signum(from.x()), 0, 0);
        final Vec3 bow = inward.multiply(FLARE).add(new Vec3(0, 0, -UPTURN));
        final List<Vec3> points = new ArrayList<>();
        for (int k = 0; k <= 40; k++) {
            points.add(Curve.bowAt(bow, SWEEP, from, to, k / 40.0));
        }
        return points;
    }

    /** A hip, from the eave's corner {@code corner} up to the ridge's end {@code end}, bowed down, as points. */
    private List<Vec3> hip(final Vec3 corner, final Vec3 end) {
        final Curve bow = Curve.bow(new Vec3(0, 0, -HIP_SAG), 2);
        final List<Vec3> points = new ArrayList<>();
        for (int k = 0; k <= 24; k++) {
            points.add(bow.at(corner, end, k / 24.0));
        }
        return points;
    }

    private Sheet rect(final String name, final String kind, final Vec3 a, final Vec3 b, final Vec3 c,
                              final Vec3 d, final Vec3 out) {
        return Sheet.flat(name, kind, List.of(a, b, c, d), List.of(), out);
    }

    /**
     * Its faces: its platform's sides and top, and its steps; its back and side walls; the doors in its
     * front's bays; its roof's two long slopes (each filled in between its eave, its two hips and its
     * ridge) and its two hip ends (each between its eave and its two hips, up to the ridge's end).
     */
    public List<Sheet> sheets() {
        final List<Sheet> sheets = new ArrayList<>();
        final double pw = halfWide + marginWidth;
        final double pd = halfDeep + marginWidth;
        // the platform: its four sides, and its top
        sheets.add(rect("platform, front", "platform", new Vec3(-pw, pd, 0), new Vec3(pw, pd, 0), new Vec3(pw, pd, platformHeight),
                new Vec3(-pw, pd, platformHeight), new Vec3(0, 1, 0)));
        sheets.add(rect("platform, back", "platform", new Vec3(-pw, -pd, 0), new Vec3(pw, -pd, 0),
                new Vec3(pw, -pd, platformHeight), new Vec3(-pw, -pd, platformHeight), new Vec3(0, -1, 0)));
        for (final double side : new double[] {-pw, pw}) {
            sheets.add(rect("platform, " + (side < 0 ? "left" : "right"), "platform", new Vec3(side, -pd, 0),
                    new Vec3(side, pd, 0), new Vec3(side, pd, platformHeight), new Vec3(side, -pd, platformHeight),
                    new Vec3(Math.signum(side), 0, 0)));
        }
        sheets.add(rect("platform, top", "platform", new Vec3(-pw, -pd, platformHeight), new Vec3(pw, -pd, platformHeight),
                new Vec3(pw, pd, platformHeight), new Vec3(-pw, pd, platformHeight), new Vec3(0, 0, 1)));
        // its steps: each riser, and each tread
        final double half = bayWidths[bayWidths.length / 2] / 2;
        final double rise = platformHeight / stepCount;
        for (int k = 0; k < stepCount; k++) {
            final double y = pd + GOING * k;
            final double z = platformHeight - rise * (k + 1);
            sheets.add(rect("steps, riser " + k, "platform", new Vec3(-half, y, z), new Vec3(half, y, z),
                    new Vec3(half, y, z + rise), new Vec3(-half, y, z + rise), new Vec3(0, 1, 0)));
            sheets.add(rect("steps, tread " + k, "platform", new Vec3(-half, y, z), new Vec3(half, y, z),
                    new Vec3(half, y + GOING, z), new Vec3(-half, y + GOING, z), new Vec3(0, 0, 1)));
        }
        // its back and side walls, on its lines of columns, from the platform to the columns' heads
        sheets.add(rect("back wall", "walls", new Vec3(-halfWide, -halfDeep, platformHeight), new Vec3(halfWide, -halfDeep, platformHeight),
                new Vec3(halfWide, -halfDeep, headHeight), new Vec3(-halfWide, -halfDeep, headHeight), new Vec3(0, -1, 0)));
        for (final double side : new double[] {-halfWide, halfWide}) {
            sheets.add(rect((side < 0 ? "left" : "right") + " wall", "walls", new Vec3(side, -halfDeep, platformHeight),
                    new Vec3(side, halfDeep, platformHeight), new Vec3(side, halfDeep, headHeight), new Vec3(side, -halfDeep, headHeight),
                    new Vec3(Math.signum(side), 0, 0)));
        }
        // the doors in its front's bays
        final double[] xs = lines(bayWidths, halfWide);
        for (int b = 0; b + 1 < xs.length; b++) {
            sheets.add(rect("doors, bay " + b, "doors", new Vec3(xs[b], halfDeep, platformHeight), new Vec3(xs[b + 1], halfDeep, platformHeight),
                    new Vec3(xs[b + 1], halfDeep, headHeight), new Vec3(xs[b], halfDeep, headHeight), new Vec3(0, 1, 0)));
        }
        // its roof: the eaves' corners, the ridge's ends
        final Vec3[] corner = new Vec3[4];
        final double[][] signs = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int c = 0; c < 4; c++) {
            corner[c] = new Vec3(signs[c][0] * (halfWide + OVERHANG + FLARE), signs[c][1] * (halfDeep + OVERHANG + FLARE),
                    eaveHeight + UPTURN);
        }
        final Vec3 west = new Vec3(-ridgeHalf, 0, ridgeHeight);
        final Vec3 east = new Vec3(ridgeHalf, 0, ridgeHeight);
        final Vec3 inside = new Vec3(0, 0, eaveHeight);
        // its long slopes: front (its eave from the front left corner to the front right), and back
        sheets.add(Sheet.patch("roof, front slope", "roof", eave(corner[3], corner[2]), hip(corner[2], east),
                List.of(west, east), hip(corner[3], west), 32, 12, inside));
        sheets.add(Sheet.patch("roof, back slope", "roof", eave(corner[0], corner[1]), hip(corner[1], east),
                List.of(west, east), hip(corner[0], west), 32, 12, inside));
        // its hip ends, each up to a ridge's end
        sheets.add(Sheet.patch("roof, east end", "roof", eave(corner[1], corner[2]), hip(corner[2], east), List.of(east),
                hip(corner[1], east), 16, 12, inside));
        sheets.add(Sheet.patch("roof, west end", "roof", eave(corner[0], corner[3]), hip(corner[3], west), List.of(west),
                hip(corner[0], west), 16, 12, inside));
        return sheets;
    }

    /** Its palettes: red lacquer, painted brackets, red walls on a stone platform; or dark timber and grey brick. */
    public static final Palette LACQUERED = new Palette("Red lacquer, painted brackets, red walls",
            Map.of(Role.WALL, Material.PLASTER, Role.BASE, Material.STONE, Role.DRESSING, Material.PAINTED,
                    Role.STRUCTURE, Material.LACQUER, Role.FRAME, Material.LACQUER, Role.ROOF, Material.GLAZED),
            30, 30, 0);
    public static final Palette PLAIN = new Palette("Dark timber, grey brick",
            Map.of(Role.WALL, Material.BRICK, Role.BASE, Material.STONE, Role.DRESSING, Material.TIMBER,
                    Role.STRUCTURE, Material.TIMBER, Role.FRAME, Material.TIMBER, Role.ROOF, Material.GREY_TILE),
            30, 30, 0);

    /**
     * The hall as a building to dress ({@link Building}): its skeleton, its faces; its columns, its
     * bracket sets and its roof's ridge, hips and ornaments as members; its roof's tiles a choice
     * (yellow glazed, green glazed, grey), the rest as its palette says.
     */
    public Building building() {
        final Map<String, Design> faces = Map.of(
                "platform", Design.slab(Role.BASE, -30, 0),
                "walls", Design.slab(Role.WALL, -15, 15),
                "doors", Design.slab(Role.FRAME, -5, 5),
                "roof", Design.slab(Role.ROOF, 0, 30));
        final Map<String, List<Building.Option>> choices = new LinkedHashMap<>();
        choices.put("Roof tiles", List.of(
                Building.Option.choosing("Yellow glazed", faces, Map.of(Role.ROOF, Material.GLAZED)),
                Building.Option.choosing("Green glazed", faces, Map.of(Role.ROOF, Material.GREEN_GLAZED)),
                Building.Option.choosing("Grey", faces, Map.of(Role.ROOF, Material.GREY_TILE))));
        // the columns: the hall's own, from the platform to their heads
        final Form hall = stacks().get(1).skeleton(true);
        final Building.Structure columns = (skeleton, palette) -> Building.Structure.members("column",
                e -> hall.at(e.from()).x() == hall.at(e.to()).x() && hall.at(e.from()).y() == hall.at(e.to()).y(),
                44, Role.STRUCTURE).of(lifted(hall), palette);
        // the bracket sets, every arm of them
        final Form sets = brackets().form();
        final Building.Structure brackets = (skeleton, palette) -> Building.Structure.members("bracket", e -> true, 12,
                Role.DRESSING).of(sets, palette);
        // the ridge, the hips and the ornaments: the roof's edges up from its eaves' corners and along its top
        final Form roof = roof().form();
        final Building.Structure ridges = (skeleton, palette) -> Building.Structure.members("ridge", e -> {
            final String a = roof.vertices().get(e.from()).name();
            final String b = roof.vertices().get(e.to()).name();
            final boolean top = a.startsWith("ridge") || a.startsWith("ornament");
            final boolean high = b.startsWith("ridge") || b.startsWith("ornament");
            return top && high || top && b.startsWith("eave corner") || high && a.startsWith("eave corner");
        }, 36, Role.ROOF).of(roof, palette);
        return new Building("temple hall", skeleton(), List.of(), List.of(), sheets(), List.of(columns, brackets, ridges),
                choices, List.of(LACQUERED, PLAIN));
    }

    /** {@code form}, lifted onto the platform. */
    private Form lifted(final Form form) {
        final Form.Builder up = new Form.Builder(form.name());
        for (final Form.Vertex v : form.vertices()) {
            up.vertex(v.name(), v.across(), v.along(), v.up() + platformHeight);
        }
        for (final Form.Edge e : form.edges()) {
            up.edge(e.from(), e.to(), e.curved() ? e.curve().moved(new Vec3(0, 0, platformHeight)) : null);
        }
        return up.build();
    }

    private int vertex(final Form.Builder form, final String name, final Vec3 at) {
        return form.vertex(name, at.x(), at.y(), at.z());
    }
}
