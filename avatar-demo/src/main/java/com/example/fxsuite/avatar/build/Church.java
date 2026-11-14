package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Assembly.Placed;
import com.example.fxsuite.avatar.build.Grid.Quarter;
import com.example.fxsuite.avatar.build.Tower.Section;
import com.example.fxsuite.avatar.sim.Curve;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A town church, its skeleton composed of parts ({@link Assembly}): what stacks, from plans
 * ({@link Tower}); what does not, drawn as 3D graphs, its curves curved edges ({@link Curve}).
 *
 * <ul>
 *   <li>From plans: the nave, 24 m by 10 m, its walls 9 m high in six bays; the apse at its back, a
 *       half circle 10 m across, its walls 7.5 m high, its curve a curved edge; the tower at its
 *       front, 5 m square, in four stages to its belfry's top at 22 m.</li>
 *   <li>Drawn in 3D: the nave's roof, its ridge at 15 m, a rafter up to it each side at every bay,
 *       its gables the end ones; the apse's roof, ribs from its eaves up to an apex on the back gable;
 *       the spire, its four edges flared, bowed in toward its middle near its foot, to its tip at 37 m;
 *       the cross on it; the pointed arches of the nave's windows and of the belfry's openings, each
 *       two circular arcs; the tower's arched doorway.</li>
 * </ul>
 *
 * <p>Each 3D part says where it joins the parts from plans: the roof at the nave's eaves, the apse's
 * roof at its eaves, the spire at the tower's top, the doorway at the tower's foot. The front gable
 * runs through the tower's back corners, where the tower's stages meet it.
 */
public final class Church {

    private Church() {
    }

    // in centimetres: the nave's half width, its front and back, its eaves and ridge, its bays
    private static final double HALF = 500;
    private static final double FRONT = 1200;
    private static final double BACK = -1200;
    private static final double EAVES = 900;
    private static final double RIDGE = 1500;
    private static final double BAY = 400;
    // the tower: half its side, its middle along, its stages' tops
    private static final double TOWER = 250;
    private static final double TOWER_ALONG = FRONT + TOWER;
    private static final double CROSSING = EAVES + (RIDGE - EAVES) * (HALF - TOWER) / HALF;
    private static final double[] STAGES = {EAVES, CROSSING, 1700, 2200};
    // the apse: its eaves, and its roof's apex on the back gable
    private static final double APSE = 750;
    private static final double APEX = 1250;
    private static final double TIP = 3700;

    /** The plans its stacked parts are made from, by name. */
    public static Map<String, Plan> plans() {
        final Map<String, Plan> plans = new LinkedHashMap<>();
        final double[] bays = new double[(int) ((FRONT - BACK) / BAY)];
        java.util.Arrays.fill(bays, BAY);
        plans.put("nave", Plan.of("nave", new Grid("nave", -HALF, BACK, new double[] {2 * HALF}, bays, Map.of())));
        // a half circle: a grid half as deep as it is wide, its two back corners rounded right round
        plans.put("apse", Plan.of("apse", new Grid("apse", -HALF, -HALF, new double[] {HALF, HALF},
                new double[] {HALF}, Map.of()).shaped(Quarter.SOUTH_WEST, Corner.round(HALF))
                .shaped(Quarter.SOUTH_EAST, Corner.round(HALF))));
        plans.put("tower", Plan.of("tower", new Grid("tower", -TOWER, -TOWER, new double[] {2 * TOWER},
                new double[] {2 * TOWER}, Map.of())));
        return plans;
    }

    /** Its stacked parts: the nave, the apse, the tower in its stages. */
    public static List<Tower> stacks() {
        final Map<String, Plan> plans = plans();
        final List<Section> stages = new ArrayList<>();
        double below = 0;
        final String[] names = {"base", "lower stage", "upper stage", "belfry"};
        for (int s = 0; s < STAGES.length; s++) {
            stages.add(new Section(names[s], plans.get("tower"), 1, STAGES[s] - below));
            below = STAGES[s];
        }
        return List.of(Tower.of("nave", plans.get("nave"), 1, EAVES), Tower.of("apse", plans.get("apse"), 1, APSE),
                new Tower("tower", stages));
    }

    /** Its parts, placed. */
    public static List<Placed> parts() {
        final List<Tower> stacks = stacks();
        final List<Placed> parts = new ArrayList<>();
        parts.add(new Placed("nave", stacks.get(0).skeleton(true), Vec3.ZERO, 0, List.of()));
        parts.add(new Placed("apse", stacks.get(1).skeleton(true), new Vec3(0, BACK, 0), 0, List.of()));
        parts.add(new Placed("tower", stacks.get(2).skeleton(true), new Vec3(0, TOWER_ALONG, 0), 0, List.of()));
        parts.add(roof());
        parts.add(apseRoof(stacks.get(1)));
        parts.add(spire());
        parts.add(cross());
        parts.add(openings());
        return parts;
    }

    /** It all, put together. */
    public static Assembly.Result assembled() {
        return Assembly.of("town church", parts());
    }

    /** Its skeleton. */
    public static Form skeleton() {
        return assembled().form();
    }

    /** The nave's roof: its ridge; at every bay a rafter up to it from the eaves each side; its gables the end ones. */
    private static Placed roof() {
        final Form.Builder form = new Form.Builder("roof");
        final List<String> joints = new ArrayList<>();
        int previous = -1;
        for (double along = BACK; along <= FRONT + 1e-6; along += BAY) {
            final int ridge = form.vertex("ridge at " + Math.round(along), 0, along, RIDGE);
            for (final double side : new double[] {-HALF, HALF}) {
                final String eaves = (side < 0 ? "left" : "right") + " eaves at " + Math.round(along);
                form.edge(form.vertex(eaves, side, along, EAVES), ridge);
                joints.add(eaves);
            }
            if (previous >= 0) {
                form.edge(previous, ridge);
            }
            previous = ridge;
        }
        return new Placed("roof", form.build(), Vec3.ZERO, 0, joints);
    }

    /** The apse's roof: a rib from each point of its eaves (but the middle of its back) up to its apex on the back gable. */
    private static Placed apseRoof(final Tower apse) {
        final Form.Builder form = new Form.Builder("apse roof");
        final int apex = form.vertex("apex", 0, BACK, APEX);
        final List<String> joints = new ArrayList<>();
        final Form plan = apse.sections().getFirst().plan().layout().graph(true);
        int k = 0;
        for (final Form.Vertex v : plan.vertices()) {
            // its eaves: the points of its outline, but where the apex stands right over one
            final boolean onOutline = plan.edges().stream().anyMatch(e -> (e.from() == plan.vertex(v.name())
                    || e.to() == plan.vertex(v.name())) && e.curved()) || Math.abs(v.along()) < 1e-6;
            if (!onOutline || Math.abs(v.across()) < 1e-6 && Math.abs(v.along()) < 1e-6) {
                continue;
            }
            final String name = "eaves " + k++;
            form.edge(form.vertex(name, v.across(), BACK + v.along(), APSE), apex);
            joints.add(name);
        }
        return new Placed("apse roof", form.build(), Vec3.ZERO, 0, joints);
    }

    /** The spire: from each corner of the tower's top to its tip, each edge bowed in toward the middle near its foot. */
    private static Placed spire() {
        final Form.Builder form = new Form.Builder("spire");
        final int tip = form.vertex("tip", 0, TOWER_ALONG, TIP);
        final List<String> joints = new ArrayList<>();
        final double top = STAGES[STAGES.length - 1];
        for (final double[] c : new double[][] {{-TOWER, -TOWER}, {TOWER, -TOWER}, {TOWER, TOWER}, {-TOWER, TOWER}}) {
            final String name = "foot " + Math.round(c[0]) + " " + Math.round(c[1]);
            final int foot = form.vertex(name, c[0], TOWER_ALONG + c[1], top);
            // bowed in, square to the edge, toward the spire's middle: a flare at its foot
            final Vec3 run = new Vec3(-c[0], -c[1], TIP - top).normalize();
            final Vec3 in = new Vec3(-c[0], -c[1], 0).normalize();
            final Vec3 bow = in.subtract(run.multiply(in.dotProduct(run))).normalize().multiply(45);
            form.edge(foot, tip, Curve.bow(bow, 3));
            joints.add(name);
        }
        return new Placed("spire", form.build(), Vec3.ZERO, 0, joints);
    }

    /** The cross on the spire's tip. */
    private static Placed cross() {
        final Form.Builder form = new Form.Builder("cross");
        final int foot = form.vertex("foot", 0, TOWER_ALONG, TIP);
        final int middle = form.vertex("middle", 0, TOWER_ALONG, TIP + 130);
        form.line(foot, middle, form.vertex("head", 0, TOWER_ALONG, TIP + 200));
        form.line(form.vertex("arm left", -45, TOWER_ALONG, TIP + 130), middle,
                form.vertex("arm right", 45, TOWER_ALONG, TIP + 130));
        return new Placed("cross", form.build(), Vec3.ZERO, 0, List.of("foot"));
    }

    /**
     * Its openings: a pointed window in each bay of each side of the nave; a pointed opening in each
     * face of the belfry; the arched doorway in the tower's foot, its feet on the ground, joined there.
     */
    private static Placed openings() {
        final Form.Builder form = new Form.Builder("openings");
        final List<String> joints = new ArrayList<>();
        for (double along = BACK + BAY / 2; along < FRONT; along += BAY) {
            for (final double side : new double[] {-HALF, HALF}) {
                // on a side wall, its along the wall's own along, seen from outside
                final Vec3 origin = new Vec3(side, along, 0);
                final Vec3 u = new Vec3(0, side < 0 ? -1 : 1, 0);
                arch(form, "window, " + (side < 0 ? "left" : "right") + ", at " + Math.round(along), origin, u, 120,
                        300, 640, false, joints);
            }
        }
        final double top = STAGES[STAGES.length - 1];
        final Vec3[][] faces = {
                {new Vec3(0, TOWER_ALONG + TOWER, 0), new Vec3(-1, 0, 0)},
                {new Vec3(0, TOWER_ALONG - TOWER, 0), new Vec3(1, 0, 0)},
                {new Vec3(-TOWER, TOWER_ALONG, 0), new Vec3(0, -1, 0)},
                {new Vec3(TOWER, TOWER_ALONG, 0), new Vec3(0, 1, 0)}};
        final String[] names = {"front", "back", "left", "right"};
        for (int f = 0; f < faces.length; f++) {
            arch(form, "belfry, " + names[f], faces[f][0], faces[f][1], 160, top - 440, top - 210, false, joints);
        }
        arch(form, "doorway", faces[0][0], faces[0][1], 180, 0, 230, true, joints);
        return new Placed("openings", form.build(), Vec3.ZERO, 0, joints);
    }

    /**
     * A pointed arch, {@code width} wide about {@code middle} on a wall: its jambs up from {@code sill} to
     * {@code spring}, then two circular arcs, each struck from the other's springing (an equilateral
     * arch), meeting at its point; across its sill, unless it is a doorway, its feet then on the ground,
     * joints. {@code u} the wall's along, seen from outside.
     */
    private static void arch(final Form.Builder form, final String name, final Vec3 middle, final Vec3 u,
                             final double width, final double sill, final double spring, final boolean doorway,
                             final List<String> joints) {
        final double half = width / 2;
        final Vec3 up = new Vec3(0, 0, 1);
        final Vec3 left = middle.add(u.multiply(-half));
        final Vec3 right = middle.add(u.multiply(half));
        final Vec3 springLeft = left.add(up.multiply(spring));
        final Vec3 springRight = right.add(up.multiply(spring));
        final Vec3 point = middle.add(up.multiply(spring + width * Math.sqrt(3) / 2));
        final int footLeft = vertex(form, name + ", foot left", left.add(up.multiply(sill)));
        final int footRight = vertex(form, name + ", foot right", right.add(up.multiply(sill)));
        final int sl = vertex(form, name + ", spring left", springLeft);
        final int sr = vertex(form, name + ", spring right", springRight);
        final int pt = vertex(form, name + ", point", point);
        // each arc's rise: its middle, round its centre (the other springing), less its chord's middle
        final Vec3 leftMiddle = springRight.add(u.multiply(width * Math.cos(Math.toRadians(150))))
                .add(up.multiply(width * Math.sin(Math.toRadians(150))));
        final Vec3 rightMiddle = springLeft.add(u.multiply(width * Math.cos(Math.toRadians(30))))
                .add(up.multiply(width * Math.sin(Math.toRadians(30))));
        form.edge(footLeft, sl);
        form.edge(sl, pt, Curve.arc(leftMiddle.subtract(springLeft.add(point).multiply(0.5))));
        form.edge(pt, sr, Curve.arc(rightMiddle.subtract(point.add(springRight).multiply(0.5))));
        form.edge(sr, footRight);
        if (doorway) {
            joints.add(name + ", foot left");
            joints.add(name + ", foot right");
        } else {
            form.edge(footRight, footLeft);
        }
    }

    private static int vertex(final Form.Builder form, final String name, final Vec3 at) {
        return form.vertex(name, at.x(), at.y(), at.z());
    }
}
