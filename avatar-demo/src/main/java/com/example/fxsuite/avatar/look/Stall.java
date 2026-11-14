package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Event;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Person;
import com.example.fxsuite.avatar.sim.Vec3;
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
 *
 * <p>On the meeting platform, a stand-up runs: each developer has the floor in turn, talking with
 * their hands, and the others turn to them, the scrum master (orange hoodie, red cap, a badge on
 * a lanyard, a clipboard) most of all.
 */
public final class Stall {

    /** How long a start lasts, how long a fist is shaken, and how long a stagger, in seconds. */
    private static final double STARTLE = 1.3;
    private static final double SHAKE = 0.9;
    private static final double STAGGER = 0.6;
    /** How long each developer has the floor at the stand-up, in seconds. */
    private static final double TURN = 5;
    /** The developers' hoodies and hair. */
    private static final String[] HOODIES = {"#4a5d7a", "#6b4a7a", "#3f6b52", "#7a5a3f", "#555b63"};
    private static final String[] HAIR = {"#1c1c1c", "#6b4a2b", "#c9a14a", "#2a1d14", "#8a8a8a"};

    private final Group root = new Group();
    private final Set<Integer> fruit = new HashSet<>();
    /** Each person's figure, by their number. */
    private final List<Figure> figures = new ArrayList<>();
    private final Wreckage wreckage;
    private double time;
    /** How many developers have figures so far. */
    private int developers;

    /** For the people of {@code course}'s cast; blown apart into {@code wreckage}. */
    public Stall(final Course course, final Wreckage wreckage) {
        this.wreckage = wreckage;
        for (int i = 0; i < course.pieces().size(); i++) {
            if (course.pieces().get(i).kind() == Course.Kind.FRUIT) {
                fruit.add(i);
            }
        }
    }

    /** A figure for {@code person}, in their role's colours: the {@code nth} developer in their own hoodie. */
    private static Figure figureFor(final Person person, final int nth) {
        return switch (person.role()) {
            case STALLHOLDER -> new Figure("#3a3f45", "#4a4f58", "#1c1c1c", false);
            case CUSTOMER -> new Figure("#18191b", "#18191b", "#121212", false);
            case RIDER -> new Figure("#5a3d2b", "#2f4560", "#2a1d14", true);
            case SCRUM_MASTER -> new Figure("#f07a1a", "#2b3a55", "#3b2a1f", false).master();
            case DEVELOPER -> new Figure(HOODIES[nth % HOODIES.length], "#2e3440", HAIR[nth % HAIR.length], false);
        };
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
        while (figures.size() < now.size()) {
            final Person person = now.get(figures.size());
            final Figure figure = figureFor(person, person.role() == Person.Role.DEVELOPER ? developers++ : 0);
            figures.add(figure);
            root.getChildren().add(figure.root);
        }
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
        standUp(now, before, share, seconds);
    }

    /**
     * The stand-up, wherever there is a team: each developer still up has the floor in turn, for
     * {@link #TURN} seconds, talking with their hands; the others, and the scrum master most of all,
     * turn to whoever is talking.
     */
    private void standUp(final List<Person> now, final List<Person> before, final double share, final double seconds) {
        final List<Integer> team = new ArrayList<>();
        final List<Integer> listening = new ArrayList<>();
        for (int k = 0; k < Math.min(figures.size(), now.size()); k++) {
            final Person person = now.get(k);
            if (person.down()) {
                continue;
            }
            if (person.role() == Person.Role.DEVELOPER) {
                team.add(k);
                listening.add(k);
            } else if (person.role() == Person.Role.SCRUM_MASTER) {
                listening.add(k);
            }
        }
        if (team.isEmpty()) {
            listening.forEach(k -> figures.get(k).listen(null, 0, seconds));
            return;
        }
        final int speaker = team.get((int) (time / TURN) % team.size());
        final Body talking = figures.get(speaker).seen;
        for (final int k : listening) {
            final Figure figure = figures.get(k);
            figure.speaking = k == speaker;
            figure.listen(k == speaker ? null : talking.position(), figure.master ? 85 : 45, seconds);
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
        /** Turned, from the waist, to whoever they listen to. */
        private final Rotate look = new Rotate(0, Rotate.Y_AXIS);
        private final boolean reaches;
        private boolean sitting;
        /** Where they were last placed; whether they have the floor; whether they run the stand-up. */
        private Body seen;
        private boolean speaking;
        private boolean master;
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
            // turned to face its way; and from the waist up, to whoever they listen to
            root.getTransforms().add(yaw);
            body.getTransforms().add(look);
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
            this.seen = seen;
        }

        /**
         * Dressed to run the stand-up, and to be picked out at a glance: a red cap, a lanyard and its
         * badge, and a clipboard held before them.
         */
        Figure master() {
            master = true;
            final PhongMaterial red = BoxRig.matte("#c8102e");
            body.getChildren().addAll(box(24, 8, 24, 0, -186, -1, red), box(22, 2, 14, 0, -182, 17, red),
                    box(9, 12, 1, 8, -102, 12.6, BoxRig.matte("#ffffff")), box(2, 26, 1, 8, -121, 12.4,
                            BoxRig.matte("#1d4ed8")),
                    box(24, 30, 2, 0, -108, 30, BoxRig.matte("#8a6440")),
                    box(20, 24, 0.6, 0, -108, 31.3, BoxRig.matte("#f5f6f2")));
            return this;
        }

        /** Turned toward {@code who} (or back ahead, if null), no further than {@code most} degrees, easing round. */
        void listen(final Vec3 who, final double most, final double seconds) {
            double wanted = 0;
            if (who != null && seen != null) {
                final double toward = Math.toDegrees(Math.atan2(who.x() - seen.position().x(), who.z() - seen.position().z()));
                wanted = Num.clamp(Num.wrap(toward - seen.yaw()), -most, most);
            }
            look.setAngle(Num.ease(look.getAngle(), wanted, 4, seconds));
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
            final double rest = reaches && sat ? 70 : master ? 42 : 6 * Math.sin(time * 1.3);
            final double up = 165;
            final double stagger = staggering > 0 ? 55 + 25 * Math.sin(time * 18) : 0;
            final double left = rest + (up - rest) * fright + stagger;
            // talking, their hands going as they do
            final boolean talking = speaking && fright == 0 && staggering == 0;
            leftShoulder.setAngle(talking ? 28 + 12 * Math.sin(time * 3.3) : left);
            rightShoulder.setAngle(shaking > 0 ? 150 + 18 * Math.sin(time * 26)
                    : talking ? 58 + 26 * Math.sin(time * 4.7) : left);
            // breathing, a little
            body.setScaleY(1 + 0.006 * Math.sin(time * 2.1));
        }
    }
}
