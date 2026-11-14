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

    /** The two drawn whole here, by name (the town church is composed of parts: {@code build.Church}). */
    public static List<Form> all() {
        return List.of(apartments(), skyscraper());
    }
}
