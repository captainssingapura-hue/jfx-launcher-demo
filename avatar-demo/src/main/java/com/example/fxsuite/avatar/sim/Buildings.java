package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Typical buildings, as forms ({@link Form}): their skeletons only, vertices and edges, the lines
 * a building's shape is drawn by. Each faces ahead (+along), its middle at across 0 and along 0,
 * standing on the ground; every vertex named for where it is, so whatever is laid on the shape
 * later (its faces, how it is drawn, what bears what) can say which part it means.
 */
public final class Buildings {

    private Buildings() {
    }

    /** A point on a wall: {@code u} along the wall, {@code up} from the ground, as across, along and up. */
    @FunctionalInterface
    private interface Wall {
        double[] at(double u, double up);
    }

    /** A wall square to across (a front or a back), at {@code along}: u is across. */
    private static Wall facing(final double along) {
        return (u, up) -> new double[] {u, along, up};
    }

    /** A wall square to along (a side), at {@code across}: u is along. */
    private static Wall side(final double across) {
        return (u, up) -> new double[] {across, u, up};
    }

    private static int vertex(final Form.Builder form, final String name, final double[] at) {
        return form.vertex(name, at[0], at[1], at[2]);
    }

    /**
     * A level of a rectangular grid, at {@code up}: a vertex at every crossing of {@code xs}
     * (across, left to right) and {@code zs} (along, back to front), named {@code name, x k z j};
     * and the edges round its edge. Its vertices' numbers, by [x][z].
     */
    private static int[][] grid(final Form.Builder form, final String name, final double[] xs, final double[] zs,
                                final double up) {
        return grid(form, name, xs, zs, up, true);
    }

    /** As {@link #grid}, its vertices inside its edge too if {@code inside}; -1 there, if not. */
    private static int[][] grid(final Form.Builder form, final String name, final double[] xs, final double[] zs,
                                final double up, final boolean inside) {
        final int[][] at = new int[xs.length][zs.length];
        for (int i = 0; i < xs.length; i++) {
            for (int j = 0; j < zs.length; j++) {
                final boolean edge = i == 0 || j == 0 || i == xs.length - 1 || j == zs.length - 1;
                at[i][j] = !edge && !inside ? -1 : form.vertex(name + ", x" + i + " z" + j, xs[i], zs[j], up);
            }
        }
        form.loop(round(at));
        return at;
    }

    /** A grid's vertices round its edge, in turn: along its front, down its right side, its back, up its left. */
    private static int[] round(final int[][] at) {
        final int nx = at.length;
        final int nz = at[0].length;
        final List<Integer> round = new ArrayList<>();
        for (int i = 0; i < nx; i++) {
            round.add(at[i][nz - 1]);
        }
        for (int j = nz - 2; j >= 0; j--) {
            round.add(at[nx - 1][j]);
        }
        for (int i = nx - 2; i >= 0; i--) {
            round.add(at[i][0]);
        }
        for (int j = 1; j < nz - 1; j++) {
            round.add(at[0][j]);
        }
        return round.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Columns: an edge up from each of {@code lower}'s vertices to {@code upper}'s, where {@code stands} says. */
    private static void columns(final Form.Builder form, final int[][] lower, final int[][] upper,
                                final boolean[][] stands) {
        for (int i = 0; i < lower.length; i++) {
            for (int j = 0; j < lower[0].length; j++) {
                if (stands == null || stands[i][j]) {
                    form.edge(lower[i][j], upper[i][j]);
                }
            }
        }
    }

    /** Whether each crossing of a grid {@code nx} by {@code nz} is on its edge. */
    private static boolean[][] edgeOnly(final int nx, final int nz) {
        final boolean[][] on = new boolean[nx][nz];
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nz; j++) {
                on[i][j] = i == 0 || j == 0 || i == nx - 1 || j == nz - 1;
            }
        }
        return on;
    }

    /**
     * A pointed arch on {@code wall}, {@code width} wide about {@code middle}: its jambs up from
     * {@code sill} to {@code spring}, then two arcs, each struck from the other's springing, meeting
     * at a point (an equilateral arch). Closed along its sill, as a window is; or, a doorway, open
     * there, its feet the vertices {@code feet} already on the ground.
     */
    private static void arch(final Form.Builder form, final String name, final Wall wall, final double middle,
                             final double width, final double sill, final double spring, final int[] feet) {
        final double half = width / 2;
        final List<Integer> points = new ArrayList<>();
        points.add(feet != null ? feet[0] : vertex(form, name + ", foot left", wall.at(middle - half, sill)));
        points.add(vertex(form, name + ", spring left", wall.at(middle - half, spring)));
        final int steps = 3;
        // the left arc, round its centre at the right springing, from the left springing to the point
        for (int k = 1; k < steps; k++) {
            final double a = Math.toRadians(180 - 60.0 * k / steps);
            points.add(vertex(form, name + ", arc left " + k,
                    wall.at(middle + half + width * Math.cos(a), spring + width * Math.sin(a))));
        }
        points.add(vertex(form, name + ", point", wall.at(middle, spring + width * Math.sin(Math.toRadians(60)))));
        for (int k = 1; k < steps; k++) {
            final double a = Math.toRadians(60 - 60.0 * k / steps);
            points.add(vertex(form, name + ", arc right " + k,
                    wall.at(middle - half + width * Math.cos(a), spring + width * Math.sin(a))));
        }
        points.add(vertex(form, name + ", spring right", wall.at(middle + half, spring)));
        points.add(feet != null ? feet[1] : vertex(form, name + ", foot right", wall.at(middle + half, sill)));
        final int[] line = points.stream().mapToInt(Integer::intValue).toArray();
        if (feet != null) {
            form.line(line);
        } else {
            form.loop(line);
        }
    }

    /** 0, step, 2 step, ... from {@code from} to {@code to}, both included. */
    private static double[] steps(final double from, final double to, final double step) {
        final int n = (int) Math.round((to - from) / step);
        final double[] at = new double[n + 1];
        for (int k = 0; k <= n; k++) {
            at[k] = from + k * step;
        }
        return at;
    }

    /**
     * A brutalist low-rise block of flats, its structure: a long concrete slab, 36 m by 12 m, five
     * storeys and 17 m high, on a 6 m grid of columns, three lines of them deep, its ground storey
     * taller (on pilotis); a parapet round its roof, and the stair and lift tower standing up from the
     * middle of it. Its faces, how it is dressed, are its surfaces' designs' to say
     * ({@code build.Apartments}).
     */
    public static Form apartments() {
        final Form.Builder form = new Form.Builder("brutalist apartments");
        final double[] xs = steps(-1800, 1800, 600);
        final double[] zs = {-600, 0, 600};
        final double[] levels = {0, 420, 720, 1020, 1320, 1620};
        final int[][][] floor = new int[levels.length][][];
        for (int l = 0; l < levels.length; l++) {
            floor[l] = grid(form, l == 0 ? "ground" : l == levels.length - 1 ? "roof" : "floor " + l, xs, zs, levels[l]);
            if (l > 0) {
                columns(form, floor[l - 1], floor[l], null);
                // its floor's beams, front to back along each line of columns
                for (int i = 1; i < xs.length - 1; i++) {
                    form.line(floor[l][i][0], floor[l][i][1], floor[l][i][2]);
                }
            }
        }
        // the parapet round the roof
        final double roof = levels[levels.length - 1];
        final int[][] parapet = new int[xs.length][zs.length];
        for (final int[] corner : new int[][] {{0, 0}, {xs.length - 1, 0}, {xs.length - 1, zs.length - 1}, {0, zs.length - 1}}) {
            parapet[corner[0]][corner[1]] = form.vertex("parapet, x" + corner[0] + " z" + corner[1], xs[corner[0]],
                    zs[corner[1]], roof + 100);
            form.edge(floor[levels.length - 1][corner[0]][corner[1]], parapet[corner[0]][corner[1]]);
        }
        form.loop(parapet[0][zs.length - 1], parapet[xs.length - 1][zs.length - 1], parapet[xs.length - 1][0],
                parapet[0][0]);
        // the stair and lift tower on the roof
        final double[] core = {-300, 300};
        final int[][] base = grid(form, "roof tower, base", core, core, roof);
        final int[][] top = grid(form, "roof tower, top", core, core, roof + 350);
        columns(form, base, top, null);
        return form.build();
    }

    /**
     * A typical office skyscraper: a two-storey podium 48 m square; on it the tower, 32 m square,
     * 36 floors of 4 m, its curtain wall's mullions every 4 m, its corners and every other mullion
     * its columns; set back to 24 m square for its top eight floors; a concrete core 12 m square
     * through its middle, beams out from it to the tower's corners at every floor; a screen round
     * its plant on the roof; and a spire on its core, a cross ring every 8 m, to 200 m.
     */
    public static Form skyscraper() {
        final Form.Builder form = new Form.Builder("skyscraper");
        final double[] podium = steps(-2400, 2400, 800);
        final int[][] street = grid(form, "podium, ground", podium, podium, 0, false);
        final int[][] mezzanine = grid(form, "podium, first", podium, podium, 500, false);
        final int[][] terrace = grid(form, "podium, roof", podium, podium, 1000, false);
        final boolean[][] podiumEdge = edgeOnly(podium.length, podium.length);
        columns(form, street, mezzanine, podiumEdge);
        columns(form, mezzanine, terrace, podiumEdge);
        // the core, from the street to the roof
        final double[] core = {-600, 600};
        int[][] coreBelow = grid(form, "core, 0", core, core, 0);
        // the tower, floor by floor
        final double base = 1000;
        final double storey = 400;
        final int floors = 36;
        final int setback = 28;
        int[][] below = null;
        boolean[][] belowStands = null;
        for (int f = 0; f <= floors; f++) {
            final double up = base + f * storey;
            final double half = f < setback ? 1600 : 1200;
            final double[] mullions = steps(-half, half, 400);
            final int[][] ring = grid(form, "tower, floor " + f, mullions, mullions, up, false);
            final boolean[][] stands = edgeOnly(mullions.length, mullions.length);
            if (f == 0) {
                // the tower's foot on the podium's roof: its corners to the podium's
                form.edge(ring[0][0], terrace[0][0]).edge(ring[mullions.length - 1][0], terrace[podium.length - 1][0])
                        .edge(ring[0][mullions.length - 1], terrace[0][podium.length - 1])
                        .edge(ring[mullions.length - 1][mullions.length - 1], terrace[podium.length - 1][podium.length - 1]);
            } else if (f == setback) {
                // set back: the terrace's corners out to the floor below's
                final int n = below.length - 1;
                final int m = mullions.length - 1;
                form.edge(ring[0][0], below[0][0]).edge(ring[m][0], below[n][0]).edge(ring[0][m], below[0][n])
                        .edge(ring[m][m], below[n][n]);
            } else {
                columns(form, below, ring, belowStands);
            }
            // the core, up a floor; and the beams out from its corners to the tower's
            final int[][] coreHere = grid(form, "core, " + (f + 1), core, core, up);
            columns(form, coreBelow, coreHere, null);
            final int m = mullions.length - 1;
            form.edge(coreHere[0][0], ring[0][0]).edge(coreHere[1][0], ring[m][0]).edge(coreHere[0][1], ring[0][m])
                    .edge(coreHere[1][1], ring[m][m]);
            coreBelow = coreHere;
            below = ring;
            belowStands = stands;
        }
        // the screen round the plant on the roof: every other mullion up 6 m, a rail round their tops
        final double roof = base + floors * storey;
        final double[] screenAt = steps(-1200, 1200, 800);
        final int[][] screen = grid(form, "screen", screenAt, screenAt, roof + 600, false);
        for (int i = 0; i < screenAt.length; i++) {
            for (int j = 0; j < screenAt.length; j++) {
                if (i == 0 || j == 0 || i == screenAt.length - 1 || j == screenAt.length - 1) {
                    form.edge(below[i * 2][j * 2], screen[i][j]);
                }
            }
        }
        // the core up to it, and the spire on the core: tapering to a mast, a ring round it every 8 m
        final int[][] coreTop = grid(form, "core, top", core, core, roof + 600);
        columns(form, coreBelow, coreTop, null);
        int[][] spire = coreTop;
        for (int k = 0; k <= 4; k++) {
            final double up = roof + 1400 + k * 800;
            final double r = 220 * (1 - k / 5.0);
            final int[][] ring = grid(form, "spire, ring " + k, new double[] {-r, r}, new double[] {-r, r}, up);
            columns(form, spire, ring, null);
            spire = ring;
        }
        final int tip = form.vertex("spire, tip", 0, 0, 20_000);
        for (final int[] corner : spire) {
            for (final int v : corner) {
                form.edge(v, tip);
            }
        }
        return form.build();
    }

    /**
     * A town church: a nave 24 m long and 10 m wide, its walls 9 m high in six bays, a tall
     * pointed window in each bay on each side, under a pitched roof to a ridge at 15 m, its rafters
     * at every bay; a rounded apse at its east end (its back), lower, its roof a half cone against
     * the nave's gable; and at its west end (its front) the tower, 5 m square, the pointed doorway
     * in its foot, a pointed opening in each face of its belfry, and on it a pyramid spire, a cross
     * on its tip at 39 m.
     */
    public static Form church() {
        final Form.Builder form = new Form.Builder("town church");
        final double half = 500;
        final double front = 1200;
        final double back = -1200;
        final double eaves = 900;
        final double ridge = 1500;
        final double[] bays = steps(back, front, 400);
        final double tower = 250;
        final double apse = 750;
        // the nave's walls: round its foot and its eaves, through each bay, and where the tower stands against it
        final int[] ground = new int[2 * bays.length + 2];
        final int[] top = new int[ground.length];
        final double[][] round = new double[ground.length][];
        int n = 0;
        round[n++] = new double[] {-half, front};
        round[n++] = new double[] {-tower, front};
        round[n++] = new double[] {tower, front};
        for (int k = bays.length - 1; k >= 0; k--) {
            round[n++] = new double[] {half, bays[k]};
        }
        for (int k = 0; k < bays.length - 1; k++) {
            round[n++] = new double[] {-half, bays[k]};
        }
        for (int k = 0; k < round.length; k++) {
            final String at = "nave " + k;
            ground[k] = form.vertex(at + ", foot", round[k][0], round[k][1], 0);
            top[k] = form.vertex(at + ", eaves", round[k][0], round[k][1], eaves);
        }
        form.loop(ground);
        form.loop(top);
        // its walls' piers; those at its back corners in two, where the apse's eaves meet them
        final int backRight = 2 + bays.length;
        final int backLeft = backRight + 1;
        final int apseRight = form.vertex("apse, eaves right", half, back, apse);
        final int apseLeft = form.vertex("apse, eaves left", -half, back, apse);
        for (int k = 0; k < round.length; k++) {
            if (k == backRight) {
                form.line(ground[k], apseRight, top[k]);
            } else if (k == backLeft) {
                form.line(ground[k], apseLeft, top[k]);
            } else if (k != 1 && k != 2) {
                form.edge(ground[k], top[k]);
            }
        }
        // the roof: its ridge, and a rafter up to it from each side at every bay; the front gable's
        // through where the tower's back corners rise past it
        final int[] ridgeAt = new int[bays.length];
        for (int k = 0; k < bays.length; k++) {
            ridgeAt[k] = form.vertex("ridge " + k, 0, bays[k], ridge);
        }
        form.line(ridgeAt);
        final double crossing = eaves + (ridge - eaves) * (half - tower) / half;
        final int gableLeft = form.vertex("front gable, left, at the tower", -tower, front, crossing);
        final int gableRight = form.vertex("front gable, right, at the tower", tower, front, crossing);
        form.line(top[0], gableLeft, ridgeAt[bays.length - 1]);
        form.line(top[3], gableRight, ridgeAt[bays.length - 1]);
        for (int k = 0; k < bays.length - 1; k++) {
            // right side: round[] runs from the front down the right; left side: from the back up the left
            form.edge(top[3 + (bays.length - 1 - k)], ridgeAt[k]);
            form.edge(top[3 + bays.length + k], ridgeAt[k]);
        }
        // a window in each bay, each side
        for (int k = 0; k + 1 < bays.length; k++) {
            final double middle = (bays[k] + bays[k + 1]) / 2;
            arch(form, "window, right, bay " + k, side(half), middle, 120, 300, 640, null);
            arch(form, "window, left, bay " + k, side(-half), middle, 120, 300, 640, null);
        }
        // the apse: a half ring round the back, its foot and its eaves, its roof a half cone up to the gable
        final int[] apseFoot = new int[7];
        final int[] apseTop = new int[7];
        apseFoot[0] = ground[backRight];
        apseTop[0] = apseRight;
        apseFoot[6] = ground[backLeft];
        apseTop[6] = apseLeft;
        for (int k = 1; k < 6; k++) {
            final double a = Math.toRadians(180.0 * k / 6);
            final double x = half * Math.cos(a);
            final double z = back - half * Math.sin(a);
            apseFoot[k] = form.vertex("apse " + k + ", foot", x, z, 0);
            apseTop[k] = form.vertex("apse " + k + ", eaves", x, z, apse);
            form.edge(apseFoot[k], apseTop[k]);
        }
        form.line(apseFoot);
        form.line(apseTop);
        final int apex = form.vertex("apse, roof's apex", 0, back, 1250);
        for (final int v : apseTop) {
            form.edge(v, apex);
        }
        // the tower, against the nave's front: its back corners on the nave's front wall
        final double far = front + 2 * tower;
        final int doorLeft = form.vertex("tower, door, foot left", -90, far, 0);
        final int doorRight = form.vertex("tower, door, foot right", 90, far, 0);
        final int frontLeft = form.vertex("tower, front left, foot", -tower, far, 0);
        final int frontRight = form.vertex("tower, front right, foot", tower, far, 0);
        form.line(ground[1], frontLeft, doorLeft);
        form.line(doorRight, frontRight, ground[2]);
        arch(form, "tower, door", facing(far), 0, 180, 0, 230, new int[] {doorLeft, doorRight});
        final double[] levels = {eaves, crossing, 1700, 2200};
        int[] lower = {ground[1], frontLeft, frontRight, ground[2]};
        for (int l = 0; l < levels.length; l++) {
            final int[] ring = new int[4];
            ring[0] = l == 0 ? top[1] : l == 1 ? gableLeft : form.vertex("tower " + l + ", back left", -tower, front, levels[l]);
            ring[1] = form.vertex("tower " + l + ", front left", -tower, far, levels[l]);
            ring[2] = form.vertex("tower " + l + ", front right", tower, far, levels[l]);
            ring[3] = l == 0 ? top[2] : l == 1 ? gableRight : form.vertex("tower " + l + ", back right", tower, front, levels[l]);
            if (l == 0) {
                form.line(ring);  // its back, the nave's eaves between them, is there already
            } else {
                form.loop(ring);
            }
            for (int c = 0; c < 4; c++) {
                form.edge(lower[c], ring[c]);
            }
            lower = ring;
        }
        // the belfry's openings, one in each face
        final double middle = front + tower;
        arch(form, "belfry, front", facing(far), 0, 160, 1760, 1990, null);
        arch(form, "belfry, back", facing(front), 0, 160, 1760, 1990, null);
        arch(form, "belfry, right", side(tower), middle, 160, 1760, 1990, null);
        arch(form, "belfry, left", side(-tower), middle, 160, 1760, 1990, null);
        // the spire, and the cross on its tip
        final int tip = form.vertex("spire, tip", 0, middle, 3700);
        for (final int corner : lower) {
            form.edge(corner, tip);
        }
        final int crossing2 = form.vertex("cross, middle", 0, middle, 3830);
        final int head = form.vertex("cross, head", 0, middle, 3900);
        form.line(tip, crossing2, head);
        form.line(form.vertex("cross, arm left", -45, middle, 3830), crossing2,
                form.vertex("cross, arm right", 45, middle, 3830));
        return form.build();
    }

    /** The three, by name. */
    public static List<Form> all() {
        return List.of(apartments(), skyscraper(), church());
    }
}
