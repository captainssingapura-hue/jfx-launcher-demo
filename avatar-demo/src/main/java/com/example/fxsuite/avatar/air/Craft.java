package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.sim.Form;

import java.util.ArrayList;
import java.util.List;

/**
 * An aircraft as it is drawn, in its own frame (across to the right, along to its nose, up; its
 * origin its main body's middle): its bodies' skins, the solid parts fixed to them, what turns, and
 * what turns on a hinge ({@link Hinge}), at rest.
 * Put together a part at a time ({@link Builder}): a body mounted, the parts on it, a pair of them
 * either side.
 *
 * @param name     what it is
 * @param skins    its bodies' skins, mounted
 * @param pieces   its solid parts, mounted
 * @param spinners what turns, mounted
 * @param hinges    what turns on a hinge, mounted
 * @param panels    its skin's flat panels, dressed on its lines ({@link Lines}), mounted
 * @param skeletons its lines' skeletons ({@link Lines#skeleton}), mounted: what it is drawn by
 */
public record Craft(String name, List<Skin> skins, List<Piece> pieces, List<Spinner> spinners, List<Hinge> hinges,
                    List<Panel> panels, List<Form> skeletons) {

    public Craft {
        skins = List.copyOf(skins);
        pieces = List.copyOf(pieces);
        spinners = List.copyOf(spinners);
        hinges = List.copyOf(hinges);
        panels = List.copyOf(panels);
        skeletons = List.copyOf(skeletons);
    }

    /** No lines of its own. */
    public Craft(final String name, final List<Skin> skins, final List<Piece> pieces, final List<Spinner> spinners,
                 final List<Hinge> hinges) {
        this(name, skins, pieces, spinners, hinges, List.of(), List.of());
    }

    /** Nothing on a hinge. */
    public Craft(final String name, final List<Skin> skins, final List<Piece> pieces, final List<Spinner> spinners) {
        this(name, skins, pieces, spinners, List.of());
    }

    /** All its points, what turns too. */
    public List<Point> points() {
        final List<Point> all = new ArrayList<>();
        skins.forEach(s -> all.addAll(s.points()));
        pieces.forEach(p -> all.addAll(p.corners()));
        for (final Spinner spinner : spinners) {
            spinner.skins().forEach(s -> all.addAll(s.points()));
            spinner.pieces().forEach(p -> all.addAll(p.corners()));
        }
        hinges.forEach(h -> all.addAll(h.part().points()));
        panels.forEach(p -> all.addAll(p.corners()));
        return all;
    }

    /** How far it reaches each way. */
    public Extent extent() {
        return Extent.of(points());
    }

    /** All of it, mounted ({@link Mount}): a part made whole in a frame of its own (an engine), fixed on another. */
    public Craft placed(final Mount mount) {
        return new Craft(name, skins.stream().map(s -> s.placed(mount)).toList(),
                pieces.stream().map(p -> p.placed(mount)).toList(), spinners.stream().map(s -> s.placed(mount)).toList(),
                hinges.stream().map(h -> h.placed(mount)).toList(), panels.stream().map(p -> p.placed(mount)).toList(),
                skeletons.stream().map(f -> placed(f, mount)).toList());
    }

    /** All of it on the other side: across turned to the left, what turns turning the other way. */
    public Craft mirrored() {
        return new Craft(name, skins.stream().map(Skin::mirrored).toList(), pieces.stream().map(Piece::mirrored).toList(),
                spinners.stream().map(Spinner::mirrored).toList(), hinges.stream().map(Hinge::mirrored).toList(),
                panels.stream().map(Panel::mirrored).toList(), skeletons.stream().map(Craft::mirrored).toList());
    }

    /**
     * All of it on one hinge, turned by {@code control} about {@code axis} through {@code pivot}, from
     * {@code least} to {@code most} degrees: a duct that tilts, a turret that traverses.
     */
    public Craft hinged(final String control, final Point pivot, final Point axis, final double least,
                        final double most) {
        return new Craft(name, List.of(), List.of(), List.of(), List.of(new Hinge(control, pivot, Piece.unit(axis), 1, least,
                most, this)));
    }

    /** A skeleton, mounted: each vertex put where the mount puts it. */
    static Form placed(final Form form, final Mount mount) {
        return new Form(form.name(), form.vertices().stream().map(v -> {
            final Point p = mount.place(new Point(v.across(), v.along(), v.up()));
            return new Form.Vertex(v.name(), p.across(), p.along(), p.up());
        }).toList(), form.edges());
    }

    /** A skeleton on the other side: across turned to the left, its right vertices named its left and its left its right. */
    static Form mirrored(final Form form) {
        return new Form(form.name(), form.vertices().stream().map(v -> new Form.Vertex(otherSide(v.name()), -v.across(),
                v.along(), v.up())).toList(), form.edges());
    }

    /** {@code name} with each word "right" in it made "left", and each "left" made "right". */
    static String otherSide(final String name) {
        final String[] words = name.split(" ", -1);
        for (int k = 0; k < words.length; k++) {
            words[k] = words[k].equals("right") ? "left" : words[k].equals("left") ? "right" : words[k];
        }
        return String.join(" ", words);
    }

    /** Puts a craft together. */
    public static Builder named(final String name) {
        return new Builder(name);
    }

    /** A craft a part at a time. */
    public static final class Builder {
        private final String name;
        private final List<Skin> skins = new ArrayList<>();
        private final List<Piece> pieces = new ArrayList<>();
        private final List<Spinner> spinners = new ArrayList<>();
        private final List<Hinge> hinges = new ArrayList<>();
        private final List<Panel> panels = new ArrayList<>();
        private final List<Form> skeletons = new ArrayList<>();

        private Builder(final String name) {
            this.name = name;
        }

        /** {@code body}, mounted. */
        public Builder body(final Body body, final Mount mount) {
            skins.add(body.skin().placed(mount));
            return this;
        }

        /** {@code body}, mounted, and its mirror image on the other side. */
        public Builder bodies(final Body body, final Mount mount) {
            final Skin skin = body.skin().placed(mount);
            skins.add(skin);
            skins.add(skin.mirrored());
            return this;
        }

        /** A skin of no body's (a duct, swept round its axis), mounted. */
        public Builder skin(final Skin skin, final Mount mount) {
            skins.add(skin.placed(mount));
            return this;
        }

        /** {@code lines}' skeleton, and its skin dressed on it as {@code dress} says, mounted. */
        public Builder lines(final Lines lines, final Lines.Dress dress, final Mount mount) {
            skeletons.add(placed(lines.skeleton(), mount));
            lines.panels(dress).forEach(p -> panels.add(p.placed(mount)));
            return this;
        }

        /** The same, and its mirror image on the other side: a pair of wings. */
        public Builder pairOfLines(final Lines lines, final Lines.Dress dress, final Mount mount) {
            final Form skeleton = placed(lines.skeleton(), mount);
            skeletons.add(skeleton);
            skeletons.add(mirrored(skeleton));
            lines.panels(dress).forEach(p -> {
                final Panel placed = p.placed(mount);
                panels.add(placed);
                panels.add(placed.mirrored());
            });
            return this;
        }

        /** {@code whole}, made in a frame of its own (an engine), mounted. */
        public Builder with(final Craft whole, final Mount mount) {
            final Craft placed = whole.placed(mount);
            skins.addAll(placed.skins());
            pieces.addAll(placed.pieces());
            spinners.addAll(placed.spinners());
            hinges.addAll(placed.hinges());
            panels.addAll(placed.panels());
            skeletons.addAll(placed.skeletons());
            return this;
        }

        /** {@code whole}, mounted, and its mirror image on the other side: a pair of engines. */
        public Builder withPair(final Craft whole, final Mount mount) {
            final Craft placed = whole.placed(mount);
            for (final Craft side : List.of(placed, placed.mirrored())) {
                skins.addAll(side.skins());
                pieces.addAll(side.pieces());
                spinners.addAll(side.spinners());
                hinges.addAll(side.hinges());
                panels.addAll(side.panels());
                skeletons.addAll(side.skeletons());
            }
            return this;
        }

        /** {@code parts}, mounted. */
        public Builder parts(final List<Piece> parts, final Mount mount) {
            parts.forEach(p -> pieces.add(p.placed(mount)));
            return this;
        }

        /** {@code part}, as it is. */
        public Builder part(final Piece part) {
            pieces.add(part);
            return this;
        }

        /** {@code parts}, mounted, and their mirror image on the other side: a pair of wings, of struts. */
        public Builder pair(final List<Piece> parts, final Mount mount) {
            parts.forEach(p -> {
                final Piece placed = p.placed(mount);
                pieces.add(placed);
                pieces.add(placed.mirrored());
            });
            return this;
        }

        /** {@code part}, and its mirror image. */
        public Builder pair(final Piece part) {
            pieces.add(part);
            pieces.add(part.mirrored());
            return this;
        }

        /**
         * What turns, made about its own hub at the origin and its axis +up ({@link Parts#rotor}), and
         * mounted: its axis turned as the mount turns it.
         */
        public Builder spinner(final String name, final List<Piece> parts, final List<Body> bodies, final Mount mount,
                               final double rate) {
            spinners.add(spin(name, parts, bodies, mount, rate));
            return this;
        }

        /** The same, and its mirror image, turning the other way. */
        public Builder spinners(final String name, final List<Piece> parts, final List<Body> bodies, final Mount mount,
                                final double rate) {
            final Spinner spinner = spin(name, parts, bodies, mount, rate);
            spinners.add(spinner);
            spinners.add(spinner.mirrored());
            return this;
        }

        private static Spinner spin(final String name, final List<Piece> parts, final List<Body> bodies,
                                    final Mount mount, final double rate) {
            return new Spinner(name, mount.at(), mount.turn(new Point(0, 0, 1)), rate,
                    parts.stream().map(p -> p.placed(mount)).toList(),
                    bodies.stream().map(b -> b.skin().placed(mount)).toList());
        }

        public Craft build() {
            return new Craft(name, skins, pieces, spinners, hinges, panels, skeletons);
        }
    }
}
