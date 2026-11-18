package com.example.fxsuite.avatar.worlds;

import com.example.fxsuite.avatar.build.Village;
import com.example.fxsuite.avatar.look.DrawnDemo;
import com.example.fxsuite.avatar.look.RoadScenery;
import com.example.fxsuite.avatar.look.Scenery;
import com.example.fxsuite.avatar.roads.Mapped;
import com.example.fxsuite.avatar.roads.Profile;
import com.example.fxsuite.avatar.roads.Decks;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.roads.Sections;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Volume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The meadow village as a demo of its own ({@link Village}): its meadow, its river, its village, its church on its
 * hill; its roads as described ({@link Village#roads}), laid as roads are laid now ({@link Roadworks}), of the meadow's
 * own kinds ({@link #KINDS}): its highway's carriageways, its slip roads, the road into the village over its bridge,
 * the lane up to the church. Drawn as it is laid: its pavements and markings by the roads' own drawing, its bridge as
 * a structure is ({@link Decks}). Known by its name, {@code VILLAGE}, its runs kept and replayed.
 *
 * <p>A one-way road whose line was described down its middle is laid with its line at its pavement's edge, as a
 * one-way kind has it, so its pavement lies where it was described.
 */
public final class Meadow implements DrawnDemo {

    /** The meadow village. */
    public static final Meadow DEMO = new Meadow();

    /**
     * The meadow's kinds across, by the names its description gives them: each carriageway one way, driving on the right,
     * its line at its inner edge, a strip of the median its own; a slip road, one lane; the road into the village, a
     * lane each way; the lane to the church, unmarked gravel. A carriageway's median a strip of grass beside it, the
     * rest of the median between the two graded as their slopes meet, however high each is.
     */
    public static final String KINDS = """
            # the meadow's roads: driving on the right
            courses carriageway: asphalt 40mm, binder 60mm, base 200mm, subbase 250mm
            courses road: asphalt 40mm, base 150mm, subbase 200mm
            courses gravel: gravel 100mm, subbase 150mm

            section "carriageway" grade=5% vertical=100m fill=2 cut=2 ditch=1.2m,0.6m bank=8m cutting=10m
              median    1m     grass        fall=4%
              |
              shoulder  1m     carriageway  fall=2%
              lane      3.65m  carriageway  fall=2%
              lane      3.65m  carriageway  fall=2%
              shoulder  3.3m   carriageway  fall=2%
              verge     2m     grass        fall=5%

            section "slip" grade=7% vertical=40m fill=2 cut=2 ditch=1m,0.5m bank=6m cutting=8m
              |
              shoulder  1m     carriageway  fall=2%
              lane      3.65m  carriageway  fall=2%
              shoulder  1m     carriageway  fall=2%
              verge     1.5m   grass        fall=5%

            section "village" grade=8% vertical=50m fill=2 cut=2 ditch=0.8m,0.4m bank=5m cutting=6m
              verge     1m     grass        fall=5%
              lane      3m     road         fall=2%
              |
              lane      3m     road         fall=2%
              verge     1m     grass        fall=5%

            section "lane" grade=14% vertical=30m fill=2 cut=2 ditch=0,0 bank=4m cutting=5m
              path      2m     gravel
              |
              path      2m     gravel
            """;

    /** The keys its bridge is drawn by. */
    static final int DECK_KEYS = 910_000;

    private Meadow() {
    }

    @Override
    public String name() {
        return "VILLAGE";
    }

    @Override
    public String title() {
        return "Meadow village";
    }

    @Override
    public String description() {
        return "A meadow with a river winding across it, a bridge over it, and a small village of earth houses round a"
                + " well by its bank, each its own size and finish: walk in, wade across, look round.";
    }

    @Override
    public double startYaw() {
        return 0;
    }

    @Override
    public boolean recordable() {
        return true;
    }

    /** Its roads, as laid: worked out once, when first wanted. */
    public static Roadworks roads() {
        return Laid.WORKS;
    }

    /** Its land: its ground ({@link Village#ground}) as its roads leave it. */
    public static Land land() {
        return Laid.LAND;
    }

    private static final class Laid {
        static final Roadworks WORKS = lay();
        static final Land LAND = Village.ground().with(WORKS.land(Double.NaN));
    }

    /**
     * Its roads as described, laid in order over its ground: each along its line worked out ({@link Mapped}), of its
     * kind; a one-way road's line moved to its pavement's edge; its bridges given where its description has them, its
     * pinned heights held.
     */
    static Roadworks lay() {
        final Map<String, Section> kinds = Sections.parse(KINDS);
        final Land ground = Village.ground();
        final Profile raw = (x, z, below) -> ground.height(x, z);
        return Roadworks.design(raw, Mapped.plans(Village.roads(), (way, kind) -> kinds.get(kind), raw, null));
    }

    @Override
    public void lay(final Course.Layout world) {
        final Roadworks works = roads();
        final List<Course.Piece> pieces = new ArrayList<>(Village.pieces(land()));
        pieces.addAll(Decks.pieces(works, DECK_KEYS, true));
        world.start(Village.start()).land(land()).pieces(pieces).rivers(Village.rivers()).ponds(Village.ponds())
                .ways(works.roads().stream().flatMap(r -> r.pavements().stream()).map(p -> (Volume) p).toList())
                .ruled(false);
    }

    /** Its roads' pavements and markings, each drawn once where one lies over another, in stretches, as they are seen. */
    @Override
    public Scenery scenery() {
        return RoadScenery.of(roads());
    }
}
