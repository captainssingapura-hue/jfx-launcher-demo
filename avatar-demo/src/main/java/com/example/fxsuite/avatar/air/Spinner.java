package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.sim.Form;

import java.util.List;

/**
 * What turns on an aircraft: a propeller, a rotor, a fan. Its pieces, skins and panels, in the
 * aircraft's frame, turn about {@code axis} through {@code hub}, {@code rate} turns a second at full
 * power; with them its lines' skeletons, if it is drawn by lines (a propeller's blades).
 *
 * @param name      what it is
 * @param hub       a point on its axis
 * @param axis      which way its axis points, a unit
 * @param rate      how fast it turns, turns a second
 * @param pieces    its pieces
 * @param skins     its skins (a spinner's cone)
 * @param panels    its panels, dressed on its lines ({@link Lines})
 * @param skeletons its lines' skeletons
 */
public record Spinner(String name, Point hub, Point axis, double rate, List<Piece> pieces, List<Skin> skins,
                      List<Panel> panels, List<Form> skeletons) {

    public Spinner {
        pieces = List.copyOf(pieces);
        skins = List.copyOf(skins);
        panels = List.copyOf(panels);
        skeletons = List.copyOf(skeletons);
    }

    /** Of pieces and skins alone, no lines. */
    public Spinner(final String name, final Point hub, final Point axis, final double rate, final List<Piece> pieces,
                   final List<Skin> skins) {
        this(name, hub, axis, rate, pieces, skins, List.of(), List.of());
    }

    /** All of {@code part} (its pieces, skins, panels and skeletons; not what turns or is hinged on it) turning so. */
    public static Spinner of(final String name, final Point hub, final Point axis, final double rate, final Craft part) {
        return new Spinner(name, hub, axis, rate, part.pieces(), part.skins(), part.panels(), part.skeletons());
    }

    /** The same, mounted ({@link Mount}): its hub put, its axis turned. */
    public Spinner placed(final Mount mount) {
        return new Spinner(name, mount.place(hub), mount.turn(axis), rate, pieces.stream().map(p -> p.placed(mount)).toList(),
                skins.stream().map(s -> s.placed(mount)).toList(), panels.stream().map(p -> p.placed(mount)).toList(),
                skeletons.stream().map(f -> Craft.placed(f, mount)).toList());
    }

    /** The same on the other side: across turned to the left, turning the other way. */
    public Spinner mirrored() {
        return new Spinner(name, Skin.mirror(hub), Skin.mirror(axis), -rate,
                pieces.stream().map(Piece::mirrored).toList(), skins.stream().map(Skin::mirrored).toList(),
                panels.stream().map(Panel::mirrored).toList(), skeletons.stream().map(Craft::mirrored).toList());
    }
}
