package com.example.fxsuite.viewer;

import javafx.geometry.Point3D;

/**
 * Where to stand to read a slate: straight in front of the face nearer the camera, level
 * with it and looking at it, far enough back that a slate {@code width} wide takes up
 * {@code share} of the screen's width (or of its height, for a tall one). Going by one width
 * for every slate keeps one reading distance, so bigger artifacts still look bigger. Pure
 * maths.
 *
 * @param fieldOfView the camera's vertical field of view, in degrees
 * @param aspect      the screen's width over its height
 */
public record Approach(double fieldOfView, double aspect, double share) {

    /** How far from the slate's centre to stand, for slates up to {@code width} by {@code height}. */
    public double distance(final double width, final double height) {
        final double tanV = Math.tan(Math.toRadians(fieldOfView) / 2);
        final double tanH = tanV * aspect;
        return Math.max(width / (2 * share * tanH), height / (2 * share * tanV));
    }

    /**
     * The standpoint for a slate centred at {@code centre}, coming from {@code from}. Slates
     * face -z, so their front is read looking along +z (heading 0) and their back looking
     * along -z (heading 180); whichever side the camera is already on, it stays on.
     */
    public FlyCamera standpoint(final Point3D centre, final double width, final double height, final Point3D from) {
        final double back = distance(width, height);
        final boolean behind = from.getZ() > centre.getZ();
        return new FlyCamera(centre.add(0, 0, behind ? back : -back), behind ? 180 : 0, 0);
    }
}
