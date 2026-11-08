package com.example.fxsuite.deployment;

import com.example.fxsuite.deployment.DependenciesTest.Core;
import com.example.fxsuite.deployment.DependenciesTest.Json;
import com.example.fxsuite.deployment.DependenciesTest.Ledger;
import com.example.fxsuite.deployment.DependenciesTest.Web;

import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolutionTest {

    /** Asks for a newer Web than Core does, which in turn needs a newer Json. */
    record Raiser() implements Application {
        record V1_20261110_0() implements PhysicalVersion<Raiser> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 10)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() {
                return new Dependencies(new Core.V8_20261001_0(), new Web.V1_20261020_0());
            }
        }
    }

    /** Needs Json on line 3 while Core needs it on line 2. */
    record Torn() implements Application {
        record V1_20261210_0() implements PhysicalVersion<Torn> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 12, 10)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() {
                return new Dependencies(new Core.V8_20261001_0(), new Json.V3_20261201_0());
            }
        }
    }

    /** Two virtual versions based on each other, so no physical release behind either. */
    record LoopLib() implements Library {
        record V1_20261001_1() implements VirtualVersion<LoopLib, V1_20261001_2> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
            public V1_20261001_2 base() { return new V1_20261001_2(); }
            public Dependencies changes() { return new Dependencies(); }
        }
        record V1_20261001_2() implements VirtualVersion<LoopLib, V1_20261001_1> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(2); }
            public V1_20261001_1 base() { return new V1_20261001_1(); }
            public Dependencies changes() { return new Dependencies(); }
        }
    }

    /** A checksum two steps down the chain still has to match the physical build. */
    record ChainChecksumLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<ChainChecksumLib>, IncludeChecksum {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(); }
            public Sha256 checksum() { return new Sha256("d".repeat(64)); }
        }
        record V1_20261001_1() implements VirtualVersion<ChainChecksumLib, V1_20261001_0> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
            public V1_20261001_0 base() { return new V1_20261001_0(); }
            public Dependencies changes() { return new Dependencies(); }
        }
        record V1_20261001_2() implements VirtualVersion<ChainChecksumLib, V1_20261001_1>, IncludeChecksum {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(2); }
            public V1_20261001_1 base() { return new V1_20261001_1(); }
            public Dependencies changes() { return new Dependencies(); }
            public Sha256 checksum() { return new Sha256("e".repeat(64)); }
        }
    }

    private static final Resolver RESOLVER = new Resolver();
    private static final VersionChecker CHECKER = new VersionChecker();

    private static String onlyWarningAbout(Resolution resolution, String unit) {
        List<String> about = resolution.warnings().items().stream()
                .map(Warning::message).filter(m -> m.contains(": " + unit + " is requested at")).toList();
        assertEquals(1, about.size(), resolution.warnings().toString());
        return about.get(0);
    }

    // ---- virtual versions based on virtual versions

    @Test
    void aVirtualVersionBuildsOnItsBaseWithoutRepeatingItsChanges() {
        assertEquals(new Dependencies(new Json.V2_20261105_0(), new Web.V1_20261020_0()),
                new Core.V8_20261001_2().dependencies());
        assertEquals(new Core.V8_20261001_0(), new Lineage().physicalOf(new Core.V8_20261001_2()));
        assertEquals(List.of(), CHECKER.check(Core.class));
    }

    @Test
    void aChainOfBasesMustEndAtAPhysicalRelease() {
        List<Violation> violations = CHECKER.check(LoopLib.class);
        assertEquals(2, violations.size(), violations.toString());
        assertTrue(violations.stream().allMatch(v -> v.message().contains("loop back")), violations.toString());
    }

    @Test
    void aChecksumIsComparedWithThePhysicalBuildAtTheEndOfTheChain() {
        List<Violation> violations = CHECKER.check(ChainChecksumLib.class);
        assertTrue(violations.stream().anyMatch(v -> v.message().contains(
                "V1_20261001_2: its checksum differs from the physical release 1.20261001.0")), violations.toString());
    }

    // ---- resolution

    @Test
    void agreeingRequestsResolveWithoutWarnings() {
        Resolution resolution = RESOLVER.resolve(new Ledger.V1_20261108_0());
        assertTrue(resolution.isValid());
        assertEquals(new Dependencies(new Core.V8_20261001_0(), new Json.V2_20260920_0(), new Web.V1_20260925_0()),
                resolution.resolved());
        assertTrue(resolution.warnings().isEmpty(), resolution.warnings().toString());
    }

    @Test
    void twoVersionsOnOneLineResolveToTheHigherWithAWarning() {
        Resolution resolution = RESOLVER.resolve(new Ledger.V1_20261108_1());
        assertTrue(resolution.isValid());
        assertEquals(new Dependencies(new Core.V8_20261001_1(), new Json.V2_20261105_0(), new Web.V1_20260925_0()),
                resolution.resolved());
        assertEquals("Ledger 1.20261108.1: Json is requested at 2.20260920.0 (by Web 1.20260925.0), "
                        + "2.20261105.0 (by Core 8.20261001.1); using 2.20261105.0. "
                        + "Move the others to 2.20261105.0 so every release runs with the version it declares",
                onlyWarningAbout(resolution, "Json"));
    }

    @Test
    void theWarningIsRaisedWhenTheLibraryIsReleasedNotOnlyWhenAnAppUsesIt() {
        List<Warning> warnings = CHECKER.warnings(Core.class);
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).message().startsWith("Core 8.20261001.1: Json is requested at"),
                warnings.toString());
    }

    @Test
    void movingTheLaggingDependencyForwardClearsTheWarning() {
        Resolution resolution = RESOLVER.resolve(new Core.V8_20261001_2());
        assertTrue(resolution.warnings().isEmpty(), resolution.warnings().toString());
    }

    @Test
    void raisingOneVersionBringsItsOwnRequestsAndDropsTheOldOnes() {
        Resolution resolution = RESOLVER.resolve(new Raiser.V1_20261110_0());
        assertTrue(resolution.isValid());
        // Web raised by the app's request; Json then raised by the newer Web's request.
        assertEquals(new Dependencies(new Core.V8_20261001_0(), new Web.V1_20261020_0(), new Json.V2_20261105_0()),
                resolution.resolved());
        assertTrue(onlyWarningAbout(resolution, "Web").contains(
                "1.20260925.0 (by Core 8.20261001.0), 1.20261020.0 (by Raiser 1.20261110.0); using 1.20261020.0"));
        // Web 1.20260925.0 was raised past, so its own request for Json no longer counts.
        String json = onlyWarningAbout(resolution, "Json");
        assertTrue(json.contains("2.20260920.0 (by Core 8.20261001.0), 2.20261105.0 (by Web 1.20261020.0)"), json);
        assertFalse(json.contains("Web 1.20260925.0"), json);
    }

    @Test
    void twoLinesOfOneUnitCannotBeResolved() {
        Resolution resolution = RESOLVER.resolve(new Torn.V1_20261210_0());
        assertFalse(resolution.isValid());
        assertEquals(1, resolution.conflicts().items().size(), resolution.conflicts().toString());
        String conflict = resolution.conflicts().items().get(0).message();
        assertTrue(conflict.contains("needs Json on more than one line: 2.20260920.0 (by Core 8.20261001.0, "
                + "Web 1.20260925.0), 3.20261201.0 (by Torn 1.20261210.0)"), conflict);
        assertTrue(CHECKER.check(Torn.class).stream().anyMatch(v -> v.message().contains("more than one line")));
    }

    @Test
    void theNewTypesMeetTheOntologyContracts() {
        OntologyEnforcer enforcer = new OntologyEnforcer();
        for (Class<?> type : List.of(Resolver.class, Resolution.class, Sequence.class, Warning.class,
                Lineage.class, VersionText.class, Core.V8_20261001_2.class)) {
            assertEquals(List.of(), enforcer.enforce(type), type.getName());
        }
    }
}
