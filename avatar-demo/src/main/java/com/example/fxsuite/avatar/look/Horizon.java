package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Vec3;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;

/**
 * The sky and the far ground, so the world has no edge to be seen: round the camera, wherever it
 * goes, so it is never come nearer to.
 *
 * <ul>
 *   <li><b>The sky</b>: a dome {@link #FAR} round, lit by itself (the sun does not shade it), deeper
 *       blue overhead, paling to a haze at the horizon, and the haze below it;</li>
 *   <li><b>the far ground</b>: a ring lying just over the ground from {@link #HAZE} out to the dome,
 *       its grass, as the sun lights it, fading into the same haze; so the land runs on into the
 *       sky with no line between them, as far things do in the air.</li>
 * </ul>
 *
 * <p>What stands far off is not hazed: only the ground is.
 */
public final class Horizon {

    /** How far round the camera the sky is: as far as it draws, nearly. */
    public static final double FAR = 400_000;
    /** How far out from the camera the ground begins to fade into the haze. */
    public static final double HAZE = 120_000;
    /** How far over the ground the far ground lies: enough to be told from it, that far off. */
    private static final double OVER = 100;

    /** Overhead; at the horizon, the haze. */
    public static final Color ZENITH = Color.web("#6f9cc6");
    public static final Color HAZY = Color.web("#c3cfd6");

    private final Group root = new Group();
    private final Sphere dome;
    private final MeshView ground;

    /** The sky and the far ground, the ground {@code grass}, lit as {@code lit} lights it from overhead, each channel ({@link #lighting}). */
    public Horizon(final Color grass, final double[] lit) {
        dome = new Sphere(FAR, 96);
        dome.setCullFace(CullFace.NONE);
        dome.setMaterial(glowing(sky()));
        ground = ring(HAZE, FAR * 0.995);
        ground.setMaterial(glowing(fading(times(grass, lit), HAZY)));
        ground.setTranslateY(-OVER);
        root.getChildren().addAll(ground, dome);
    }

    public Node node() {
        return root;
    }

    /** Round {@code eye}, where the camera now is: the sky round it, the far ground under it. */
    public void follow(final Vec3 eye) {
        dome.setTranslateX(eye.x());
        dome.setTranslateY(eye.y());
        dome.setTranslateZ(eye.z());
        ground.setTranslateX(eye.x());
        ground.setTranslateZ(eye.z());
    }

    /**
     * How much a light {@code light} coming {@code way} (as a DirectionalLight's direction) and {@code ambient}
     * light the ground from overhead, each channel: red, green, blue, together more than one, as lights add.
     */
    public static double[] lighting(final Color ambient, final Color light, final Point3D way) {
        final double facing = Math.max(0, way.normalize().getY());
        return new double[] {ambient.getRed() + light.getRed() * facing, ambient.getGreen() + light.getGreen() * facing,
                ambient.getBlue() + light.getBlue() * facing};
    }

    /** {@code a} lit by {@code by}, each channel, no brighter than white. */
    private static Color times(final Color a, final double[] by) {
        return new Color(Math.min(1, a.getRed() * by[0]), Math.min(1, a.getGreen() * by[1]),
                Math.min(1, a.getBlue() * by[2]), 1);
    }

    /** A material lit by itself alone, as {@code image} is. */
    private static PhongMaterial glowing(final WritableImage image) {
        final PhongMaterial material = new PhongMaterial(Color.BLACK);
        material.setSpecularColor(Color.BLACK);
        material.setSelfIlluminationMap(image);
        return material;
    }

    /** The sky, top to bottom: overhead, deepening to the haze at the horizon, the haze below. */
    private static WritableImage sky() {
        final int tall = 512;
        final WritableImage image = new WritableImage(4, tall);
        final PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < tall; y++) {
            // a sphere's texture runs from its top (0) to its bottom (1); the horizon at the middle
            final double up = 1 - 2.0 * (y + 0.5) / tall;
            final double share = up <= 0 ? 0 : Math.pow(Math.min(1, up / 0.6), 0.55);
            final Color colour = HAZY.interpolate(ZENITH, share);
            for (int x = 0; x < 4; x++) {
                writer.setColor(x, y, colour);
            }
        }
        return image;
    }

    /** The far ground, from its inner edge to its outer: {@code from}, easing into {@code to}. */
    private static WritableImage fading(final Color from, final Color to) {
        final int long_ = 256;
        final WritableImage image = new WritableImage(long_, 4);
        final PixelWriter writer = image.getPixelWriter();
        for (int x = 0; x < long_; x++) {
            // gently at first, from just what the ground is, the haze thickening, then all of it short of the sky
            final double t = Math.min(1, (x + 0.5) / long_ / 0.85);
            final Color colour = from.interpolate(to, t * t * (3 - 2 * t));
            for (int y = 0; y < 4; y++) {
                writer.setColor(x, y, colour);
            }
        }
        return image;
    }

    /** A flat ring, facing up, from {@code inner} round its middle out to {@code outer}: its texture along it, inner to outer. */
    private static MeshView ring(final double inner, final double outer) {
        final int sides = 180;
        final TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0.01f, 0.5f, 0.99f, 0.5f);
        for (int k = 0; k <= sides; k++) {
            final double a = 2 * Math.PI * k / sides;
            mesh.getPoints().addAll((float) (inner * Math.sin(a)), 0, (float) (inner * Math.cos(a)),
                    (float) (outer * Math.sin(a)), 0, (float) (outer * Math.cos(a)));
        }
        for (int k = 0; k < sides; k++) {
            final int p = 2 * k;
            final int q = 2 * (k + 1);
            // wound to face up, as the world's y does not
            mesh.getFaces().addAll(p, 0, q, 0, p + 1, 1, q, 0, q + 1, 1, p + 1, 1);
        }
        final MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        return view;
    }
}
