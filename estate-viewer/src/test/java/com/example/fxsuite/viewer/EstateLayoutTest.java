package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.DeploymentUnit;
import com.example.fxsuite.deployment.Library;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.estate.AcmeCharts;
import com.example.fxsuite.estate.AcmeCore;
import com.example.fxsuite.estate.AcmeFx;
import com.example.fxsuite.estate.AcmeJson;
import com.example.fxsuite.estate.AcmeNet;
import com.example.fxsuite.estate.AcmeText;
import com.example.fxsuite.estate.AcmeUi;
import com.example.fxsuite.estate.AcmeJsonGuava;
import com.example.fxsuite.estate.AcmeOntology;
import com.example.fxsuite.estate.DemoEstate;
import com.example.fxsuite.estate.Hello;
import com.example.fxsuite.estate.Ledger;
import com.example.fxsuite.viewer.EstateLayout.Placed;
import com.example.fxsuite.viewer.Highlighter.Highlight;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstateLayoutTest {

    private static final Releases RELEASES = new Releases(new DemoEstate().units());
    private static final EstateLayout LAYOUT = new EstateLayout(RELEASES.all());

    private static Placed at(Object version) {
        return LAYOUT.placed().stream().filter(p -> p.version().equals(version)).findFirst().orElseThrow();
    }

    @Test
    void everyReleaseAndDeclaredDependencyIsPlaced() {
        assertEquals(44, LAYOUT.placed().size());
        assertEquals(78, LAYOUT.edges().size());
        assertEquals(16, LAYOUT.columns().size());
    }

    @Test
    void depthIsOneMoreThanTheDeepestDependency() {
        assertEquals(0, at(new AcmeOntology.V1_20260901_0()).depth());
        assertEquals(0, at(new AcmeFx.V1_20261102_0()).depth());
        assertEquals(1, at(new AcmeCore.V8_20261001_0()).depth());
        assertEquals(1, at(new AcmeJsonGuava.V1_20261006_0()).depth());
        assertEquals(1, at(new Hello.V1_20261003_1()).depth());
        assertEquals(3, at(new AcmeUi.V1_20261021_0()).depth());
        assertEquals(4, at(new AcmeCharts.V2_20261022_1()).depth());
        assertEquals(5, at(new Ledger.V1_20261101_1()).depth());
        assertEquals(5, LAYOUT.maxDepth());
        for (EstateLayout.Edge edge : LAYOUT.edges()) {
            assertTrue(edge.from().depth() > edge.to().depth(), edge.toString());
        }
    }

    @Test
    void eachUnitKeepsOneLaneAndEachLayerIsCentred() {
        Map<Class<?>, Set<Double>> lanesByUnit = LAYOUT.placed().stream().collect(Collectors.groupingBy(
                Placed::unit, Collectors.mapping(Placed::lane, Collectors.toSet())));
        lanesByUnit.forEach((unit, lanes) -> assertEquals(1, lanes.size(), unit.getName()));

        // depth 0 holds five bundles, spread -2..2; the lone unit in front sits in the middle
        Map<Integer, Set<Double>> lanesByDepth = LAYOUT.placed().stream().collect(Collectors.groupingBy(
                Placed::depth, Collectors.mapping(Placed::lane, Collectors.toSet())));
        assertEquals(Set.of(-2.0, -1.0, 0.0, 1.0, 2.0), lanesByDepth.get(0));
        assertEquals(Set.of(-1.5, -0.5, 0.5, 1.5), lanesByDepth.get(1));
        assertEquals(Set.of(0.0), lanesByDepth.get(5));
        assertEquals(Ledger.class, LAYOUT.columns().get(LAYOUT.columns().size() - 1));
    }

    @Test
    void releasesAreStackedOldestFirst() {
        assertEquals(0, at(new AcmeCore.V8_20261001_0()).row());
        assertEquals(3, at(new AcmeCore.V8_20261106_1()).row());
        assertEquals(5, at(new AcmeCore.V9_20261120_0()).row());
        assertEquals(5, LAYOUT.maxRow());
    }

    @Test
    void virtualReleasesArePlacedLikeAnyOtherButMarked() {
        assertTrue(at(new AcmeText.V3_20261020_1()).isVirtual());
        assertFalse(at(new AcmeText.V3_20261020_0()).isVirtual());
        assertEquals(EstateLayout.Kind.LIBRARY, at(new AcmeText.V3_20261020_1()).kind());
        assertEquals(EstateLayout.Kind.BUNDLE, at(new AcmeJson.V2_20261105_0()).kind());
        assertEquals(EstateLayout.Kind.APPLICATION, at(new Ledger.V1_20261101_0()).kind());
    }

    // ---- highlighting

    @Test
    void selectingShowsTheResolvedGraphAndWhatWasRaisedPast() {
        Highlight highlight = new Highlighter().of(LAYOUT, new Ledger.V1_20261101_0());
        assertTrue(highlight.members().contains(new AcmeCore.V8_20261015_0()));
        assertFalse(highlight.members().contains(new AcmeCore.V8_20261001_0()));
        // AcmeText asked for AcmeCore 8.20261001.0, which the diamond raised past.
        assertEquals(Set.of(new AcmeCore.V8_20261001_0()), highlight.passedOver());
        assertEquals(1, highlight.raised().size());
        assertEquals(new AcmeText.V3_20261020_0(), highlight.raised().iterator().next().from().version());
        assertEquals(1, highlight.resolution().warnings().items().size());
    }

    @Test
    void aGraphWithoutWarningsRaisesNothing() {
        Highlight highlight = new Highlighter().of(LAYOUT, new Ledger.V1_20261101_1());
        assertTrue(highlight.passedOver().isEmpty());
        assertTrue(highlight.raised().isEmpty());
        assertTrue(highlight.members().contains(new AcmeNet.V5_20261016_1()));
        // every lit edge joins two members
        Set<Object> members = new HashSet<>(highlight.members());
        highlight.lit().forEach(e -> {
            assertTrue(members.contains(e.from().version()));
            assertTrue(members.contains(e.to().version()));
        });
    }

    // ---- one application's dependencies

    @Test
    void anAppsClosureIsEverythingItsDeclarationsReach() {
        List<Object> closure = List.copyOf(RELEASES.closure(new Ledger.V1_20261101_0()));
        assertEquals(12, closure.size(), closure.toString());
        assertEquals(new Ledger.V1_20261101_0(), closure.get(closure.size() - 1));
        // both sides of the diamond, including the AcmeCore that resolution raises past
        assertTrue(closure.contains(new AcmeCore.V8_20261001_0()));
        assertTrue(closure.contains(new AcmeCore.V8_20261015_0()));
        assertFalse(closure.contains(new AcmeCore.V9_20261110_0()));
        assertFalse(closure.contains(new AcmeJsonGuava.V1_20261006_0()));
    }

    @Test
    void anAppsLayoutKeepsDepthsButCompactsRowsAndLanes() {
        EstateLayout app = new EstateLayout(RELEASES.closure(new Ledger.V1_20261101_0()));
        assertEquals(12, app.placed().size());
        assertEquals(5, app.maxDepth());
        Placed ledger = app.of(new Ledger.V1_20261101_0()).orElseThrow();
        assertEquals(5, ledger.depth());
        assertEquals(0, ledger.row());
        // only two AcmeCore releases are in this graph, so rows 0 and 1
        assertEquals(1, app.of(new AcmeCore.V8_20261015_0()).orElseThrow().row());
        assertEquals(1, app.maxRow());
        // depth 0 here holds AcmeOntology, AcmeFx, AcmeJson and AcmeGuava
        assertEquals(Set.of(-1.5, -0.5, 0.5, 1.5), app.placed().stream().filter(p -> p.depth() == 0)
                .map(Placed::lane).collect(Collectors.toSet()));
    }

    @Test
    void theApplicationsAreTheAppUnitsInCatalogueOrder() {
        assertEquals(List.of(Hello.class, Ledger.class, com.example.fxsuite.estate.Atlas.class,
                com.example.fxsuite.estate.Courier.class), RELEASES.applications());
        assertEquals(List.of(new Ledger.V1_20261101_0(), new Ledger.V1_20261101_1()), RELEASES.of(Ledger.class));
    }

    // ---- a cycle leaves releases with no depth

    record LoopA() implements Library {
        record V1_20261001_0() implements PhysicalVersion<LoopA> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(new LoopB.V1_20261002_0()); }
        }
    }

    record LoopB() implements Library {
        record V1_20261002_0() implements PhysicalVersion<LoopB> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 2)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(new LoopA.V1_20261001_0()); }
        }
    }

    @Test
    void aDependencyCycleIsReportedInsteadOfLoopingForever() {
        List<Class<? extends DeploymentUnit>> units = List.of(LoopA.class, LoopB.class);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new EstateLayout(new Releases(units).all()));
        assertTrue(e.getMessage().contains("dependency cycle"), e.getMessage());
    }
}
