package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependenciesTest {

    // ---- a small valid estate: two bundles, a library and an application

    record Json() implements ExternalBundle {
        record V2_20260920_0() implements PhysicalVersion<Json>, BundleRelease {
            public Monotonic monotonic() { return new Monotonic(2); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 9, 20)); }
            public Patch patch() { return new Patch(0); }
            public Members members() { return new Members(new ExternalArtifact("org.example.upstream", "json", "2.20260920.0")); }
            public Dependencies dependencies() { return new Dependencies(); }
        }
        record V2_20261105_0() implements PhysicalVersion<Json>, BundleRelease {
            public Monotonic monotonic() { return new Monotonic(2); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 5)); }
            public Patch patch() { return new Patch(0); }
            public Members members() { return new Members(new ExternalArtifact("org.example.upstream", "json", "2.20261105.0")); }
            public Dependencies dependencies() { return new Dependencies(); }
        }
        record V3_20261201_0() implements PhysicalVersion<Json>, BundleRelease {
            public Monotonic monotonic() { return new Monotonic(3); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 12, 1)); }
            public Patch patch() { return new Patch(0); }
            public Members members() { return new Members(new ExternalArtifact("org.example.upstream", "json", "3.20261201.0")); }
            public Dependencies dependencies() { return new Dependencies(); }
        }
    }

    /** A bundle that builds on another bundle. */
    record Web() implements ExternalBundle {
        record V1_20260925_0() implements PhysicalVersion<Web>, BundleRelease {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 9, 25)); }
            public Patch patch() { return new Patch(0); }
            public Members members() { return new Members(new ExternalArtifact("org.example.upstream", "web", "1.20260925.0")); }
            public Dependencies dependencies() { return new Dependencies(new Json.V2_20260920_0()); }
        }
        record V1_20261020_0() implements PhysicalVersion<Web>, BundleRelease {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 20)); }
            public Patch patch() { return new Patch(0); }
            public Members members() { return new Members(new ExternalArtifact("org.example.upstream", "web", "1.20261020.0")); }
            public Dependencies dependencies() { return new Dependencies(new Json.V2_20261105_0()); }
        }
    }

    record Core() implements Library {
        record V8_20261001_0() implements PhysicalVersion<Core> {
            public Monotonic monotonic() { return new Monotonic(8); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() {
                return new Dependencies(new Json.V2_20260920_0(), new Web.V1_20260925_0());
            }
        }
        /** Same binary, Json moved forward within its line, Web kept. */
        record V8_20261001_1() implements VirtualVersion<Core, V8_20261001_0> {
            public Monotonic monotonic() { return new Monotonic(8); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
            public V8_20261001_0 base() { return new V8_20261001_0(); }
            public Dependencies changes() { return new Dependencies(new Json.V2_20261105_0()); }
        }
        /** Builds on .1: keeps its Json change without repeating it, and moves Web too. */
        record V8_20261001_2() implements VirtualVersion<Core, V8_20261001_1> {
            public Monotonic monotonic() { return new Monotonic(8); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(2); }
            public V8_20261001_1 base() { return new V8_20261001_1(); }
            public Dependencies changes() { return new Dependencies(new Web.V1_20261020_0()); }
        }
    }

    record Ledger() implements Application {
        record V1_20261108_0() implements PhysicalVersion<Ledger> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 8)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(new Core.V8_20261001_0()); }
        }
        record V1_20261108_1() implements VirtualVersion<Ledger, V1_20261108_0> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 8)); }
            public Patch patch() { return new Patch(1); }
            public V1_20261108_0 base() { return new V1_20261108_0(); }
            public Dependencies changes() { return new Dependencies(new Core.V8_20261001_1()); }
        }
    }

    // ---- units that each break one rule

    record SelfDependentLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<SelfDependentLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(); }
        }
        record V1_20261002_0() implements PhysicalVersion<SelfDependentLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 2)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(new V1_20261001_0()); }
        }
    }

    record BundleOnLibrary() implements ExternalBundle {
        record V1_20261002_0() implements PhysicalVersion<BundleOnLibrary>, BundleRelease {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 2)); }
            public Patch patch() { return new Patch(0); }
            public Members members() { return new Members(new ExternalArtifact("org.example.upstream", "bundleonlibrary", "1.20261002.0")); }
            public Dependencies dependencies() { return new Dependencies(new Core.V8_20261001_0()); }
        }
    }

    record VirtualAddsLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<VirtualAddsLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(new Json.V2_20260920_0()); }
        }
        record V1_20261001_1() implements VirtualVersion<VirtualAddsLib, V1_20261001_0> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
            public V1_20261001_0 base() { return new V1_20261001_0(); }
            public Dependencies changes() { return new Dependencies(new Web.V1_20260925_0()); }
        }
    }

    record VirtualNewLineLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<VirtualNewLineLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(new Json.V2_20261105_0()); }
        }
        record V1_20261001_1() implements VirtualVersion<VirtualNewLineLib, V1_20261001_0> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
            public V1_20261001_0 base() { return new V1_20261001_0(); }
            public Dependencies changes() { return new Dependencies(new Json.V3_20261201_0()); }
        }
    }

    record VirtualDowngradeLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<VirtualDowngradeLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(new Json.V2_20261105_0()); }
        }
        record V1_20261001_1() implements VirtualVersion<VirtualDowngradeLib, V1_20261001_0> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
            public V1_20261001_0 base() { return new V1_20261001_0(); }
            public Dependencies changes() { return new Dependencies(new Json.V2_20260920_0()); }
        }
        record V1_20261001_2() implements VirtualVersion<VirtualDowngradeLib, V1_20261001_0> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(2); }
            public V1_20261001_0 base() { return new V1_20261001_0(); }
            public Dependencies changes() { return new Dependencies(new Json.V2_20261105_0()); }
        }
    }

    /** Overrides the derived dependencies, dropping one its base has. */
    record VirtualDropsLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<VirtualDropsLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() {
                return new Dependencies(new Json.V2_20260920_0(), new Web.V1_20260925_0());
            }
        }
        record V1_20261001_1() implements VirtualVersion<VirtualDropsLib, V1_20261001_0> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
            public V1_20261001_0 base() { return new V1_20261001_0(); }
            public Dependencies changes() { return new Dependencies(new Json.V2_20261105_0()); }
            @Override public Dependencies dependencies() { return new Dependencies(new Json.V2_20261105_0()); }
        }
    }

    private static final VersionChecker CHECKER = new VersionChecker();

    private static void assertViolation(Class<? extends DeploymentUnit> unit, String fragment) {
        List<Violation> violations = CHECKER.check(unit);
        assertTrue(violations.stream().anyMatch(v -> v.message().contains(fragment)),
                "expected a violation containing '" + fragment + "' but got " + violations);
    }

    // ---- the vector

    @Test
    void dependenciesAreASetOfExactVersionsInAnyOrder() {
        Dependencies a = new Dependencies(new Json.V2_20260920_0(), new Web.V1_20260925_0());
        Dependencies b = new Dependencies(new Web.V1_20260925_0(), new Json.V2_20260920_0());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(Set.of(Json.class, Web.class), a.units());
        assertEquals(Optional.of(new Json.V2_20260920_0()), a.on(Json.class));
        assertEquals(Optional.empty(), a.on(Core.class));
        assertNotEquals(a, new Dependencies(new Json.V2_20261105_0(), new Web.V1_20260925_0()));
    }

    @Test
    void aReleaseDependsOnAtMostOneVersionOfEachUnit() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Dependencies(new Json.V2_20260920_0(), new Json.V2_20261105_0()));
        assertTrue(e.getMessage().contains("two versions of " + Json.class.getName()), e.getMessage());
        // the same version twice is just that version
        assertEquals(new Dependencies(new Json.V2_20260920_0()),
                new Dependencies(new Json.V2_20260920_0(), new Json.V2_20260920_0()));
    }

    @Test
    void aChangeReplacesVersionsButNeverAddsOrRemovesUnits() {
        Dependencies base = new Dependencies(new Json.V2_20260920_0(), new Web.V1_20260925_0());
        assertEquals(new Dependencies(new Json.V2_20261105_0(), new Web.V1_20260925_0()),
                base.changedBy(new Dependencies(new Json.V2_20261105_0())));
        assertEquals(base, base.changedBy(new Dependencies()));
        assertThrows(IllegalArgumentException.class,
                () -> new Dependencies(new Json.V2_20260920_0()).changedBy(new Dependencies(new Web.V1_20260925_0())));
    }

    @Test
    void aVirtualVersionsDependenciesAreItsBasesWithItsChanges() {
        assertEquals(new Dependencies(new Json.V2_20261105_0(), new Web.V1_20260925_0()),
                new Core.V8_20261001_1().dependencies());
        assertEquals(new Dependencies(new Core.V8_20261001_1()), new Ledger.V1_20261108_1().dependencies());
    }

    @Test
    void theNewTypesMeetTheOntologyContracts() {
        OntologyEnforcer enforcer = new OntologyEnforcer();
        for (Class<?> type : List.of(Dependable.class, Dependencies.class, VersionUnit.class,
                Core.V8_20261001_0.class, Core.V8_20261001_1.class, Ledger.V1_20261108_1.class)) {
            assertEquals(List.of(), enforcer.enforce(type), type.getName());
        }
    }

    // ---- the checker

    @Test
    void theValidEstateHasNoViolations() {
        for (Class<? extends DeploymentUnit> unit : List.of(Json.class, Web.class, Core.class, Ledger.class)) {
            assertEquals(List.of(), CHECKER.check(unit), unit.getName());
        }
    }

    @Test
    void aVersionCannotDependOnItsOwnUnit() {
        assertViolation(SelfDependentLib.class, "depends on its own unit");
    }

    @Test
    void aBundleDependsOnlyOnBundles() {
        assertViolation(BundleOnLibrary.class, "may depend only on bundles, not " + Core.class.getName());
    }

    @Test
    void aVirtualVersionCannotAddADependency() {
        assertViolation(VirtualAddsLib.class, "not add " + Web.class.getName());
    }

    @Test
    void aVirtualVersionCannotDropADependency() {
        assertViolation(VirtualDropsLib.class, "its dependencies are not its base's with its changes applied");
    }

    @Test
    void aVirtualVersionKeepsEachDependencyOnItsLine() {
        assertViolation(VirtualNewLineLib.class, "moves " + Json.class.getName() + " from line 2 to line 3");
    }

    @Test
    void aVirtualVersionOnlyMovesDependenciesForward() {
        assertViolation(VirtualDowngradeLib.class, "to 2.20260920.0, which is not later than its base's 2.20261105.0");
        assertViolation(VirtualDowngradeLib.class, "to 2.20261105.0, which is not later than its base's 2.20261105.0");
    }
}
