package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembersTest {

    private static final ExternalArtifact DATABIND =
            new ExternalArtifact("tools.jackson.core", "jackson-databind", "3.2.4");
    private static final ExternalArtifact GRAPHICS_WIN =
            new ExternalArtifact("org.openjfx", "javafx-graphics", "25.0.1", "win");

    /** A bundle release that forgot to say what it brings. */
    record NoMembersBundle() implements ExternalBundle {
        record V1_20261001_0() implements PhysicalVersion<NoMembersBundle> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(); }
        }
    }

    /** A bundle release that brings nothing. */
    record EmptyBundle() implements ExternalBundle {
        record V1_20261001_0() implements PhysicalVersion<EmptyBundle>, BundleRelease {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(); }
            public Members members() { return new Members(); }
        }
    }

    /** A library pretending to carry external jars. */
    record SmugglingLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<SmugglingLib>, BundleRelease {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() { return new Dependencies(); }
            public Members members() { return new Members(DATABIND); }
        }
    }

    private static final VersionChecker CHECKER = new VersionChecker();

    private static void assertViolation(Class<? extends DeploymentUnit> unit, String fragment) {
        List<Violation> violations = CHECKER.check(unit);
        assertTrue(violations.stream().anyMatch(v -> v.message().contains(fragment)),
                "expected a violation containing '" + fragment + "' but got " + violations);
    }

    @Test
    void anArtifactKnowsItsCoordinatesFileAndRepositoryPath() {
        assertEquals("tools.jackson.core:jackson-databind:3.2.4", DATABIND.coordinates());
        assertEquals("jackson-databind-3.2.4.jar", DATABIND.fileName());
        assertEquals("tools/jackson/core/jackson-databind/3.2.4/jackson-databind-3.2.4.jar",
                DATABIND.repositoryPath());
        assertEquals("org.openjfx:javafx-graphics:25.0.1:win", GRAPHICS_WIN.coordinates());
        assertEquals("org/openjfx/javafx-graphics/25.0.1/javafx-graphics-25.0.1-win.jar",
                GRAPHICS_WIN.repositoryPath());
    }

    @Test
    void anArtifactRejectsMissingParts() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalArtifact(" ", "a", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ExternalArtifact("g", "a", ""));
        assertThrows(IllegalArgumentException.class, () -> new ExternalArtifact("g", "a", "1", null));
    }

    @Test
    void membersAreASetWithOneVersionOfEachJar() {
        assertEquals(new Members(DATABIND, GRAPHICS_WIN), new Members(GRAPHICS_WIN, DATABIND));
        assertNotEquals(new Members(DATABIND), new Members(GRAPHICS_WIN));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Members(DATABIND,
                new ExternalArtifact("tools.jackson.core", "jackson-databind", "3.2.3")));
        assertTrue(e.getMessage().contains("two versions of tools.jackson.core:jackson-databind"), e.getMessage());
        // the platform jar and the plain jar are different jars
        assertEquals(2, new Members(GRAPHICS_WIN,
                new ExternalArtifact("org.openjfx", "javafx-graphics", "25.0.1")).artifacts().size());
    }

    @Test
    void everyBundleReleaseSaysWhatItBrings() {
        assertViolation(NoMembersBundle.class, "a bundle release must implement BundleRelease");
        assertViolation(EmptyBundle.class, "a bundle release must bring at least one member");
    }

    @Test
    void onlyABundleReleaseHasMembers() {
        assertViolation(SmugglingLib.class, "only a bundle release has members");
    }

    @Test
    void theNewTypesMeetTheOntologyContracts() {
        OntologyEnforcer enforcer = new OntologyEnforcer();
        for (Class<?> type : List.of(ExternalArtifact.class, Members.class, BundleRelease.class)) {
            assertEquals(List.of(), enforcer.enforce(type), type.getName());
        }
    }
}
