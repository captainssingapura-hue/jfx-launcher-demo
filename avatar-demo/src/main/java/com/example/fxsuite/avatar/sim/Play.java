package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Everything that moves: the avatar, what its weapons have done to the world, what moves of
 * itself (the airships and their bombs, {@link Sky}), and the platforms moved from their
 * consoles ({@link Lift}), stepped together. Pure state; what happened comes out of each step
 * beside it.
 *
 * <p>A step goes in five phases:
 * <ol>
 *   <li><b>The controls change hands:</b> taken at a platform's console, or let go
 *       ({@link Action#TAKE}). At them, the keys that would move the avatar fly the
 *       platform instead, and the avatar stands at the console.</li>
 *   <li><b>The world moves on:</b> the airships go on along their routes, the platforms as their
 *       controls say, and whoever stands on one is carried with it, turned as it turns.</li>
 *   <li><b>Act:</b> the avatar does what it was asked, in order ({@link Controls#actions}), moves
 *       on among what still stands, where it now is, and its weapon deals its {@link Blow}s; the
 *       bombs fall, and those that meet something go off, dealing theirs. Intents, not yet
 *       anything that happened.</li>
 *   <li><b>Resolve:</b> the world takes the blows, in the order they were dealt ({@link Damage}).</li>
 *   <li><b>Report:</b> what happened goes out: the controls changing hands, the avatar's own
 *       events, the bombs', then what came of each blow (struck, with what the piece has left;
 *       what broke).</li>
 * </ol>
 * The blows never leave the step: they follow from the state and the inputs, and only what came
 * of them is an output.
 */
public record Play(Avatar avatar, Damage damage, Sky sky, List<Lift> lifts, List<Person> people, List<Prop> props,
                   List<Turret> turrets) {

    /**
     * Which rules the world steps by. Raise it whenever a change would step the same inputs to a
     * different world, so a recording made under the old rules says so rather than going astray.
     */
    public static final int RULES = 14;

    public Play {
        lifts = List.copyOf(lifts);
        people = List.copyOf(people);
        props = List.copyOf(props);
        turrets = List.copyOf(turrets);
    }

    /** The avatar, the world, the sky, the platforms, the people and the loose things, with the guns mounted as they were built. */
    public Play(final Avatar avatar, final Damage damage, final Sky sky, final List<Lift> lifts,
                final List<Person> people, final List<Prop> props) {
        this(avatar, damage, sky, lifts, people, props, Turret.of(damage));
    }

    /** The avatar, the world, the sky, the platforms and the people, with the loose things where they were put. */
    public Play(final Avatar avatar, final Damage damage, final Sky sky, final List<Lift> lifts,
                final List<Person> people) {
        this(avatar, damage, sky, lifts, people, Prop.of(damage));
    }

    /** The avatar, the world, the sky and the platforms, with the people where they were put, on the platforms as they are. */
    public Play(final Avatar avatar, final Damage damage, final Sky sky, final List<Lift> lifts) {
        this(avatar, damage, sky, lifts, Person.of(damage, lifts));
    }

    /** The avatar and the world, with the sky as it starts and the platforms where they were built. */
    public Play(final Avatar avatar, final Damage damage) {
        this(avatar, damage, Sky.START);
    }

    /** The avatar, the world and the sky, with the platforms where they were built. */
    public Play(final Avatar avatar, final Damage damage, final Sky sky) {
        this(avatar, damage, sky, Lift.of(damage));
    }

    /** The world at the start: the avatar standing at the start, with the pack on or not, and the course unharmed. */
    public static Play start(final boolean pack) {
        return start(Demo.PLAYGROUND, pack);
    }

    /** The world of {@code demo} at the start: the avatar standing at the start, facing its way, with the pack on or not. */
    public static Play start(final Demo demo, final boolean pack) {
        return new Play(new Avatar(Body.standing(Vec3.ZERO, demo.startYaw()), pack), Damage.of(new Course(demo)));
    }

    /**
     * The whole world stirred into one number, to the bit: two worlds with the same checksum are,
     * as near as matters, the same. The avatar, the sky, the platforms, the people and the loose
     * things are all records, so their text is exact and complete.
     */
    public long checksum() {
        long sum = damage.checksum();
        // the guns mounted, if there are any: a world without them sums as it always has
        final String text = avatar.toString() + sky.toString() + lifts.toString() + people.toString()
                + props.toString() + (turrets.isEmpty() ? "" : turrets.toString());
        for (int i = 0; i < text.length(); i++) {
            sum = Num.mix(sum, text.charAt(i));
        }
        return sum;
    }

    /** The world after a step, and what happened in it. */
    public record Stepped(Play play, List<Event> events) {

        public Stepped {
            events = List.copyOf(events);
        }
    }

    /** What still stands, where it now is: to move among, to shoot at, to fall onto. */
    public Obstacles obstacles() {
        return obstacles(sky.time(), lifts, props);
    }

    /** The airships still flying, each where it now is. */
    public List<Course.Piece> airships() {
        return damage.flying().stream().map(i -> damage.piece(i).at(sky.time())).toList();
    }

    /** Each piece that can move and still stands, by number, and where it now is, all of it: for drawing it there. */
    public Map<Integer, Block> moving() {
        final Map<Integer, Block> boxes = new LinkedHashMap<>();
        for (final int piece : damage.moving()) {
            boxes.put(piece, placed(piece, sky.time(), lifts, props).block());
        }
        return boxes;
    }

    /** The platform whose controls the avatar has, if it has any. */
    public Optional<Lift> atControls() {
        return lifts.stream().filter(Lift::held).findFirst();
    }

    /** The platform whose controls the avatar could take where it stands, if any, and it has none yet. */
    public Optional<Lift> withinReach() {
        if (atControls().isPresent()) {
            return Optional.empty();
        }
        final Obstacles world = obstacles();
        return lifts.stream().filter(lift -> !armed(lift.piece()) && !damage.broken(lift.piece())
                        && lift.reaches(avatar.body(), world))
                .findFirst();
    }

    /** One step of {@code seconds}, with {@code controls}. */
    public Stepped step(final Controls controls, final double seconds) {
        final List<Event> happened = new ArrayList<>();
        final Obstacles was = obstacles();
        // the controls change hands; at them, the climb and descent are the platform's
        // manning a gun, the controls are the gun's: no vehicle's are taken
        final boolean gunner = turrets.stream().anyMatch(Turret::held);
        final Controls forLifts = gunner ? withoutTaking(controls) : controls;
        final List<Lift> reached = engines(forLifts, handed(forLifts, was, happened), happened);
        final boolean working = reached.stream().anyMatch(Lift::held);
        // going aboard a closed craft, the avatar is inside it, out of sight; stepping out, it is at its door
        final Avatar boarded = boarded(reached);
        final int inside = inside(reached);
        // and a gun is manned or let go, with what is left of the controls once a vehicle's have changed hands
        boolean changed = false;
        for (int i = 0; i < reached.size(); i++) {
            changed |= reached.get(i).held() != lifts.get(i).held();
        }
        final List<Turret> manned = manned(controls, changed, happened);
        // a vehicle with a gun on it is flown by whoever mans the gun: its keys fly it, A and D
        // turning it, and the mouse is the gun's
        final List<Lift> handed = gunshipsHeld(reached, manned);
        final int gunship = gunning(manned) < 0 ? -1 : liftOf(handed, manned.get(gunning(manned)).host());
        // (a land vehicle takes them as it always does: A and D steer it, and the mouse never turned it)
        final Controls steering = gunship >= 0 && !(handed.get(gunship).craft() instanceof Course.Chassis)
                ? gunshipControls(controls, seconds) : controls;
        final Avatar self = gunner(boarded, manned);
        final int gunning = gunning(manned);
        final boolean still = inside >= 0 || gunning >= 0;
        // driving a vehicle with a gun fixed to it, the trigger is the gun's, not the hands'
        final Controls own = !working ? withoutTaking(controls)
                : drivesAGun(reached) ? unarmed(atTheControls(controls)) : atTheControls(controls);
        // what each loose thing rests on, as things are
        final double to = sky.time() + seconds;
        final Lift.Support[] under = new Lift.Support[props.size()];
        for (int k = 0; k < props.size(); k++) {
            under[k] = props.get(k).support(was, List.of());
        }
        // the world moves on, carrying whoever stands on what moves, and the platforms docked on it
        // and the loose things resting on it
        final List<Lift> moved = moved(steering, seconds, to, was, handed, happened,
                piece -> restingOn(piece, under));
        final List<Prop> carriedProps = carriedProps(to, handed, moved, under);
        // each loose thing a step on: those still first, then those sliding, the foremost of them
        // first, each moving out of the way of what comes behind it
        Obstacles among = obstacles(to, moved, carriedProps);
        final List<Blow> landings = new ArrayList<>();
        final Prop[] stepped = new Prop[carriedProps.size()];
        for (final int k : stepOrder(carriedProps)) {
            final Prop prop = carriedProps.get(k);
            if (damage.broken(prop.piece())) {
                stepped[k] = prop;
                continue;
            }
            final Prop.Stepped step = prop.step(seconds, among, restingOn(prop.piece(), under));
            stepped[k] = step.prop();
            landings.addAll(step.blows());
            if (!step.prop().place().equals(prop.place())) {
                among = among.replaced(prop.piece(), List.of(step.prop().box()));
            }
        }
        final List<Prop> settled = new ArrayList<>(List.of(stepped));
        slidWith(carriedProps, settled, under);
        final Moves moves = new Moves(handed, moved, props, settled);
        final Avatar carried = inside >= 0
                ? self.carried(handed.get(inside).place(), moved.get(inside).place())
                : gunship >= 0 ? self.carried(handed.get(gunship).place(), moved.get(gunship).place())
                : gunning >= 0 ? self : carried(self, was, moves, to);
        final Obstacles now = obstacles(to, moved, settled);
        // the people go on as they were going, carried by what they stand on, hurt by coming down hard
        final List<Event> toPeople = new ArrayList<>();
        final List<Person> going = people(seconds, to, was, now, moves, toPeople);
        // act: the avatar does what it was asked, moves, and deals its blows, at the world and the
        // people in it; the bombs fall
        final Obstacles aimedAt = targets(now, going);
        final Avatar.Stepped acted = still ? Avatar.Stepped.still(carried.aged(seconds))
                : carried.step(own, seconds, now, aimedAt);
        // and the guns on their mounts are aimed and fired, by whoever mans one; their rounds pass
        // through what carries them
        final List<Turret> turned = new ArrayList<>();
        final List<Event> gunfire = new ArrayList<>();
        final List<Blow> rounds = new ArrayList<>(acted.blows());
        for (final Turret mounted : manned) {
            if (damage.broken(mounted.host())) {
                turned.add(mounted);
                continue;
            }
            // on what moves, it goes with it
            final int on = liftOf(moved, mounted.host());
            final Turret turret = on >= 0 ? mounted.carried(moved.get(on).place(), moved.get(on).tilt()) : mounted;
            // fired by whoever mans it; a fixed one, by whoever drives what it is fixed to
            final boolean firer = turret.held() || turret.mount().fixed() && on >= 0 && moved.get(on).held();
            final Turret.Stepped fired = turret.step(firer ? controls : Controls.NONE, seconds,
                    aimedAt.without(turret.host()), firer);
            turned.add(fired.turret());
            gunfire.addAll(fired.events());
            rounds.addAll(fired.blows());
        }
        // from inside a vehicle with a bomb bay, the trigger lets its bombs go; moored, it loads them back
        final List<Lift> loaded = new ArrayList<>(moved);
        Sky dropping = sky;
        for (int i = 0; i < loaded.size(); i++) {
            final Lift lift = loaded.get(i);
            if (damage.broken(lift.piece())) {
                continue;
            }
            final Lift after = lift.loaded(seconds, i == inside && controls.trigger());
            if (after.bombs() < lift.bombs()) {
                final Vec3 at = after.at(after.craft().bay());
                dropping = dropping.dropped(at, after.velocity(), after.piece());
                happened.add(new Event.Dropped(at, after.piece()));
            }
            loaded.set(i, after);
        }
        final Sky.Stepped fell = dropping.step(seconds, damage, now);
        // resolve: the world takes the blows, the avatar's and then the bombs'; and so do the people
        final List<Blow> blows = new ArrayList<>();
        final List<Blow> atPeople = new ArrayList<>();
        for (final Blow blow : rounds) {
            (Person.is(blow.piece()) ? atPeople : blows).add(blow);
        }
        blows.addAll(fell.blows());
        blows.addAll(landings);
        final Damage.Taken taken = damage.took(blows, piece -> placed(piece, to, moved, settled));
        final List<Person> hurt = struck(going, atPeople, fell.events(), toPeople);
        // and whoever the avatar walks into is pushed out of its way, and whatever it pushes against goes
        final List<Person> pushed = still ? hurt : pushed(acted.avatar().body(), hurt, toPeople);
        final List<Prop> shoved = still ? settled : shoved(settled, acted.avatar().body(), own, taken.damage());
        // report: the controls changing hands, the avatar's own events, the bombs', what came of the
        // blows, and what came to the people
        happened.addAll(acted.events());
        happened.addAll(gunfire);
        happened.addAll(fell.events());
        happened.addAll(taken.events());
        happened.addAll(toPeople);
                // standing at an open gun, the gunner faces the way it points
        final Avatar shown = gunning >= 0 && !turned.get(gunning).mount().closed()
                ? acted.avatar().facing(turned.get(gunning).heading()) : acted.avatar();
        return new Stepped(new Play(shown, taken.damage(), fell.sky(), loaded, pushed, shoved, turned),
                happened);
    }

    /** How everything that moves under its own rules moved on this step: the platforms, and the loose things. */
    private record Moves(List<Lift> fromLifts, List<Lift> toLifts, List<Prop> fromProps, List<Prop> toProps) {
    }

    /**
     * The order to step loose things in: those not sliding first, as they come; then those sliding,
     * the foremost along the way it is going first, so a row pushed along moves front to back, each
     * into the room the one before it has just left.
     */
    private static List<Integer> stepOrder(final List<Prop> props) {
        final List<Integer> still = new ArrayList<>();
        final List<Integer> sliding = new ArrayList<>();
        for (int k = 0; k < props.size(); k++) {
            (props.get(k).sliding() > 0 ? sliding : still).add(k);
        }
        sliding.sort((a, b) -> Double.compare(ahead(props.get(b)), ahead(props.get(a))));
        still.addAll(sliding);
        return still;
    }

    /** How far along the way it slides a loose thing is: for putting the foremost first. */
    private static double ahead(final Prop prop) {
        final double speed = prop.sliding();
        return (prop.place().x() * prop.vx() + prop.place().z() * prop.vz()) / speed;
    }

    /**
     * What rests on a loose thing that slid this step (from where it was {@code carried} to where it
     * {@code settled}) slides with it, across the ground: the fruit on the table pushed along. Those
     * under first, so what rests on what rests on it goes too.
     */
    private void slidWith(final List<Prop> carried, final List<Prop> settled, final Lift.Support[] under) {
        final int count = props.size();
        final int[] on = new int[count];
        for (int k = 0; k < count; k++) {
            on[k] = -1;
            if (props.get(k).resting(under[k])) {
                for (int j = 0; j < count; j++) {
                    if (j != k && props.get(j).piece() == under[k].piece()) {
                        on[k] = j;
                    }
                }
            }
        }
        final boolean[] done = new boolean[count];
        for (int pass = 0; pass <= count; pass++) {
            for (int k = 0; k < count; k++) {
                if (done[k] || (on[k] >= 0 && !done[on[k]])) {
                    continue;
                }
                if (on[k] >= 0) {
                    final int j = on[k];
                    final double dx = settled.get(j).place().x() - carried.get(j).place().x();
                    final double dz = settled.get(j).place().z() - carried.get(j).place().z();
                    if (dx != 0 || dz != 0) {
                        final Place p = settled.get(k).place();
                        settled.set(k, settled.get(k).at(new Place(p.x() + dx, p.z() + dz, p.height(), p.yaw())));
                    }
                }
                done[k] = true;
            }
        }
    }

    /** The loose things resting on piece {@code piece}, and those resting on them, by number, as {@code under} says. */
    private List<Integer> restingOn(final int piece, final Lift.Support[] under) {
        final List<Integer> on = new ArrayList<>();
        for (int k = 0; k < props.size(); k++) {
            final Prop prop = props.get(k);
            if (under[k].piece() == piece && prop.resting(under[k]) && !damage.broken(prop.piece())) {
                on.add(prop.piece());
                on.addAll(restingOn(prop.piece(), under));
            }
        }
        return on;
    }

    /**
     * The loose things, each carried by what it rests on as that moved: an airship along its route,
     * a platform as it went from among {@code from} to among {@code moved}, another loose thing as
     * that was carried. Those resting on loose things go after them.
     */
    private List<Prop> carriedProps(final double to, final List<Lift> from, final List<Lift> moved,
                                    final Lift.Support[] under) {
        final Prop[] carried = new Prop[props.size()];
        for (int pass = 0; pass < props.size() + 1; pass++) {
            boolean waiting = false;
            for (int k = 0; k < props.size(); k++) {
                if (carried[k] != null) {
                    continue;
                }
                final Prop prop = props.get(k);
                final int support = under[k].piece();
                if (!prop.resting(under[k]) || support < 0 || damage.broken(prop.piece())
                        || !damage.piece(support).moves()) {
                    carried[k] = prop;
                    continue;
                }
                final Course.Piece what = damage.piece(support);
                final Course.Piece root = what.rides() ? damage.piece(what.carrier()) : what;
                final int rootPiece = what.rides() ? what.carrier() : support;
                if (root.flies()) {
                    carried[k] = prop.carried(root.route().at(sky.time()), root.route().at(to));
                    continue;
                }
                Prop result = prop;
                boolean ready = true;
                for (int i = 0; i < from.size(); i++) {
                    if (from.get(i).piece() == rootPiece && !from.get(i).place().equals(moved.get(i).place())) {
                        result = prop.carried(from.get(i).place(), moved.get(i).place());
                    }
                }
                for (int j = 0; j < props.size(); j++) {
                    if (props.get(j).piece() == rootPiece) {
                        if (carried[j] == null) {
                            ready = false;
                        } else if (!props.get(j).place().equals(carried[j].place())) {
                            result = prop.carried(props.get(j).place(), carried[j].place());
                        }
                    }
                }
                if (ready) {
                    carried[k] = result;
                } else {
                    waiting = true;
                }
            }
            if (!waiting) {
                break;
            }
        }
        for (int k = 0; k < carried.length; k++) {
            if (carried[k] == null) {
                carried[k] = props.get(k);
            }
        }
        return List.of(carried);
    }

    /** How near the next loose thing on must be to be pushed along too: up against it, near as makes no odds. */
    private static final double CONTACT = 5;

    /**
     * The loose things, pushed: whatever the avatar's {@code body} is up against and moving into,
     * as {@code own} controls say, slides on the way it is pushed; and so does whatever that is up
     * against that way, and so on down the row ({@link #chain}). The whole row goes together, as
     * fast as it all gives: each thing's give ({@link Loose#give}) adding up as weights do, so two
     * melons go at half the pace of one, and a melon pushed into the table at the table's pace or
     * less. Not what it stands on, nor anything broken in {@code after}.
     */
    private static List<Prop> shoved(final List<Prop> props, final Body body, final Controls own,
                                     final Damage after) {
        final Vec3 wanted = Num.heading(body.yaw()).multiply(own.forward()).add(Num.right(body.yaw()).multiply(own.sideways()));
        final double strength = Math.min(1, wanted.magnitude());
        if (body.flying() || strength < 0.1) {
            return props;
        }
        final Vec3 way = wanted.normalize();
        final double pace = own.run() ? Body.RUN : Body.WALK;
        final List<Prop> now = new ArrayList<>(props);
        for (int k = 0; k < now.size(); k++) {
            final Prop prop = now.get(k);
            if (after.broken(prop.piece())) {
                continue;
            }
            final Block box = prop.box();
            if (body.height() >= box.top() - 1 || body.height() + Body.TALL <= box.bottom()) {
                continue;  // standing on it, or over or under it
            }
            final Vec2 out = box.push(body.position().x(), body.position().z(), Body.RADIUS + 4);
            if (out == null) {
                continue;
            }
            final double length = Math.hypot(out.x(), out.z());
            if (length < 1e-9) {
                continue;
            }
            // toward it, the way the push out of it is from
            final Vec3 into = new Vec3(-out.x() / length, 0, -out.z() / length);
            final double along = way.dotProduct(into);
            if (along <= 0.3) {
                continue;
            }
            final List<Integer> row = chain(props, k, into, after);
            double heaviness = 0;
            for (final int member : row) {
                heaviness += 1 / props.get(member).thing().give();
            }
            final double speed = pace * strength * along / heaviness;
            for (final int member : row) {
                final Prop pushed = now.get(member);
                if (speed > pushed.sliding()) {
                    now.set(member, pushed.pushed(into.x() * speed, into.z() * speed));
                }
            }
        }
        return now;
    }

    /**
     * The row of loose things pushed {@code into} by pushing the {@code first}: it, whatever it is up
     * against that way (side to side, not standing on it or under it), whatever that is up against,
     * and so on. Not what is broken in {@code after}.
     */
    private static List<Integer> chain(final List<Prop> props, final int first, final Vec3 into, final Damage after) {
        final List<Integer> row = new ArrayList<>(List.of(first));
        for (int c = 0; c < row.size(); c++) {
            final Block at = props.get(row.get(c)).box();
            // a little on the way it is pushed, and a little short top and bottom: what it would meet
            final Block nudged = new Block(at.x() + into.x() * CONTACT, at.z() + into.z() * CONTACT, at.halfWidth(),
                    at.halfDepth(), at.yaw(), at.bottom() + 1, at.top() - 1);
            final double here = at.x() * into.x() + at.z() * into.z();
            for (int j = 0; j < props.size(); j++) {
                if (row.contains(j) || after.broken(props.get(j).piece())) {
                    continue;
                }
                final Block other = props.get(j).box();
                if (other.x() * into.x() + other.z() * into.z() > here && nudged.overlaps(other)) {
                    row.add(j);
                }
            }
        }
        return row;
    }

    /**
     * The platforms, their controls taken or let go: a {@link Action#TAKE} at a console takes
     * them, and another lets them go, as does no longer standing at them.
     */
    private List<Lift> handed(final Controls controls, final Obstacles world, final List<Event> happened) {
        boolean take = controls.actions().stream().anyMatch(a -> a instanceof Action.Take);
        final List<Lift> handed = new ArrayList<>(lifts);
        for (int i = 0; i < handed.size(); i++) {
            final Lift lift = handed.get(i);
            if (armed(lift.piece())) {
                continue;  // flown by whoever mans its gun
            }
            if (lift.held() && (take || damage.broken(lift.piece())
                    || !lift.craft().closed() && !lift.reaches(avatar.body(), world))) {
                handed.set(i, lift.held(false));
                happened.add(new Event.LetGo(lift.piece()));
                take = false;
            }
        }
        for (int i = 0; take && i < handed.size(); i++) {
            final Lift lift = handed.get(i);
            if (!armed(lift.piece()) && !damage.broken(lift.piece()) && lift.reaches(avatar.body(), world)) {
                handed.set(i, lift.held(true));
                happened.add(new Event.TookControls(lift.piece()));
                take = false;
            }
        }
        return handed;
    }

    /** The gun the avatar mans, if it mans one. */
    public Optional<Turret> manning() {
        return turrets.stream().filter(Turret::held).findFirst();
    }

    /** The gun the avatar could man where it stands, if any, and it is at no controls or gun yet. */
    public Optional<Turret> gunWithinReach() {
        if (atControls().isPresent() || manning().isPresent()) {
            return Optional.empty();
        }
        return turrets.stream().filter(t -> !damage.broken(t.host()) && t.reaches(avatar.body())).findFirst();
    }

    /** Whether the avatar is out of sight: inside a closed craft, or manning a gun from inside. */
    public boolean hidden() {
        return aboard().isPresent() || manning().filter(t -> t.mount().closed()).isPresent();
    }

    /**
     * The guns, manned or let go: a {@link Action#TAKE} at a gun's station mans it, unless the
     * controls of a vehicle have just changed hands with it ({@code taken}); another lets it go, as
     * does what carries it breaking.
     */
    private List<Turret> manned(final Controls controls, final boolean taken, final List<Event> happened) {
        boolean take = !taken && controls.actions().stream().anyMatch(a -> a instanceof Action.Take);
        final List<Turret> manned = new ArrayList<>(turrets);
        for (int i = 0; i < manned.size(); i++) {
            final Turret turret = manned.get(i);
            if (turret.held() && (take || damage.broken(turret.host()))) {
                manned.set(i, turret.held(false));
                happened.add(new Event.LetGo(turret.host()));
                take = false;
            }
        }
        for (int i = 0; take && i < manned.size(); i++) {
            final Turret turret = manned.get(i);
            if (!damage.broken(turret.host()) && turret.reaches(avatar.body())) {
                manned.set(i, turret.held(true));
                happened.add(new Event.TookControls(turret.host()));
                take = false;
            }
        }
        return manned;
    }

    /** How fast A and D turn a gunship, whose gunner's mouse is the gun's: degrees a second. */
    public static final double GUNSHIP_TURN = 60;

    /**
     * Whether piece {@code piece} carries a gun that is manned: then it is flown by whoever mans the
     * gun, not from its console. (A fixed gun is fired by whoever drives it, from its wheel.)
     */
    private boolean drivesAGun(final List<Lift> handed) {
        return handed.stream().anyMatch(l -> l.held() && turrets.stream()
                .anyMatch(t -> t.host() == l.piece() && t.mount().fixed()));
    }

    /** Whether piece {@code piece} carries a gun to be manned. */
    private boolean armed(final int piece) {
        return turrets.stream().anyMatch(t -> t.host() == piece && !t.mount().fixed());
    }

    /** Which of {@code lifts} is piece {@code piece}; -1 for none. */
    private static int liftOf(final List<Lift> lifts, final int piece) {
        for (int i = 0; i < lifts.size(); i++) {
            if (lifts.get(i).piece() == piece) {
                return i;
            }
        }
        return -1;
    }

    /** {@code lifts}, those with a gun on them held while their gun is manned ({@code manned}), and let go when it is not. */
    private static List<Lift> gunshipsHeld(final List<Lift> lifts, final List<Turret> manned) {
        final List<Lift> held = new ArrayList<>(lifts);
        for (final Turret turret : manned) {
            final int on = liftOf(held, turret.host());
            if (on >= 0 && !turret.mount().fixed() && held.get(on).held() != turret.held()) {
                held.set(on, held.get(on).held(turret.held()));
            }
        }
        return held;
    }

    /**
     * The controls, as a gunship takes them from its gunner: ahead and back, up and down as they
     * are; A and D, rather than sliding it sideways, turn it, the mouse's turn being the gun's.
     */
    private static Controls gunshipControls(final Controls controls, final double seconds) {
        return new Controls(controls.forward(), 0, controls.sideways() * GUNSHIP_TURN * seconds, controls.lift(),
                controls.run(), false, 0, List.of());
    }

    /** Which of {@code manned} the avatar mans; -1 for none. */
    private static int gunning(final List<Turret> manned) {
        for (int i = 0; i < manned.size(); i++) {
            if (manned.get(i).held()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@code avatar}, as a gun was manned or let go ({@code manned}): manning it, at its seat,
     * facing the way it points; letting a closed one go, back out at its station, settling onto
     * what is there (at an open one, it is standing there already).
     */
    private Avatar gunner(final Avatar avatar, final List<Turret> manned) {
        for (int i = 0; i < manned.size(); i++) {
            final Turret was = turrets.get(i);
            final Turret now = manned.get(i);
            if (was.held() == now.held() || !now.held() && !now.mount().closed()) {
                continue;  // at an open gun, whoever let it go is standing there still
            }
            final Body body = now.held()
                    ? Body.standing(now.at(now.mount().seat()), now.heading())
                    : new Body(now.at(now.mount().station()), Num.wrap(now.base().yaw() + 180),
                    Num.wrap(now.base().yaw() + 180), 0, 0, 0, 0, false, false, 0, avatar.body().time());
            return new Avatar(body, avatar.wearingPack(), avatar.sweep(), avatar.arms());
        }
        return avatar;
    }

    /** The closed craft the avatar is inside, at its controls, if it is in one: then it is out of sight. */
    public Optional<Lift> aboard() {
        return atControls().filter(lift -> lift.craft().closed());
    }

    /** Which of {@code handed} is a closed craft with the avatar at its controls, inside it; -1 for none. */
    private static int inside(final List<Lift> handed) {
        for (int i = 0; i < handed.size(); i++) {
            if (handed.get(i).held() && handed.get(i).craft().closed()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The avatar, as the controls changed hands ({@code handed}): taking a closed craft's, it goes
     * inside, to its cabin, facing the way it does; letting them go, it drops out of the hatch, and
     * falls from there if the craft is aloft. Otherwise, where it was.
     */
    private Avatar boarded(final List<Lift> handed) {
        for (int i = 0; i < handed.size(); i++) {
            final Lift was = lifts.get(i);
            final Lift now = handed.get(i);
            if (!now.craft().closed() || was.held() == now.held()) {
                continue;
            }
            final Body body = now.held()
                    ? Body.standing(now.at(now.craft().cabin()), now.place().yaw())
                    : new Body(out(now), now.place().yaw(), now.place().yaw(), 0, 0, 0, 0, false, false, 0,
                    avatar.body().time());
            return new Avatar(body, avatar.wearingPack(), avatar.sweep(), avatar.arms());
        }
        return avatar;
    }

    /** Where the avatar is as it steps out of {@code craft}: dropping from its hatch, its head just under it; on the ground, if it is moored. */
    private static Vec3 out(final Lift craft) {
        final Vec3 hatch = craft.pilot();
        return new Vec3(hatch.x(), Math.min(0, hatch.y() + Body.TALL + 1), hatch.z());
    }

    /** At a platform's controls, the key that takes off or lands ({@link Action#FLY}) starts its engine, or cuts it. */
    private static List<Lift> engines(final Controls controls, final List<Lift> handed, final List<Event> happened) {
        if (controls.actions().stream().noneMatch(a -> a instanceof Action.Fly)) {
            return handed;
        }
        final List<Lift> switched = new ArrayList<>(handed);
        for (int i = 0; i < switched.size(); i++) {
            final Lift lift = switched.get(i);
            if (lift.held() && lift.craft().handling().cuts()) {
                switched.set(i, lift.engine(!lift.engine()));
                happened.add(lift.engine() ? new Event.EngineCut(lift.piece()) : new Event.EngineStarted(lift.piece()));
            }
        }
        return switched;
    }

    /**
     * The platforms, a step on: each docked one first carried by what it rests on (an airship
     * along its route, another platform as that moved), then flown, or falling, or resting. Those
     * docked on another platform go after it. A platform coming down hard enough is heard settling.
     */
    private List<Lift> moved(final Controls controls, final double seconds, final double to, final Obstacles was,
                             final List<Lift> handed, final List<Event> happened,
                             final java.util.function.IntFunction<List<Integer>> resting) {
        final int count = handed.size();
        final Lift.Support[] under = new Lift.Support[count];
        final int[] onLift = new int[count];
        final List<List<Block>> also = new ArrayList<>();
        final int standing = avatar.body().grounded()
                ? was.under(avatar.body().position().x(), avatar.body().position().z(), Body.FOOTING,
                avatar.body().height()) : -1;
        for (int i = 0; i < count; i++) {
            final Lift lift = handed.get(i);
            // the avatar is solid to a platform it is not on: it will not come down through it
            final Vec3 at = avatar.body().position();
            also.add(standing == lift.piece() || damage.riding(lift.piece()).contains(standing) ? List.of()
                    : List.of(new Block(at.x(), at.z(), Body.RADIUS, Body.RADIUS, 0, avatar.body().height(),
                    avatar.body().height() + Body.TALL)));
            under[i] = lift.support(was, damage.riding(lift.piece()), also.get(i));
            onLift[i] = -1;
            final int carrier = under[i].piece() >= 0 && damage.piece(under[i].piece()).rides()
                    ? damage.piece(under[i].piece()).carrier() : under[i].piece();
            for (int j = 0; j < count; j++) {
                if (j != i && handed.get(j).piece() == carrier) {
                    onLift[i] = j;
                }
            }
        }
        final Obstacles ahead = obstacles(to, handed, props);
        final Lift[] moved = new Lift[count];
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < count; i++) {
                if ((onLift[i] >= 0) != (pass == 1)) {
                    continue;
                }
                Lift lift = handed.get(i);
                if (damage.broken(lift.piece())) {
                    moved[i] = lift;
                    continue;
                }
                if (lift.resting(under[i]) && under[i].piece() >= 0) {
                    final Course.Piece what = damage.piece(under[i].piece());
                    final Course.Piece carrier = what.rides() ? damage.piece(what.carrier()) : what;
                    if (carrier.flies()) {
                        lift = lift.carried(carrier.route().at(sky.time()), carrier.route().at(to));
                    } else if (onLift[i] >= 0) {
                        // carried as the platform under it moved, it rests on it still: nothing more to do
                        moved[i] = lift.carried(handed.get(onLift[i]).place(), moved[onLift[i]].place());
                        continue;
                    }
                }
                // its cargo, and the platforms docked on it, go where it goes: not in its way
                final List<Integer> aboard = new ArrayList<>(damage.riding(lift.piece()));
                aboard.addAll(resting.apply(lift.piece()));
                for (int j = 0; j < count; j++) {
                    if (onLift[j] == i && handed.get(j).resting(under[j])) {
                        aboard.add(handed.get(j).piece());
                    }
                }
                moved[i] = lift.step(controls, seconds, ahead, aboard, also.get(i));
                if (lift.climb() < -150 && moved[i].climb() == 0 && !moved[i].engine()) {
                    final Place p = moved[i].place();
                    happened.add(new Event.Settled(lift.piece(), -lift.climb(), new Vec3(p.x(), -p.height(), p.z())));
                }
            }
        }
        return List.of(moved);
    }

    /** What platform {@code lift} rests on, or would settle on with its engine cut. */
    public Lift.Support under(final Lift lift) {
        return lift.support(obstacles(), damage.riding(lift.piece()), List.of());
    }

    /**
     * At the controls the avatar stands at the console: the keys that would move it, and the
     * mouse's turn, fly the platform instead, and it is carried round as that turns. It can still
     * aim, draw a weapon and fire.
     */
    private static Controls atTheControls(final Controls controls) {
        return new Controls(0, 0, 0, 0, controls.run(), controls.trigger(), controls.aim(),
                controls.actions().stream().filter(a -> a instanceof Action.Draw).toList());
    }

    /** The controls, less the trigger. */
    private static Controls unarmed(final Controls controls) {
        return new Controls(controls.forward(), controls.sideways(), controls.turn(), controls.lift(), controls.run(), false,
                controls.aim(), controls.actions());
    }

    /** The controls, less taking a platform's: that is the world's to hand over, and done already. */
    private static Controls withoutTaking(final Controls controls) {
        return controls.with(controls.actions().stream().filter(a -> !(a instanceof Action.Take)).toList());
    }

    /**
     * {@code avatar}, carried by what it stands on as that moves on: an airship along its route to
     * {@code to}, a platform or a loose thing as it went ({@code moves}).
     */
    private Avatar carried(final Avatar avatar, final Obstacles was, final Moves moves, final double to) {
        final Place[] motion = motion(avatar.body(), was, moves, to);
        return motion == null ? avatar : avatar.carried(motion[0], motion[1]);
    }

    /**
     * How what {@code body} stands on moves on this step, from where it was to where it will be;
     * null if it stands on nothing that moves, or is not standing.
     */
    private Place[] motion(final Body body, final Obstacles was, final Moves moves, final double to) {
        if (!body.grounded()) {
            return null;
        }
        final int under = was.under(body.position().x(), body.position().z(), Body.FOOTING, body.height());
        if (under < 0 || !damage.piece(under).moves()) {
            return null;
        }
        // standing on what rides on something (the stall's table, on the barge), it goes as that goes
        final int piece = damage.piece(under).rides() ? damage.piece(under).carrier() : under;
        final Course.Piece what = damage.piece(piece);
        if (what.flies()) {
            return new Place[] {what.route().at(sky.time()), what.route().at(to)};
        }
        // a platform, or a loose thing, that has not moved carries nothing anywhere
        for (int i = 0; i < moves.fromLifts().size(); i++) {
            if (moves.fromLifts().get(i).piece() == piece) {
                final Place a = moves.fromLifts().get(i).place();
                final Place b = moves.toLifts().get(i).place();
                return a.equals(b) ? null : new Place[] {a, b};
            }
        }
        for (int i = 0; i < moves.fromProps().size(); i++) {
            if (moves.fromProps().get(i).piece() == piece) {
                final Place a = moves.fromProps().get(i).place();
                final Place b = moves.toProps().get(i).place();
                return a.equals(b) ? null : new Place[] {a, b};
            }
        }
        return null;
    }

    /**
     * The people, a step on: carried by what they stand on as it moved, then going on as they
     * were, among {@code world}: coming to a stop, or falling.
     */
    private List<Person> people(final double seconds, final double to, final Obstacles was, final Obstacles world,
                                final Moves moves, final List<Event> happened) {
        final List<Person> now = new ArrayList<>();
        for (final Person person : people) {
            if (person.down()) {
                now.add(person);  // gone: nothing more happens to them
                continue;
            }
            final Place[] motion = motion(person.body(), was, moves, to);
            final Person carried = motion == null ? person : person.carried(motion[0], motion[1]);
            final Person.Stepped stepped = carried.moved(world, seconds);
            now.add(stepped.person());
            happened.addAll(stepped.events());
        }
        return now;
    }

    /** {@code world}, and the people in it as targets to strike, each by their number ({@link Person#piece}). */
    private static Obstacles targets(final Obstacles world, final List<Person> people) {
        if (people.isEmpty()) {
            return world;
        }
        final List<Block> blocks = new ArrayList<>(world.blocks());
        final List<Integer> pieces = new ArrayList<>(world.pieces());
        for (final Person person : people) {
            if (person.down()) {
                continue;
            }
            blocks.add(person.box());
            pieces.add(person.piece());
        }
        return new Obstacles(blocks, world.gates(), pieces);
    }

    /**
     * The people, struck: by the avatar's {@code blows} that met them, and by any bomb going off
     * among {@code bombs}' events near enough, each knocked back from it. What came of it goes into
     * {@code happened}: each one struck, with what they have left, and anyone knocked down.
     */
    private static List<Person> struck(final List<Person> people, final List<Blow> blows, final List<Event> bombs,
                                       final List<Event> happened) {
        final List<Person> now = new ArrayList<>(people);
        for (final Blow blow : blows) {
            final int k = Person.which(blow.piece());
            final Vec3 back = blow.normal().multiply(-Person.FLINCH);
            now.set(k, hit(now.get(k), blow, back.x(), back.z(), happened));
        }
        for (final Event event : bombs) {
            if (event instanceof Event.Exploded exploded) {
                for (int k = 0; k < now.size(); k++) {
                    final Person person = now.get(k);
                    if (person.down()) {
                        continue;
                    }
                    final Vec3 at = person.body().position();
                    final Vec3 away = at.subtract(exploded.at());
                    final double across = Math.hypot(away.x(), away.z());
                    if (across <= Sky.BLAST + Body.RADIUS && Math.abs(away.y()) <= Sky.BLAST + Body.TALL) {
                        final Vec3 push = across < 1e-6 ? Vec3.ZERO
                                : new Vec3(away.x() / across, 0, away.z() / across).multiply(4 * Person.FLINCH);
                        final Blow blow = new Blow(exploded.at(), new Vec3(0, -1, 0), Surface.BLOCK, Weapon.BOMB,
                                person.piece());
                        now.set(k, hit(person, blow, push.x(), push.z(), happened));
                    }
                }
            }
        }
        return now;
    }

    private static Person hit(final Person person, final Blow blow, final double vx, final double vz,
                              final List<Event> happened) {
        final Person after = person.struck(blow.by().blow()).shoved(vx, vz);
        happened.add(Event.Struck.of(blow, after.health() / Person.TOUGHNESS));
        if (after.down() && !person.down()) {
            happened.add(new Event.Destroyed(person.id(), after.body().position()));
        }
        return after;
    }

    /**
     * The people, pushed: whoever the avatar's {@code body} walks into is shoved out of its way, sent
     * along at its pace and a little more, and up off their seat; and people pushed into each other
     * part, each going half the way. A shove hard enough is heard.
     */
    private static List<Person> pushed(final Body body, final List<Person> people, final List<Event> happened) {
        final List<Person> now = new ArrayList<>(people);
        final double reach = 2 * Body.RADIUS;
        for (int k = 0; k < now.size(); k++) {
            final Person person = now.get(k);
            final Vec3 apart = person.body().position().subtract(body.position());
            final double across = Math.hypot(apart.x(), apart.z());
            if (person.down() || across >= reach || Math.abs(apart.y()) >= Body.TALL) {
                continue;
            }
            final Vec3 out = across < 1e-6 ? Num.heading(body.yaw()) : new Vec3(apart.x() / across, 0, apart.z() / across);
            Person shoved = person.shifted(out.x() * (reach - across), out.z() * (reach - across));
            final double pushing = body.going().dotProduct(out) * Person.SHOVE;
            final double going = person.body().going().dotProduct(out);
            if (pushing > going) {
                shoved = shoved.shoved(out.x() * (pushing - going), out.z() * (pushing - going));
                if (pushing > 200 && going < 100) {
                    happened.add(new Event.Shoved(person.id(), pushing, person.body().position()));
                }
            }
            now.set(k, shoved);
        }
        for (int i = 0; i < now.size(); i++) {
            for (int j = i + 1; j < now.size(); j++) {
                final Vec3 apart = now.get(j).body().position().subtract(now.get(i).body().position());
                final double across = Math.hypot(apart.x(), apart.z());
                if (now.get(i).down() || now.get(j).down() || across >= reach || across < 1e-6
                        || Math.abs(apart.y()) >= Body.TALL) {
                    continue;
                }
                final double half = (reach - across) / 2 / across;
                now.set(i, now.get(i).shifted(-apart.x() * half, -apart.z() * half));
                now.set(j, now.get(j).shifted(apart.x() * half, apart.z() * half));
            }
        }
        return now;
    }

    /** Piece {@code piece} where it is {@code seconds} after the start, the platforms being {@code lifts} and the loose things {@code props}. */
    private Course.Piece placed(final int piece, final double seconds, final List<Lift> lifts, final List<Prop> props) {
        final Course.Piece what = damage.piece(piece);
        if (what.flies()) {
            return what.at(seconds);
        }
        if (what.rides()) {
            return what.placed(placed(what.carrier(), seconds, lifts, props).place());
        }
        if (what.kind().loose()) {
            for (final Prop prop : props) {
                if (prop.piece() == piece) {
                    return what.placed(prop.place());
                }
            }
        }
        for (final Lift lift : lifts) {
            if (lift.piece() == piece) {
                return what.placed(lift.place());
            }
        }
        return what;
    }

    /** What still stands, {@code seconds} after the start with the platforms {@code lifts} and the loose things {@code props}: everything that moves where it then is. */
    private Obstacles obstacles(final double seconds, final List<Lift> lifts, final List<Prop> props) {
        final Obstacles built = damage.obstacles();
        final List<Integer> moving = damage.moving();
        if (moving.isEmpty()) {
            return built;
        }
        // each moving piece's blocks, in the order it was built with, put where it now is
        final Map<Integer, List<Block>> placed = new TreeMap<>();
        for (final int piece : moving) {
            placed.put(piece, placed(piece, seconds, lifts, props).solid());
        }
        final Map<Integer, Integer> next = new TreeMap<>();
        final List<Block> blocks = new ArrayList<>(built.blocks().size());
        for (int i = 0; i < built.blocks().size(); i++) {
            final int piece = built.pieces().get(i);
            final List<Block> now = placed.get(piece);
            if (now == null) {
                blocks.add(built.blocks().get(i));
            } else {
                final int k = next.merge(piece, 1, Integer::sum) - 1;
                blocks.add(now.get(k));
            }
        }
        return new Obstacles(blocks, built.gates(), built.pieces());
    }
}
