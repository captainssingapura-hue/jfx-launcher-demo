package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Weapon;

import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The people at the melon stall on the barge, drawn in the barge's own group, so they go where it
 * flies: the stallholder on a stool behind the table, a customer before it, and another sat on a
 * scooter parked to one side. They are only to be seen: nothing solid, and nothing harms them.
 *
 * <p>They do notice what happens to the fruit, from the simulation's events: a piece of fruit
 * struck and the stallholder shakes a fist; one bursting and everyone jumps, hands in the air, and the
 * stallholder is up off the stool.
 */
public final class Stall {

    /** How long a start lasts, and how long a fist is shaken, in seconds. */
    private static final double STARTLE = 1.3;
    private static final double SHAKE = 0.9;

    private final Set<Integer> fruit = new HashSet<>();
    private final Person keeper;
    private final List<Person> people = new ArrayList<>();
    private double time;

    /** At the stall on {@code course}'s barge, drawn into {@code world}. Nothing, if it has none. */
    public Stall(final World world, final Course course) {
        int barge = -1;
        for (int i = 0; i < course.pieces().size(); i++) {
            final Course.Piece piece = course.pieces().get(i);
            if (piece.kind() == Course.Kind.FRUIT) {
                fruit.add(i);
            } else if (piece.kind() == Course.Kind.PLATFORM && Course.Deck.of(piece.shape()).equals(Course.BARGE)) {
                barge = i;
            }
        }
        if (barge < 0) {
            keeper = null;
            return;
        }
        final Group deck = world.group(barge);
        // the barge's frame has its box's middle at the origin; its deck is that far below (y is down)
        final double floor = course.pieces().get(barge).shape().box().up();
        final Course.Section table = Course.TABLE;
        // the stall faces the back of the barge, toward the start: the stallholder between it and the console
        final double behind = table.along() + table.halfAlong() + 45;
        final double before = table.along() - table.halfAlong() - 55;

        final Cylinder stool = new Cylinder(16, 45);
        stool.setMaterial(BoxRig.matte("#7a5a3a"));
        stool.setTranslateX(-30);
        stool.setTranslateY(floor - 22.5);
        stool.setTranslateZ(behind);
        deck.getChildren().add(stool);
        keeper = new Person("#3a3f45", "#4a4f58", "#1c1c1c");
        keeper.at(deck, -30, floor, behind, 180).sitting(45);
        people.add(keeper);

        people.add(new Person("#18191b", "#18191b", "#121212").at(deck, 45, floor, before, 0));

        final Group scooter = scooter();
        scooter.setTranslateX(-310);
        scooter.setTranslateY(floor);
        scooter.setTranslateZ(before - 20);
        scooter.getTransforms().add(new Rotate(30, Rotate.Y_AXIS));
        deck.getChildren().add(scooter);
        people.add(new Person("#5a3d2b", "#2f4560", "#2a1d14").at(scooter, 0, 0, -8, 0).sitting(58).reaching());
    }

    /** Moves them on {@code seconds}, as they react to this frame's {@code events}. */
    public void show(final List<Event> events, final double seconds) {
        if (keeper == null) {
            return;
        }
        time += seconds;
        for (final Event event : events) {
            if (event instanceof Event.Broke broke && broke.kind() == Course.Kind.FRUIT) {
                people.forEach(person -> person.startled = STARTLE);
            } else if (event instanceof Event.Struck struck && fruit.contains(struck.piece())
                    && struck.by() != Weapon.BOMB) {
                keeper.shaking = SHAKE;
            }
        }
        for (final Person person : people) {
            person.show(time, seconds);
        }
    }

    /** A little scooter, its front along its z: body, seat, front, handlebar, wheels and a yellow plate. */
    private static Group scooter() {
        final PhongMaterial shell = BoxRig.matte("#2c3238");
        final PhongMaterial black = BoxRig.matte("#141414");
        final Group scooter = new Group();
        scooter.getChildren().add(box(34, 22, 100, 0, -26, 0, shell));
        scooter.getChildren().add(box(30, 10, 48, 0, -52, -18, black));
        scooter.getChildren().add(box(34, 70, 12, 0, -60, 46, shell));
        scooter.getChildren().add(box(62, 4, 4, 0, -98, 44, black));
        scooter.getChildren().add(box(16, 8, 1, 0, -40, -51, BoxRig.matte("#e8c21a")));
        for (final double z : new double[] {-38, 40}) {
            final Cylinder wheel = new Cylinder(17, 8);
            wheel.setMaterial(black);
            wheel.setTranslateY(-17);
            wheel.setTranslateZ(z);
            wheel.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
            scooter.getChildren().add(wheel);
        }
        return scooter;
    }

    private static Box box(final double w, final double h, final double d, final double x, final double y,
                           final double z, final PhongMaterial material) {
        final Box box = new Box(w, h, d);
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        box.setMaterial(material);
        return box;
    }

    /** One of them: boxes hung from hips, knees and shoulders, facing its own z. */
    private static final class Person {
        private final Group root = new Group();
        private final Group body = new Group();
        private final Rotate leftShoulder = new Rotate(0, Rotate.X_AXIS);
        private final Rotate rightShoulder = new Rotate(0, Rotate.X_AXIS);
        private final Rotate[] hips = {new Rotate(0, Rotate.X_AXIS), new Rotate(0, Rotate.X_AXIS)};
        private final Rotate[] knees = {new Rotate(0, Rotate.X_AXIS), new Rotate(0, Rotate.X_AXIS)};
        private double seat = -1;
        private boolean reaches;
        private double startled;
        private double shaking;

        Person(final String jacket, final String trousers, final String hair) {
            final PhongMaterial coat = BoxRig.matte(jacket);
            final PhongMaterial legs = BoxRig.matte(trousers);
            final PhongMaterial skin = BoxRig.matte("#e0b48f");
            final PhongMaterial shoes = BoxRig.matte("#1a1a1a");
            for (int side = 0; side < 2; side++) {
                final double x = side == 0 ? -10 : 10;
                final Group hip = new Group();
                hip.setTranslateX(x);
                hip.setTranslateY(-85);
                hip.getTransforms().add(hips[side]);
                hip.getChildren().add(box(15, 42, 17, 0, 21, 0, legs));
                final Group knee = new Group();
                knee.setTranslateY(42);
                knee.getTransforms().add(knees[side]);
                knee.getChildren().addAll(box(14, 40, 15, 0, 20, 0, legs), box(15, 6, 24, 0, 41, 4, shoes));
                hip.getChildren().add(knee);
                body.getChildren().add(hip);
            }
            body.getChildren().add(box(42, 62, 24, 0, -116, 0, coat));
            body.getChildren().add(box(12, 8, 12, 0, -150, 0, skin));
            body.getChildren().add(box(22, 25, 22, 0, -166, 0, skin));
            body.getChildren().add(box(23, 7, 23, 0, -180, -1, BoxRig.matte(hair)));
            for (int side = 0; side < 2; side++) {
                final Group shoulder = new Group();
                shoulder.setTranslateX(side == 0 ? -27 : 27);
                shoulder.setTranslateY(-142);
                shoulder.getTransforms().add(side == 0 ? leftShoulder : rightShoulder);
                shoulder.getChildren().addAll(box(11, 50, 11, 0, 25, 0, coat), box(9, 10, 9, 0, 54, 0, skin));
                body.getChildren().add(shoulder);
            }
            root.getChildren().add(body);
        }

        /** Put in {@code group} with its feet at ({@code x}, {@code y}, {@code z}), facing {@code yaw}. */
        Person at(final Group group, final double x, final double y, final double z, final double yaw) {
            root.setTranslateX(x);
            root.setTranslateY(y);
            root.setTranslateZ(z);
            root.getTransforms().add(new Rotate(yaw, Rotate.Y_AXIS));
            group.getChildren().add(root);
            return this;
        }

        /** Sat on something {@code high}. */
        Person sitting(final double high) {
            seat = high;
            return this;
        }

        /** Its hands on handlebars before it. */
        Person reaching() {
            reaches = true;
            return this;
        }

        void show(final double time, final double seconds) {
            startled = Math.max(0, startled - seconds);
            shaking = Math.max(0, shaking - seconds);
            final double fright = startled > 0 ? Math.min(1, (STARTLE - startled) / 0.12) * Math.min(1, startled / 0.3) : 0;
            // sat, or up off the seat in a fright
            final boolean sat = seat >= 0 && fright < 0.5;
            for (int side = 0; side < 2; side++) {
                hips[side].setAngle(sat ? 90 : 0);
                knees[side].setAngle(sat ? -90 : 0);
            }
            final double lift = sat ? 85 - seat : 0;
            final double hop = fright > 0 && startled > STARTLE - 0.45 ? 22 * Math.sin(Math.PI * (STARTLE - startled) / 0.45) : 0;
            body.setTranslateY(lift - hop);
            // the arms: at rest hanging, or on the handlebars, or flung up; and a fist shaken
            final double rest = reaches ? 70 : 6 * Math.sin(time * 1.3);
            final double up = 165;
            leftShoulder.setAngle(rest + (up - rest) * fright);
            rightShoulder.setAngle(shaking > 0 ? 150 + 18 * Math.sin(time * 26) : rest + (up - rest) * fright);
            // breathing, a little
            body.setScaleY(1 + 0.006 * Math.sin(time * 2.1));
        }
    }
}
