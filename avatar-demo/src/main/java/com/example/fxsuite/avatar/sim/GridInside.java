package com.example.fxsuite.avatar.sim;

/**
 * The plain inside: a box cut into a grid of cells, so long across, along and up each, the last cell each way cut short
 * where the box ends. Cells are numbered across first, then along, then up.
 */
public final class GridInside implements Inside {

    private final Box bounds;
    private final double sizeX;
    private final double sizeZ;
    private final double sizeUp;
    private final int nx;
    private final int nz;
    private final int nu;

    public GridInside(final Box bounds, final double sizeX, final double sizeZ, final double sizeUp) {
        if (!(sizeX > 0 && sizeZ > 0 && sizeUp > 0)) {
            throw new IllegalArgumentException("cells some size each way: " + sizeX + ", " + sizeZ + ", " + sizeUp);
        }
        this.bounds = bounds;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.sizeUp = sizeUp;
        nx = count(bounds.maxX() - bounds.minX(), sizeX);
        nz = count(bounds.maxZ() - bounds.minZ(), sizeZ);
        nu = count(bounds.high() - bounds.low(), sizeUp);
        if ((long) nx * nz * nu > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("too many cells: " + nx + " x " + nz + " x " + nu);
        }
    }

    private static int count(final double extent, final double size) {
        return Math.max(1, (int) Math.ceil(extent / size));
    }

    /** Which cell, one way, a distance from the box's start falls in: on the far side, the last. */
    private static int index(final double from, final double size, final int n) {
        return Math.min(n - 1, Math.max(0, (int) Math.floor(from / size)));
    }

    private int id(final int ix, final int iz, final int iu) {
        return (iu * nz + iz) * nx + ix;
    }

    @Override
    public Box bounds() {
        return bounds;
    }

    @Override
    public int count() {
        return nx * nz * nu;
    }

    @Override
    public int cell(final double across, final double along, final double up) {
        if (!bounds.holds(across, along, up)) {
            return -1;
        }
        return id(index(across - bounds.minX(), sizeX, nx), index(along - bounds.minZ(), sizeZ, nz),
                index(up - bounds.low(), sizeUp, nu));
    }

    @Override
    public int[] cells(final Box box) {
        if (!bounds.meets(box)) {
            return new int[0];
        }
        final int x0 = index(Math.max(box.minX(), bounds.minX()) - bounds.minX(), sizeX, nx);
        final int x1 = index(Math.min(box.maxX(), bounds.maxX()) - bounds.minX(), sizeX, nx);
        final int z0 = index(Math.max(box.minZ(), bounds.minZ()) - bounds.minZ(), sizeZ, nz);
        final int z1 = index(Math.min(box.maxZ(), bounds.maxZ()) - bounds.minZ(), sizeZ, nz);
        final int u0 = index(Math.max(box.low(), bounds.low()) - bounds.low(), sizeUp, nu);
        final int u1 = index(Math.min(box.high(), bounds.high()) - bounds.low(), sizeUp, nu);
        final int[] found = new int[(x1 - x0 + 1) * (z1 - z0 + 1) * (u1 - u0 + 1)];
        int n = 0;
        for (int iu = u0; iu <= u1; iu++) {
            for (int iz = z0; iz <= z1; iz++) {
                for (int ix = x0; ix <= x1; ix++) {
                    found[n++] = id(ix, iz, iu);
                }
            }
        }
        return found;
    }

    @Override
    public Box box(final int cell) {
        if (cell < 0 || cell >= count()) {
            throw new IllegalArgumentException("no cell " + cell + " of " + count());
        }
        final int ix = cell % nx;
        final int iz = cell / nx % nz;
        final int iu = cell / (nx * nz);
        final double x = bounds.minX() + ix * sizeX;
        final double z = bounds.minZ() + iz * sizeZ;
        final double u = bounds.low() + iu * sizeUp;
        return new Box(x, Math.min(bounds.maxX(), x + sizeX), z, Math.min(bounds.maxZ(), z + sizeZ), u,
                Math.min(bounds.high(), u + sizeUp));
    }
}
