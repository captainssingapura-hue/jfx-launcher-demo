package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.List;

/**
 * What turns on an aircraft: a propeller, a rotor, a fan. Its pieces and skins, in the aircraft's
 * frame, turn about {@code axis} through {@code hub}, {@code rate} turns a second at full power.
 *
 * @param name   what it is
 * @param hub    a point on its axis
 * @param axis   which way its axis points, a unit
 * @param rate   how fast it turns, turns a second
 * @param pieces its pieces
 * @param skins  its skins (a spinner's cone)
 */
public record Spinner(String name, Point hub, Point axis, double rate, List<Piece> pieces, List<Skin> skins) {

    public Spinner {
        pieces = List.copyOf(pieces);
        skins = List.copyOf(skins);
    }

    /** The same, mounted ({@link Mount}): its hub put, its axis turned. */
    public Spinner placed(final Mount mount) {
        return new Spinner(name, mount.place(hub), mount.turn(axis), rate, pieces.stream().map(p -> p.placed(mount)).toList(),
                skins.stream().map(s -> s.placed(mount)).toList());
    }

    /** The same on the other side: across turned to the left, turning the other way. */
    public Spinner mirrored() {
        return new Spinner(name, Skin.mirror(hub), Skin.mirror(axis), -rate,
                pieces.stream().map(Piece::mirrored).toList(), skins.stream().map(Skin::mirrored).toList());
    }
}
