package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A loose thing ({@link Loose}): a piece of furniture, a piece of fruit. Piece {@code piece} of the
 * course, where it now is ({@code place}: its underside at the height, heading its way) and how
 * fast it is going ({@code vx}, {@code vz} across the ground, {@code vy} up). Pure state, stepped
 * with the rest of the world ({@link Play}).
 *
 * <ul>
 *   <li><b>It stands on whatever is under it</b> ({@link #support}): the ground, a block, a
 *       platform's deck, another loose thing. Resting on something that moves, it goes where
 *       that goes, turned as it turns ({@link #carried}): the fruit on the table, the table on
 *       the barge.</li>
 *   <li><b>Pushed</b>, it slides along the ground ({@link #pushed}) and comes to a stop; it stops
 *       short of what is in its way. Pushed off an edge, or with what held it up gone, it falls.</li>
 *   <li><b>Landing</b> harder than it can take ({@link Loose#landing}), it breaks
 *       ({@link Weapon#FALL}): a watermelon off the table is fine, off the barge high up it is not.
 *       Struck like any other piece, it breaks as its toughness says ({@link Damage}).</li>
 * </ul>
 */
public record Prop(int piece, Loose thing, Place place, double vx, double vz, double vy) {

    /** How briskly it slides to a stop on what it stands on. */
    public static final double FRICTION = 5;

    /** Something loose, at rest where it was put. */
    public Prop(final int piece, final Loose thing, final Place place) {
        this(piece, thing, place, 0, 0, 0);
    }

    /** The loose things among {@code damage}'s pieces, each at rest where it was put. */
    public static List<Prop> of(final Damage damage) {
        final List<Prop> props = new ArrayList<>();
        for (int piece = 0; piece < damage.size(); piece++) {
            final Course.Piece what = damage.piece(piece);
            if (what.kind().loose()) {
                props.add(new Prop(piece, what.loose(), what.place()));
            }
        }
        return props;
    }

    /** Its solid box where it now is. */
    public Block box() {
        return thing.at(0, 0, 0).placed(place);
    }

    /** The same, at {@code to}, going as it was. */
    public Prop at(final Place to) {
        return new Prop(piece, thing, to, vx, vz, vy);
    }

    /** The same, carried by what it rests on as that moved from {@code from} to {@code to}. */
    public Prop carried(final Place from, final Place to) {
        final Vec3 at = from.carry(new Vec3(place.x(), -place.height(), place.z()), to);
        return at(new Place(at.x(), at.z(), -at.y(), Num.wrap(place.yaw() + to.yaw() - from.yaw())));
    }

    /** The same, pushed: sliding across the ground at {@code vx}, {@code vz}. */
    public Prop pushed(final double toX, final double toZ) {
        return new Prop(piece, thing, place, toX, toZ, vy);
    }

    /** How fast it is sliding across the ground. */
    public double sliding() {
        return Math.hypot(vx, vz);
    }

    /**
     * The highest thing under it where it is, among {@code world}, but for {@code ignore} (itself,
     * and what rests on it): what it rests on, or would land on. The ground if nothing else.
     */
    public Lift.Support support(final Obstacles world, final Collection<Integer> ignore) {
        final Block box = box();
        final double underside = place.height();
        final Block below = new Block(box.x(), box.z(), box.halfWidth(), box.halfDepth(), box.yaw(), -1, underside + 0.5);
        Lift.Support best = new Lift.Support(-1, 0);
        for (int i = 0; i < world.blocks().size(); i++) {
            final int other = world.pieces().get(i);
            final Block block = world.blocks().get(i);
            if (other != piece && !ignore.contains(other) && block.top() > best.top()
                    && block.top() <= underside + 0.5 && below.overlaps(block)) {
                best = new Lift.Support(other, block.top());
            }
        }
        return best;
    }

    /** Whether it rests on {@code under}: its underside on it, and not going up. */
    public boolean resting(final Lift.Support under) {
        return Math.abs(place.height() - under.top()) < 0.5 && vy <= 0;
    }

    /** A loose thing a step on, and the blow of its landing, if it landed too hard. */
    public record Stepped(Prop prop, List<Blow> blows) {

        public Stepped {
            blows = List.copyOf(blows);
        }
    }

    /**
     * {@code seconds} on, among {@code world}, but for {@code ignore} (what rests on it): resting
     * and sliding to a stop, or falling and landing; and going across the ground only as far as is
     * clear.
     */
    public Stepped step(final double seconds, final Obstacles world, final Collection<Integer> ignore) {
        final Lift.Support under = support(world, ignore);
        double height = place.height();
        double up = vy;
        double x = vx;
        double z = vz;
        final List<Blow> blows = new ArrayList<>();
        if (resting(under)) {
            up = 0;
            final double slowed = Math.exp(-FRICTION * seconds);
            x = Math.abs(vx * slowed) < 1 ? 0 : vx * slowed;
            z = Math.abs(vz * slowed) < 1 ? 0 : vz * slowed;
        } else {
            final double dropping = vy - Body.GRAVITY * seconds;
            height = place.height() + (vy + dropping) / 2 * seconds;
            up = dropping;
            if (height <= under.top()) {
                // landed: whole, or broken by it
                height = under.top();
                if (-dropping > thing.landing()) {
                    final Vec3 at = new Vec3(place.x(), -height, place.z());
                    blows.add(new Blow(at, new Vec3(0, -1, 0), under.piece() < 0 ? Surface.GROUND : Surface.BLOCK,
                            Weapon.FALL, piece));
                }
                up = 0;
            }
        }
        // across the ground, if that is clear
        final Place across = new Place(place.x() + (vx + x) / 2 * seconds, place.z() + (vz + z) / 2 * seconds, height,
                place.yaw());
        if ((x != 0 || z != 0 || vx != 0 || vz != 0) && clear(across, world, ignore)) {
            return new Stepped(new Prop(piece, thing, across, x, z, up), blows);
        }
        // blocked: as far as is clear, found by halving the way, and stopped there
        final double dx = across.x() - place.x();
        final double dz = across.z() - place.z();
        double clearShare = 0;
        double blockedShare = 1;
        for (int i = 0; i < 8 && (dx != 0 || dz != 0); i++) {
            final double mid = (clearShare + blockedShare) / 2;
            if (clear(new Place(place.x() + dx * mid, place.z() + dz * mid, height, place.yaw()), world, ignore)) {
                clearShare = mid;
            } else {
                blockedShare = mid;
            }
        }
        final Place still = new Place(place.x() + dx * clearShare, place.z() + dz * clearShare, height, place.yaw());
        return new Stepped(new Prop(piece, thing, still, 0, 0, up), blows);
    }

    /** Whether at {@code at} it is clear of everything solid in {@code world} but itself and {@code ignore}. */
    public boolean clear(final Place at, final Obstacles world, final Collection<Integer> ignore) {
        final Block box = thing.at(0, 0, 0).placed(at);
        for (int i = 0; i < world.blocks().size(); i++) {
            final int other = world.pieces().get(i);
            if (other != piece && !ignore.contains(other) && box.overlaps(world.blocks().get(i))) {
                return false;
            }
        }
        return true;
    }
}
