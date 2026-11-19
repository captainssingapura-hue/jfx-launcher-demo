package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.bench.kit.Bus;
import com.example.fxsuite.avatar.bench.kit.Guest;
import com.example.fxsuite.avatar.bench.kit.Orbit;
import com.example.fxsuite.avatar.bench.kit.Player;
import com.example.fxsuite.avatar.bench.kit.Shell;
import com.example.fxsuite.avatar.bench.kit.Workshop;
import com.example.fxsuite.avatar.build.Apartments;
import com.example.fxsuite.avatar.build.Building;
import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Palette;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.look.BuildingModel;
import com.example.fxsuite.avatar.look.FormModel;
import com.example.fxsuite.avatar.look.Stall;
import com.example.fxsuite.avatar.sim.Buildings;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Form;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The buildings bench: a building's shape on show on its own ({@link Buildings}), as its skeleton
 * ({@link FormModel}): every edge a thin rod, upright ones, level ones and sloped ones each in a
 * colour of their own, and, if asked, its vertices; on a floor ruled every 10 m, to tell how big it
 * is. Its figures read out: how many vertices and edges, its edges' length all told, how wide, deep
 * and high it is, how many pieces it is in, and what is wrong with it. Saved as text, to read and
 * edit by hand. Played, a building composed: walked round and into on level ground, a street along its front
 * ({@link BuildingSite}), with whoever is chosen to come.
 *
 * <p>{@code -Dbuilding.pick=church} (or the start of any building's name) picks what is on show to
 * start with; {@code -Dbuilding.facade=egg} and {@code -Dbuilding.ground=shop} (the start of an option's
 * name) how a composed one is dressed, and {@code -Dbuilding.materials=brick} what it is built in;
 * {@code -Dbuilding.tab=plan} opens on its floor plan ({@link PlanPanel}, for its options);
 * {@code -Dbuilding.yaw=-140}, {@code -Dbuilding.pitch=18} where it is seen from;
 * {@code -Dbuilding.distance} how far off, cm (by default, far enough to see all of it);
 * {@code -Dbuilding.scale=1.5} how big it is built (1, life size); {@code -Dbuilding.look=person}
 * starts by the person stood before it, of ordinary height, to tell how big it is;
 * {@code -Dbuilding.size=bays=9,width=14} sizes the church or the temple hall ({@link SizeControls});
 * {@code -Dbench.play=true} plays it as soon as it is shown, with whoever {@code -Dbench.guests} and
 * {@code -Dbench.people} bring ({@link Player});
 * {@code -Dbench.snapshot=out.png} saves the window after {@code -Dbench.settle} ms and exits.
 *
 * <p>Put together of the bench kit's parts ({@code bench.kit}): its window ({@link Shell}), its view ({@link Orbit}),
 * its building dressed, drawn and read out away from the screen ({@link Workshop}), asked and answered by message
 * ({@link Bus}), played ({@link Player}).
 */
public final class BuildingBench extends Application {

    /** The floor's rule: a line every 10 m. */
    private static final double RULE = 1000;

    private final Bus bus = new Bus();
    private final Shell shell = new Shell("Buildings bench", bus);
    private final Orbit view = new Orbit(Color.web("#c9dbe8"), Color.web("#9aa2ab"), Color.web("#8a8780"),
            new javafx.geometry.Point3D(0.4, 1, 0.6), 10, 1_000_000);
    private final Group shown = new Group();
    private final Group floor = new Group();
    private final ComboBox<Form> buildings = new ComboBox<>();
    /** A choice of options for each of a composed building's choices: its facade, its ground floor. */
    private final VBox choosing = new VBox(8);
    private final List<ComboBox<Building.Option>> picks = new java.util.ArrayList<>();
    /** What a composed building is built in. */
    private final ComboBox<Palette> palettes = new ComboBox<>();
    private final CheckBox showSkeleton = new CheckBox("Its skeleton");
    private final CheckBox showDressed = new CheckBox("Dressed: its surfaces' designs");
    private final CheckBox showVertices = new CheckBox("Vertices");
    private final CheckBox showFloor = new CheckBox("The floor, ruled every 10 m");
    /** Its roof's covering left off, to look in from over it ({@code -Dbuilding.roofless=true}). */
    private final CheckBox withoutRoof = new CheckBox("Without its roof: look in from over it");
    /** How big it is built: 1, life size; every way at once, its doors and windows with it. */
    private final javafx.scene.control.Spinner<Double> scaling = new javafx.scene.control.Spinner<>(0.25, 4,
            Double.parseDouble(System.getProperty("building.scale", "1")), 0.25);
    /** Someone stood before its front, of ordinary height, to tell how big it is. */
    private final CheckBox showPerson = new CheckBox(String.format(Locale.ROOT, "A person, %.2f m tall, before its front",
            Stall.PERSON / 100));
    private final Group person = new Group(Stall.person());
    /** How big the church or the temple hall is built: its bays, its heights. */
    private final SizeControls sizing = new SizeControls();
    /** The 3D view, and the floor plan's tab. */
    private final javafx.scene.control.TabPane tabs = new javafx.scene.control.TabPane();
    private final javafx.scene.control.Tab planTab = new javafx.scene.control.Tab("Plan (2D)");
    /** The tower's floor plan, worked on: the only building whose plan is, as yet. */
    private final PlanPanel towerPlan = new PlanPanel();
    private final Map<FormModel.Lean, PhongMaterial> colours = new EnumMap<>(FormModel.Lean.class);
    private final PhongMaterial dots = matte("#d64545");
    /** How a composed building is played: walked round and into, a car and a motorcycle parked on its street. */
    private final Player player = new Player(this::site, List.of(new Guest.Vehicle(Course.CAR_CHASSIS),
            new Guest.Vehicle(Course.BOXER)));
    private Form building;
    /** The building composed, for those that are: dressed as its options say. */
    private static final Map<String, Building> COMPOSED = new java.util.HashMap<>(
            Map.of("brutalist apartments", Apartments.building()));
    /** The buildings composed of parts, by name: their stacked parts' plans, in their plan tabs; how they were put together. */
    private static final Map<String, Map<String, com.example.fxsuite.avatar.build.Plan>> PARTS_PLANS =
            new java.util.HashMap<>(Map.of(
                    "town church", com.example.fxsuite.avatar.build.Church.standard().plans(),
                    "temple hall", com.example.fxsuite.avatar.build.Temple.standard().plans()));
    private static final Map<String, com.example.fxsuite.avatar.build.Assembly.Result> ASSEMBLED =
            new java.util.LinkedHashMap<>();

    static {
        ASSEMBLED.put("town church", com.example.fxsuite.avatar.build.Church.standard().assembled());
        ASSEMBLED.put("temple hall", com.example.fxsuite.avatar.build.Temple.standard().assembled());
        COMPOSED.put("town church", com.example.fxsuite.avatar.build.Church.standard().building());
        COMPOSED.put("temple hall", com.example.fxsuite.avatar.build.Temple.standard().building());
        // the pavilions, one of each kind
        com.example.fxsuite.avatar.build.Pavilions.PRESETS.forEach(p -> COMPOSED.put(p.name(),
                com.example.fxsuite.avatar.build.Pavilion.of(p).building()));
        // the wall tower, its walls bearing it, its legs meeting over its atrium
        COMPOSED.put(com.example.fxsuite.avatar.build.WallTower.NAME,
                com.example.fxsuite.avatar.build.WallTower.standard().building());
        // the hull to lie across their roofs, built as a ship is: keel, frames, stringers, beams, bulkheads, plating
        COMPOSED.put(com.example.fxsuite.avatar.build.SkyHull.NAME,
                com.example.fxsuite.avatar.build.SkyHull.standard().building());
        // and the park on its deck: a pitch, a pool, a court, a garden
        final com.example.fxsuite.avatar.build.SkyPark park = new com.example.fxsuite.avatar.build.SkyPark(
                com.example.fxsuite.avatar.build.SkyHull.standard());
        COMPOSED.put(com.example.fxsuite.avatar.build.SkyPark.NAME, com.example.fxsuite.avatar.build.SkyHull.standard()
                .opened(park.holes()).building(com.example.fxsuite.avatar.build.SkyPark.NAME, java.util.stream.Stream.concat(
                        park.solids().stream(), java.util.stream.Stream.of(park.waterSolid())).toList()));
        // the earth house, as homesteaders built them
        COMPOSED.put(com.example.fxsuite.avatar.build.EarthHouse.NAME,
                com.example.fxsuite.avatar.build.EarthHouse.standard().building());
    }

    /** The towers stacked from their plans, by name: their skeletons generated from them. */
    private static final Map<String, com.example.fxsuite.avatar.build.Tower> TOWERS = new java.util.LinkedHashMap<>();

    static {
        com.example.fxsuite.avatar.build.Towers.all().forEach(t -> {
            TOWERS.put(t.name(), t);
            COMPOSED.put(t.name(), dressed(t));
        });
    }
    /** {@code tower} as a building to dress: with a garden on its void deck, if it has one. */
    static Building dressed(final com.example.fxsuite.avatar.build.Tower tower) {
        final Building plain = com.example.fxsuite.avatar.build.TowerBuildings.of(tower);
        return tower.sections().stream().anyMatch(com.example.fxsuite.avatar.build.Tower.Section::open)
                ? com.example.fxsuite.avatar.build.DeckGarden.of(tower).on(plain) : plain;
    }

    private Building composed;
    private Stage stage;

    /**
     * What is asked of the workshop: the building on show, as composed, dressed with its options in its materials,
     * so big; what is shown of it; and what it is read out with (its tower's plans, how its parts were put together).
     */
    private record Asked(Form building, Building composed, List<Building.Option> chosen, Palette palette, double scale,
                         boolean dressed, boolean skeleton, boolean vertices, boolean roofless,
                         com.example.fxsuite.avatar.build.Tower tower,
                         com.example.fxsuite.avatar.build.Assembly.Result assembled) {
    }

    /** What the workshop made of it: the building dressed, its skeleton (either none, if not shown), its figures. */
    private record Made(Asked asked, Node dressed, Node skeleton, String figures) {
    }

    /**
     * Where the building is dressed, drawn and read out, away from the screen ({@link #make}): its solids, dressed as
     * chosen, made again only when the building, its options, its materials or its size change; drawn again only when
     * those, or its roof, do; its skeleton drawn again only when it, its size or its vertices do.
     */
    private final Workshop<Asked, Made> workshop = new Workshop<>("buildings bench", bus, Asked.class, this::make);
    private final Workshop<Asked, Made>.Stage<List<Solid>> solids = workshop.stage("dressed",
            a -> Workshop.same(a.composed(), a.chosen(), a.palette(), a.scale()),
            a -> a.composed() == null ? List.of() : a.composed().dressed(a.chosen(), a.palette(), a.scale()));
    private final Workshop<Asked, Made>.Stage<Node> model = workshop.stage("drawn",
            a -> Workshop.same(solids.of(a), a.roofless()), this::model);
    private final Workshop<Asked, Made>.Stage<Node> rods = workshop.stage("skeleton",
            a -> Workshop.same(a.building(), a.scale(), a.vertices()), this::skeleton);

    private static final Map<FormModel.Lean, String> SHADES = Map.of(
            FormModel.Lean.UPRIGHT, "#2f6fb3", FormModel.Lean.LEVEL, "#3b3f45", FormModel.Lean.SLOPED, "#c26a1b");

    @Override
    public void start(final Stage stage) {
        this.stage = stage;
        SHADES.forEach((lean, shade) -> colours.put(lean, matte(shade)));
        view.add(floor, shown, person);
        bus.subscribe(Made.class, this::show, Bus.SCREEN);

        // the buildings drawn by hand; then the towers stacked from their plans; then one stacked from the plan tab's plan
        final List<Form> all = new java.util.ArrayList<>(Buildings.all());
        ASSEMBLED.values().forEach(r -> all.add(r.form()));
        TOWERS.values().forEach(t -> all.add(t.skeleton()));
        com.example.fxsuite.avatar.build.Pavilions.PRESETS.forEach(p -> all.add(COMPOSED.get(p.name()).skeleton()));
        all.add(COMPOSED.get(com.example.fxsuite.avatar.build.EarthHouse.NAME).skeleton());
        all.add(COMPOSED.get(com.example.fxsuite.avatar.build.WallTower.NAME).skeleton());
        all.add(COMPOSED.get(com.example.fxsuite.avatar.build.SkyHull.NAME).skeleton());
        final Building fromPlan = com.example.fxsuite.avatar.build.TowerBuildings.of(towerPlan.tower());
        COMPOSED.put(PlanPanel.TOWER, fromPlan);
        all.add(fromPlan.skeleton());
        buildings.getItems().setAll(all);
        towerPlan.onChange(this::replan);
        sizing.onBuilt(this::resized);
        buildings.setConverter(new StringConverter<>() {
            @Override
            public String toString(final Form form) {
                return form == null ? "" : capitalised(form.name());
            }

            @Override
            public Form fromString(final String name) {
                return null;
            }
        });
        building = all.getFirst();
        final String picked = System.getProperty("building.pick");
        if (picked != null) {
            final String wanted = picked.toLowerCase(Locale.ROOT);
            building = all.stream().filter(f -> f.name().toLowerCase(Locale.ROOT).startsWith(wanted)
                    || f.name().toLowerCase(Locale.ROOT).contains(" " + wanted)).findFirst().orElse(building);
        }
        buildings.setValue(building);
        buildings.setOnAction(e -> {
            if (!replanning) {
                pick(buildings.getValue(), true);
            }
        });
        showSkeleton.setOnAction(e -> request());
        showDressed.setSelected(true);
        showDressed.setOnAction(e -> request());
        showFloor.setSelected(true);
        showVertices.setOnAction(e -> request());
        showFloor.setOnAction(e -> floor.setVisible(showFloor.isSelected()));
        showPerson.setSelected(true);
        showPerson.setOnAction(e -> person.setVisible(showPerson.isSelected()));
        withoutRoof.setSelected(Boolean.getBoolean("building.roofless"));
        withoutRoof.setOnAction(e -> request());
        scaling.setEditable(true);
        scaling.setPrefWidth(110);
        scaling.valueProperty().addListener((seen, was, now) -> {
            fit(false);
            request();
        });
        final Button nearPerson = new Button("Stand by the person");
        nearPerson.setOnAction(e -> lookAtPerson());
        final Label figures = shell.figures();
        figures.setFont(Font.font("Monospaced", 12));
        final Button save = new Button("Save as text…");
        save.setOnAction(e -> save());

        final VBox legend = new VBox(4);
        for (final FormModel.Lean lean : FormModel.Lean.values()) {
            final Rectangle swatch = new Rectangle(14, 14, Color.web(SHADES.get(lean)));
            legend.getChildren().add(new HBox(8, swatch, new Label(switch (lean) {
                case UPRIGHT -> "Upright edges: columns, piers, mullions";
                case LEVEL -> "Level edges: floors, eaves, sills, ridges";
                case SLOPED -> "Sloped edges: roofs, arches, spires";
            })));
        }
        final VBox controls = new VBox(10, EngineBench.heading("On show"), buildings,
                EngineBench.label("Its skeleton: vertices with no size, edges with no thickness, each drawn as a"
                        + " thin rod. A building composed has surfaces too, each dressed by a design: pick its facade"
                        + " and its ground floor to dress the same building another way, and what to build it in."
                        + " Doubtful choices are marked ? in its figures."),
                choosing, sizing, EngineBench.row("Scale (1 = life size)", scaling),
                EngineBench.label("Scaled, it is bigger every way: its doors, windows and floors with it, as a"
                        + " model is. The person stays life size."),
                showDressed, showSkeleton, withoutRoof,
                legend, showVertices, showFloor, new HBox(10, showPerson, nearPerson),
                EngineBench.label("Drag to turn round it; right-drag (or Shift-drag) to move across it; scroll to"
                        + " come nearer."),
                EngineBench.heading("Its figures"), shell.working(), figures, shell.problems(), save,
                EngineBench.heading("Play it"),
                EngineBench.label("A building composed, on level ground, a street along its front: walked round and"
                        + " into, as it is dressed."), player.panel());
        controls.setPadding(new Insets(14));
        controls.setPrefWidth(370);
        final ScrollPane scroll = new ScrollPane(controls);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(390);
        scroll.setMinWidth(390);
        // the building in 3D; and, in a tab of its own, its floor plan flat, for those whose plans are worked out
        final javafx.scene.control.Tab inThree = new javafx.scene.control.Tab("3D", view.node());
        inThree.setClosable(false);
        planTab.setClosable(false);
        tabs.getTabs().setAll(inThree, planTab);
        if ("plan".equalsIgnoreCase(System.getProperty("building.tab"))) {
            tabs.getSelectionModel().select(planTab);
        }
        shell.barless().left(scroll).middle(tabs);

        pick(building, false);
        view.turned(Double.parseDouble(System.getProperty("building.yaw", "-140")),
                Double.parseDouble(System.getProperty("building.pitch", "18")));
        final String distance = System.getProperty("building.distance");
        if ("person".equalsIgnoreCase(System.getProperty("building.look"))) {
            lookAtPerson();
        }
        if (distance != null) {
            view.distance(Double.parseDouble(distance));
        }
        shell.show(stage, 1260, 860);
        if (Boolean.getBoolean("bench.play")) {
            player.play();
        }
    }

    @Override
    public void stop() {
        workshop.close();
    }

    /** The building composed on show, as it is now dressed, as a world to play; none for a building only a skeleton. */
    private BuildingSite site() {
        return composed == null ? null
                : new BuildingSite(building.name(), composed.dressed(chosen(), palettes.getValue(), scale()));
    }

    /** Another building on show: seen whole, looked at about its middle, if {@code frame}d afresh. */
    private void pick(final Form picked, final boolean frame) {
        building = picked;
        composed = COMPOSED.get(building.name());
        // its floor plan, flat: the tower's; empty, for the rest, until theirs are worked out
        planTab.setContent(PlanPanel.TOWER.equals(building.name()) ? towerPlan
                : TOWERS.containsKey(building.name()) ? new TowerPlans(TOWERS.get(building.name()))
                : PARTS_PLANS.containsKey(building.name()) ? new PartsPlans(PARTS_PLANS.get(building.name())) : new Pane());
        choices();
        sizing.show(building.name());
        showDressed.setDisable(composed == null);
        showSkeleton.setDisable(composed == null);
        showSkeleton.setSelected(composed == null);
        fit(frame);
        request();
    }

    /** How big it is built: 1, life size. */
    private double scale() {
        final Double value = scaling.getValue();
        return value == null || !(value > 0) ? 1 : value;
    }

    /** Its skeleton, as big as it is built. */
    private Form scaled() {
        return building.scaled(scale());
    }

    /**
     * Seen whole, looked at about its middle, and seen from the usual side again if {@code frame}d
     * afresh; the floor ruled out round it; the person stood a couple of metres before the middle of
     * its front, facing out.
     */
    private void fit(final boolean frame) {
        final Form.Bounds box = scaled().bounds();
        final double whole = Math.max(box.height(), Math.max(box.width(), box.depth())) * 1.9 + 800;
        view.lookAt((box.leftmost() + box.rightmost()) / 2, -(box.low() + box.high()) / 2,
                (box.back() + box.front()) / 2);
        view.distance(whole);
        view.zoom(Math.max(100, whole / 40), whole * 4);
        if (frame) {
            view.turned(-140, 18);
        }
        person.setTranslateX((box.leftmost() + box.rightmost()) / 2);
        person.setTranslateZ(box.front() + 250);
        ruleFloor(box);
    }

    /** Looked at from close by the person, at their height, the building rising behind them. */
    private void lookAtPerson() {
        view.lookAt(person.getTranslateX(), -Stall.PERSON * 0.6, person.getTranslateZ());
        view.distance(3000);
        view.turned(-160, 7);
    }

    /**
     * A pick of options for each of the composed building's choices, its own first; or, asked for
     * ({@code -Dbuilding.facade}, {@code -Dbuilding.ground}: the first word of a choice), the option
     * whose name starts so. None, for a building only a skeleton.
     */
    private void choices() {
        picks.clear();
        choosing.getChildren().clear();
        if (composed == null) {
            return;
        }
        composed.choices().forEach((choice, options) -> {
            final ComboBox<Building.Option> pick = new ComboBox<>();
            pick.setConverter(new StringConverter<>() {
                @Override
                public String toString(final Building.Option option) {
                    return option == null ? "" : option.name();
                }

                @Override
                public Building.Option fromString(final String name) {
                    return null;
                }
            });
            pick.getItems().setAll(options);
            pick.setValue(options.getFirst());
            final String asked = System.getProperty("building." + choice.split(" ")[0].toLowerCase(Locale.ROOT));
            if (asked != null) {
                options.stream().filter(o -> o.name().toLowerCase(Locale.ROOT).startsWith(asked.toLowerCase(Locale.ROOT)))
                        .findFirst().ifPresent(pick::setValue);
            }
            pick.setOnAction(e -> request());
            picks.add(pick);
            choosing.getChildren().add(EngineBench.row(choice, pick));
        });
        palettes.setConverter(new StringConverter<>() {
            @Override
            public String toString(final Palette palette) {
                return palette == null ? "" : palette.name();
            }

            @Override
            public Palette fromString(final String name) {
                return null;
            }
        });
        palettes.getItems().setAll(composed.palettes());
        palettes.setValue(composed.palettes().getFirst());
        final String material = System.getProperty("building.materials");
        if (material != null) {
            composed.palettes().stream().filter(p -> p.name().toLowerCase(Locale.ROOT)
                    .startsWith(material.toLowerCase(Locale.ROOT))).findFirst().ifPresent(palettes::setValue);
        }
        palettes.setOnAction(e -> request());
        choosing.getChildren().add(EngineBench.row("Materials", palettes));
    }

    /** The options picked, one of each choice. */
    private List<Building.Option> chosen() {
        return picks.stream().map(ComboBox::getValue).toList();
    }

    /** Asks the workshop for the building as the bench now has it: drawn afresh where it has changed, read out. */
    private void request() {
        final com.example.fxsuite.avatar.build.Tower tower = PlanPanel.TOWER.equals(building.name()) ? towerPlan.tower()
                : TOWERS.get(building.name());
        bus.publish(new Asked(building, composed, List.copyOf(chosen()), palettes.getValue(), scale(),
                composed != null && showDressed.isSelected(), composed == null || showSkeleton.isSelected(),
                showVertices.isSelected(), withoutRoof.isSelected(), tower, ASSEMBLED.get(building.name())));
    }

    /** On the workshop's worker: the building drawn as asked, each stage again only if what it is made from changed. */
    private Made make(final Asked a) {
        return new Made(a, a.dressed() ? model.of(a) : null, a.skeleton() ? rods.of(a) : null, figures(a));
    }

    /** On the screen's thread: what the workshop made, shown. */
    private void show(final Made m) {
        final List<Node> drawn = new ArrayList<>();
        if (m.dressed() != null) {
            drawn.add(m.dressed());
        }
        if (m.skeleton() != null) {
            drawn.add(m.skeleton());
        }
        shown.getChildren().setAll(drawn);
        bus.publish(new Bus.Figures(m.figures()));
        bus.publish(new Bus.Problems(""));
    }

    /** The building dressed: without its roof, its covering and what grows on it left off, what holds it up left. */
    private Node model(final Asked a) {
        final List<Solid> dressed = solids.of(a);
        return BuildingModel.of(a.roofless() ? dressed.stream()
                .filter(s -> s.role() != com.example.fxsuite.avatar.build.Role.ROOF && !s.name().startsWith("roof"))
                .toList() : dressed);
    }

    /** Its skeleton, its edges as thick as suits its size: thin, but never too thin to see. */
    private Node skeleton(final Asked a) {
        final Form skeleton = a.building().scaled(a.scale());
        final Form.Bounds box = skeleton.bounds();
        final double size = Math.max(box.height(), Math.max(box.width(), box.depth()));
        return FormModel.of(skeleton, Math.max(5, size / 900), colours, a.vertices(), dots);
    }

    /** The floor under it, well beyond it all round, ruled every 10 m. */
    private void ruleFloor(final Form.Bounds box) {
        final double reach = Math.ceil((Math.max(box.width(), box.depth()) / 2 + 3000) / RULE) * RULE;
        final Box ground = new Box(reach * 2, 2, reach * 2);
        ground.setTranslateY(1.5);
        ground.setMaterial(matte("#9fae8f"));
        floor.getChildren().setAll(ground);
        final PhongMaterial line = matte("#7d8b70");
        for (double at = -reach; at <= reach + 1e-6; at += RULE) {
            final Box across = new Box(reach * 2, 1, 12);
            across.setTranslateZ(at);
            final Box along = new Box(12, 1, reach * 2);
            along.setTranslateX(at);
            across.setMaterial(line);
            along.setMaterial(line);
            floor.getChildren().addAll(across, along);
        }
    }

    /** Whether the tower from the plan tab is being made afresh: the picker's value set, not picked. */
    private boolean replanning;

    /**
     * The church or the temple hall built again to another size: put in place of what it was, its
     * plans in its plan tab, and shown, if it is on show, seen whole again; dressed as it was.
     */
    private void resized(final SizeControls.Built built) {
        COMPOSED.put(built.name(), built.building());
        // a tower, stacked from its plans; one put together from parts, its plans with it; one made whole (a house), its own skeleton
        final boolean whole = built.tower() == null && built.assembled() == null;
        if (built.tower() != null) {
            TOWERS.put(built.name(), built.tower());
        } else if (!whole) {
            ASSEMBLED.put(built.name(), built.assembled());
            PARTS_PLANS.put(built.name(), built.plans());
        }
        final Form made = built.tower() != null ? built.tower().skeleton()
                : whole ? built.building().skeleton() : built.assembled().form();
        final int k = buildings.getItems().indexOf(buildings.getItems().stream()
                .filter(f -> f.name().equals(built.name())).findFirst().orElse(null));
        if (k < 0) {
            return;
        }
        final boolean shown = building != null && built.name().equals(building.name());
        replanning = true;
        try {
            buildings.getItems().set(k, made);
            buildings.setValue(shown ? made : building);
        } finally {
            replanning = false;
        }
        if (shown) {
            building = made;
            composed = built.building();
            planTab.setContent(built.tower() != null ? new TowerPlans(built.tower())
                    : whole ? new Pane() : new PartsPlans(built.plans()));
            fit(false);
            request();
        }
    }

    /** The plan tab's plan changed: the tower stacked from it made afresh, and shown, if it is on show. */
    private void replan() {
        final Building rebuilt = com.example.fxsuite.avatar.build.TowerBuildings.of(towerPlan.tower());
        COMPOSED.put(PlanPanel.TOWER, rebuilt);
        final Form made = rebuilt.skeleton();
        final int k = buildings.getItems().indexOf(buildings.getItems().stream()
                .filter(f -> f.name().equals(PlanPanel.TOWER)).findFirst().orElse(null));
        if (k < 0) {
            return;
        }
        final boolean shown = building != null && PlanPanel.TOWER.equals(building.name());
        replanning = true;
        try {
            buildings.getItems().set(k, made);
            buildings.setValue(shown ? made : building);
        } finally {
            replanning = false;
        }
        if (shown) {
            building = made;
            composed = rebuilt;
            request();
        }
    }

    /** Its figures, read out: as big as it is built. */
    private String figures(final Asked a) {
        final double scale = a.scale();
        final Form building = a.building().scaled(scale);
        final Building composed = a.composed();
        final Form.Bounds box = building.bounds();
        final List<String> problems = building.problems();
        final StringBuilder out = new StringBuilder();
        if (scale != 1) {
            out.append(String.format(Locale.ROOT, "built %.2f times life size%n", scale));
        }
        out.append(String.format("%d vertices, %d edges%n", building.vertices().size(), building.edges().size()));
        out.append(String.format("%.0f m of edges all told%n", building.length() / 100));
        out.append(String.format("%.1f m wide, %.1f m deep, %.1f m high%n", box.width() / 100, box.depth() / 100,
                box.height() / 100));
        final Map<FormModel.Lean, Integer> leaning = new EnumMap<>(FormModel.Lean.class);
        for (final Form.Edge edge : building.edges()) {
            leaning.merge(FormModel.lean(building, edge), 1, Integer::sum);
        }
        out.append(String.format("%d upright, %d level, %d sloped%n", leaning.getOrDefault(FormModel.Lean.UPRIGHT, 0),
                leaning.getOrDefault(FormModel.Lean.LEVEL, 0), leaning.getOrDefault(FormModel.Lean.SLOPED, 0)));
        out.append(building.pieces() == 1 ? "all one piece\n"
                : String.format("in %d pieces: its frame, and outlines standing apart%n", building.pieces()));
        if (composed != null) {
            final List<Solid> solids = this.solids.of(a);
            final Map<Material, Integer> made = new EnumMap<>(Material.class);
            solids.forEach(s -> made.merge(s.material(), 1, Integer::sum));
            out.append(String.format("%d surfaces and %d faces, dressed in %d solids:%n", composed.surfaces().size()
                    + composed.plates().size(), composed.sheets().size(), solids.size()));
            made.forEach((material, count) -> out.append(String.format("  %d %s%n", count, material.title())));
            final List<String> doubts = composed.doubts(a.chosen(), a.palette());
            doubts.forEach(d -> out.append("? ").append(d).append('\n'));
        }
        out.append(problems.isEmpty() ? "its skeleton: nothing wrong with it" : "its skeleton:");
        problems.stream().limit(12).forEach(p -> out.append("\n! ").append(p));
        if (problems.size() > 12) {
            out.append("\n! and ").append(problems.size() - 12).append(" more");
        }
        final com.example.fxsuite.avatar.build.Tower tower = a.tower();
        if (a.assembled() != null) {
            final List<String> joints = a.assembled().problems();
            return out + (joints.isEmpty() ? "\nits parts all meet where they mean to"
                    : joints.stream().map(p -> "\n! " + p).reduce("", String::concat));
        }
        if (tower != null) {
            return out + String.format(Locale.ROOT, "%n%d floors, %.1f m to its roof, in %d section%s", tower.floors(),
                    tower.height() * scale / 100, tower.sections().size(), tower.sections().size() == 1 ? "" : "s")
                    + tower.problems().stream().map(p -> "\n! " + p).reduce("", String::concat)
                    + (composed == null ? "" : com.example.fxsuite.avatar.build.TowerBuildings.mismatches(composed, tower)
                            .stream().limit(8).map(p -> "\n! " + p).reduce("", String::concat));
        }
        return out.toString();
    }

    private void save() {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Save the building as text");
        chooser.setInitialFileName(building.name().replace(' ', '-') + ".form");
        final File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), building.text());
        } catch (IOException e) {
            bus.publish(new Bus.Problems("Could not save: " + e.getMessage()));
        }
    }

    private static PhongMaterial matte(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        material.setSpecularColor(Color.web("#3a3f46"));
        return material;
    }

    private static String capitalised(final String name) {
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
