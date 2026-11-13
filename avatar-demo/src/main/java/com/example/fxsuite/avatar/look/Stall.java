package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Person;
import com.example.fxsuite.avatar.sim.Weapon;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The people ({@link Person}), drawn where the simulation has them, between its last two steps
 * as the avatar is: standing or sat. Destroyed, they are blown apart ({@link Wreckage#blowApart}),
 * in their own colours, and are seen no more. Sat, they sit on top of what they sit on (the
 * stallholder's stool, the scooter: furniture, drawn with the world).
 *
 * <p>They react to what happens, from the simulation's events: a piece of fruit struck and the
 * stallholder shakes a fist; one bursting and everyone still up jumps, hands in the air; anyone
 * hit or shoved staggers, arms out.
 */
public final class Stall {

    /** How long a start lasts, how long a fist is shaken, and how long a stagger, in seconds. */
    private static final double STARTLE = 1.3;
    private static final double SHAKE = 0.9;
    private static final double STAGGER = 0.6;

    private final Group root = new Group();
    private final Set<Integer> fruit = new HashSet<>();
    /** Each person's figure, by their number. */
    private final List<Figure> figures = new ArrayList<>();
    private final Wreckage wreckage;
    private double time;

    /** For the people of {@code course}'s cast; blown apart into {@code wreckage}. */
    public Stall(final Course course, final Wreckage wreckage) {
        this.wreckage = wreckage;
        for (int i = 0; i < course.pieces().size(); i++) {
            if (course.pieces().get(i).kind() == Course.Kind.FRUIT) {
                fruit.add(i);
            }
        }
        for (final Course.Cast cast : Course.CAST) {
            final Figure figure = switch (cast.role()) {
                case STALLHOLDER -> new Figure("#3a3f45", "#4a4f58", "#1c1c1c", false);
                case CUSTOMER -> new Figure("#18191b", "#18191b", "#121212", false);
                case RIDER -> new Figure("#5a3d2b", "#2f4560", "#2a1d14", true);
            };
            figures.add(figure);
            root.getChildren().add(figure.root);
        }
    }

    public Node node() {
        return root;
    }

    /**
     * Shows the people as they are, {@code now}, part way ({@code share}) from as they were a step
     * before ({@code before}), reacting to this frame's {@code events}, {@code seconds} on.
     */
    public void show(final List<Person> now, final List<Person> before, final double share, final List<Event> events,
                     final double seconds) {
        time += seconds;
        for (final Event event : events) {
            if (event instanceof Event.Broke broke && broke.kind() == Course.Kind.FRUIT) {
                figures.forEach(figure -> figure.startled = STARTLE);
            } else if (event instanceof Event.Struck struck && Person.is(struck.piece())) {
                staggers(Person.which(struck.piece()));
            } else if (event instanceof Event.Struck struck && fruit.contains(struck.piece())
                    && struck.by() != Weapon.BOMB) {
                for (int k = 0; k < Math.min(figures.size(), now.size()); k++) {
                    if (now.get(k).role() == Person.Role.STALLHOLDER) {
                        figures.get(k).shaking = SHAKE;
                    }
                }
            } else if (event instanceof Event.Shoved shoved) {
                staggers(shoved.person());
            } else if (event instanceof Event.Destroyed destroyed && destroyed.person() < figures.size()) {
                wreckage.blowApart(destroyed.at(), figures.get(destroyed.person()).colours);
            }
        }
        for (int k = 0; k < figures.size(); k++) {
            final Figure figure = figures.get(k);
            if (k >= now.size()) {
                figure.root.setVisible(false);
                continue;
            }
            final Person person = now.get(k);
            final Body seen = k < before.size() ? person.body().between(before.get(k).body(), share) : person.body();
            figure.place(seen, person.sitting(), person.down());
            figure.show(time, seconds);
        }
    }

    private void staggers(final int person) {
        if (person >= 0 && person < figures.size()) {
            figures.get(person).staggering = STAGGER;
        }
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

    /** One person as drawn: boxes hung from hips, knees and shoulders, facing its own z, its feet at its origin. */
    private static final class Figure {
        private final Group root = new Group();
        private final Group body = new Group();
        private final Rotate yaw = new Rotate(0, Rotate.Y_AXIS);
        private final Rotate leftShoulder = new Rotate(0, Rotate.X_AXIS);
        private final Rotate rightShoulder = new Rotate(0, Rotate.X_AXIS);
        private final Rotate[] hips = {new Rotate(0, Rotate.X_AXIS), new Rotate(0, Rotate.X_AXIS)};
        private final Rotate[] knees = {new Rotate(0, Rotate.X_AXIS), new Rotate(0, Rotate.X_AXIS)};
        private final boolean reaches;
        private boolean sitting;
        private double startled;
        private double shaking;
        private double staggering;
        /** What they are made of, to be blown apart in: coat, trousers, skin, hair. */
        private final List<PhongMaterial> colours;

        /** In these colours, their hands before them when sat if {@code reaches}: on handlebars. */
        Figure(final String jacket, final String trousers, final String hair, final boolean reaches) {
            this.reaches = reaches;
            final PhongMaterial coat = BoxRig.matte(jacket);
            final PhongMaterial legs = BoxRig.matte(trousers);
            final PhongMaterial skin = BoxRig.matte("#e0b48f");
            final PhongMaterial shoes = BoxRig.matte("#1a1a1a");
            colours = List.of(coat, legs, skin, BoxRig.matte(hair));
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
            // turned to face its way
            root.getTransforms().add(yaw);
            root.getChildren().add(body);
        }

        /** Where {@code seen} is, facing its way, sat or standing; not to be seen at all if {@code gone}. */
        void place(final Body seen, final boolean sat, final boolean gone) {
            root.setVisible(!gone);
            root.setTranslateX(seen.position().x());
            root.setTranslateY(seen.position().y());
            root.setTranslateZ(seen.position().z());
            yaw.setAngle(seen.yaw());
            sitting = sat;
        }

        void show(final double time, final double seconds) {
            startled = Math.max(0, startled - seconds);
            shaking = Math.max(0, shaking - seconds);
            staggering = Math.max(0, staggering - seconds);
            final double fright = startled > 0 ? Math.min(1, (STARTLE - startled) / 0.12) * Math.min(1, startled / 0.3) : 0;
            // sat, their feet where the seat is, or up on it in a fright
            final boolean sat = sitting && fright < 0.5;
            for (int side = 0; side < 2; side++) {
                hips[side].setAngle(sat ? 90 : 0);
                knees[side].setAngle(sat ? -90 : 0);
            }
            final double lift = sat ? 85 : 0;
            final double hop = fright > 0 && startled > STARTLE - 0.45 ? 22 * Math.sin(Math.PI * (STARTLE - startled) / 0.45) : 0;
            body.setTranslateY(lift - hop);
            // the arms: at rest hanging, or on the handlebars, or flung up; out to the sides
            // staggering; a fist shaken
            final double rest = reaches && sat ? 70 : 6 * Math.sin(time * 1.3);
            final double up = 165;
            final double stagger = staggering > 0 ? 55 + 25 * Math.sin(time * 18) : 0;
            final double left = rest + (up - rest) * fright + stagger;
            leftShoulder.setAngle(left);
            rightShoulder.setAngle(shaking > 0 ? 150 + 18 * Math.sin(time * 26) : left);
            // breathing, a little
            body.setScaleY(1 + 0.006 * Math.sin(time * 2.1));
        }
    }
}
