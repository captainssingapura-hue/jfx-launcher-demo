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
public record Play(Avatar avatar, Damage damage, Sky sky, List<Lift> lifts, List<Person> people) {

    /**
     * Which rules the world steps by. Raise it whenever a change would step the same inputs to a
     * different world, so a recording made under the old rules says so rather than going astray.
     */
    public static final int RULES = 8;

    public Play {
        lifts = List.copyOf(lifts);
        people = List.copyOf(people);
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
        return new Play(new Avatar(Body.standing(Vec3.ZERO, Course.START_YAW), pack), Damage.of(new Course()));
    }

    /**
     * The whole world stirred into one number, to the bit: two worlds with the same checksum are,
     * as near as matters, the same. The avatar, the sky, the platforms and the people are all
     * records, so their text is exact and complete.
     */
    public long checksum() {
        long sum = damage.checksum();
        final String text = avatar.toString() + sky.toString() + lifts.toString() + people.toString();
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
        return obstacles(sky.time(), lifts);
    }

    /** The airships still flying, each where it now is. */
    public List<Course.Piece> airships() {
        return damage.flying().stream().map(i -> damage.piece(i).at(sky.time())).toList();
    }

    /** Each piece that can move and still stands, by number, and where it now is, all of it: for drawing it there. */
    public Map<Integer, Block> moving() {
        final Map<Integer, Block> boxes = new LinkedHashMap<>();
        for (final int piece : damage.moving()) {
            boxes.put(piece, placed(piece, sky.time(), lifts).block());
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
        return lifts.stream().filter(lift -> lift.reaches(avatar.body(), world)).findFirst();
    }

    /** One step of {@code seconds}, with {@code controls}. */
    public Stepped step(final Controls controls, final double seconds) {
        final List<Event> happened = new ArrayList<>();
        final Obstacles was = obstacles();
        // the controls change hands; at them, the climb and descent are the platform's
        final List<Lift> handed = engines(controls, handed(controls, was, happened), happened);
        final boolean working = handed.stream().anyMatch(Lift::held);
        final Controls own = working ? atTheControls(controls) : withoutTaking(controls);
        // the world moves on, carrying whoever stands on what moves, and the platforms docked on it
        final double to = sky.time() + seconds;
        final List<Lift> moved = moved(controls, seconds, to, was, handed, happened);
        final Avatar carried = carried(avatar, was, handed, moved, to);
        final Obstacles now = obstacles(to, moved);
        // the people go on as they were going, carried by what they stand on, hurt by coming down hard
        final List<Event> toPeople = new ArrayList<>();
        final List<Person> going = people(seconds, to, was, now, handed, moved, toPeople);
        // act: the avatar does what it was asked, moves, and deals its blows, at the world and the
        // people in it; the bombs fall
        final Avatar.Stepped acted = carried.step(own, seconds, now, targets(now, going));
        final Sky.Stepped fell = sky.step(seconds, damage, now);
        // resolve: the world takes the blows, the avatar's and then the bombs'; and so do the people
        final List<Blow> blows = new ArrayList<>();
        final List<Blow> atPeople = new ArrayList<>();
        for (final Blow blow : acted.blows()) {
            (Person.is(blow.piece()) ? atPeople : blows).add(blow);
        }
        blows.addAll(fell.blows());
        final Damage.Taken taken = damage.took(blows, piece -> placed(piece, to, moved));
        final List<Person> hurt = struck(going, atPeople, fell.events(), toPeople);
        // and whoever the avatar walks into is pushed out of its way
        final List<Person> pushed = pushed(acted.avatar().body(), hurt, toPeople);
        // report: the controls changing hands, the avatar's own events, the bombs', what came of the
        // blows, and what came to the people
        happened.addAll(acted.events());
        happened.addAll(fell.events());
        happened.addAll(taken.events());
        happened.addAll(toPeople);
        return new Stepped(new Play(acted.avatar(), taken.damage(), fell.sky(), moved, pushed), happened);
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
            if (lift.held() && (take || !lift.reaches(avatar.body(), world))) {
                handed.set(i, lift.held(false));
                happened.add(new Event.LetGo(lift.piece()));
                take = false;
            }
        }
        for (int i = 0; take && i < handed.size(); i++) {
            final Lift lift = handed.get(i);
            if (lift.reaches(avatar.body(), world)) {
                handed.set(i, lift.held(true));
                happened.add(new Event.TookControls(lift.piece()));
                take = false;
            }
        }
        return handed;
    }

    /** At a platform's controls, the key that takes off or lands ({@link Action#FLY}) starts its engine, or cuts it. */
    private static List<Lift> engines(final Controls controls, final List<Lift> handed, final List<Event> happened) {
        if (controls.actions().stream().noneMatch(a -> a instanceof Action.Fly)) {
            return handed;
        }
        final List<Lift> switched = new ArrayList<>(handed);
        for (int i = 0; i < switched.size(); i++) {
            final Lift lift = switched.get(i);
            if (lift.held()) {
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
                             final List<Lift> handed, final List<Event> happened) {
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
        final Obstacles ahead = obstacles(to, handed);
        final Lift[] moved = new Lift[count];
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < count; i++) {
                if ((onLift[i] >= 0) != (pass == 1)) {
                    continue;
                }
                Lift lift = handed.get(i);
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

    /** The controls, less taking a platform's: that is the world's to hand over, and done already. */
    private static Controls withoutTaking(final Controls controls) {
        return controls.with(controls.actions().stream().filter(a -> !(a instanceof Action.Take)).toList());
    }

    /**
     * {@code avatar}, carried by what it stands on as that moves on: an airship along its route to
     * {@code to}, or a platform from where it was among {@code from} to where it is among {@code moved}.
     */
    private Avatar carried(final Avatar avatar, final Obstacles was, final List<Lift> from, final List<Lift> moved,
                           final double to) {
        final Place[] motion = motion(avatar.body(), was, from, moved, to);
        return motion == null ? avatar : avatar.carried(motion[0], motion[1]);
    }

    /**
     * How what {@code body} stands on moves on this step, from where it was to where it will be;
     * null if it stands on nothing that moves, or is not standing.
     */
    private Place[] motion(final Body body, final Obstacles was, final List<Lift> from, final List<Lift> moved,
                           final double to) {
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
        for (int i = 0; i < from.size(); i++) {
            if (from.get(i).piece() == piece) {
                // a platform that has not moved carries nothing anywhere
                return from.get(i).place().equals(moved.get(i).place()) ? null
                        : new Place[] {from.get(i).place(), moved.get(i).place()};
            }
        }
        return null;
    }

    /**
     * The people, a step on: carried by what they stand on as it moved, then going on as they
     * were, among {@code world}: coming to a stop, or falling.
     */
    private List<Person> people(final double seconds, final double to, final Obstacles was, final Obstacles world,
                                final List<Lift> from, final List<Lift> moved, final List<Event> happened) {
        final List<Person> now = new ArrayList<>();
        for (final Person person : people) {
            if (person.down()) {
                now.add(person);  // gone: nothing more happens to them
                continue;
            }
            final Place[] motion = motion(person.body(), was, from, moved, to);
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

    /** Piece {@code piece} where it is {@code seconds} after the start, the platforms being {@code lifts}. */
    private Course.Piece placed(final int piece, final double seconds, final List<Lift> lifts) {
        final Course.Piece what = damage.piece(piece);
        if (what.flies()) {
            return what.at(seconds);
        }
        if (what.rides()) {
            return what.placed(placed(what.carrier(), seconds, lifts).place());
        }
        for (final Lift lift : lifts) {
            if (lift.piece() == piece) {
                return what.placed(lift.place());
            }
        }
        return what;
    }

    /** What still stands, {@code seconds} after the start with the platforms {@code lifts}: everything that moves where it then is. */
    private Obstacles obstacles(final double seconds, final List<Lift> lifts) {
        final Obstacles built = damage.obstacles();
        final List<Integer> moving = damage.moving();
        if (moving.isEmpty()) {
            return built;
        }
        // each moving piece's blocks, in the order it was built with, put where it now is
        final Map<Integer, List<Block>> placed = new TreeMap<>();
        for (final int piece : moving) {
            placed.put(piece, placed(piece, seconds, lifts).solid());
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
