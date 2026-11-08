package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionTest {

    private static final String CORE_BINARY = "9f2c".repeat(16);

    // ---- a valid library: two lines, a checksummed release and a virtual release of it

    record AcmeCore() implements Library {

        record V8_20261001_0() implements PhysicalVersion<AcmeCore> {
            public Monotonic monotonic() { return new Monotonic(8); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
        }

        record V8_20261106_0() implements PhysicalVersion<AcmeCore>, IncludeChecksum {
            public Monotonic monotonic() { return new Monotonic(8); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 6)); }
            public Patch patch() { return new Patch(0); }
            public Sha256 checksum() { return new Sha256(CORE_BINARY); }
        }

        record V8_20261106_1() implements VirtualVersion<AcmeCore, V8_20261106_0>, IncludeChecksum {
            public Monotonic monotonic() { return new Monotonic(8); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 6)); }
            public Patch patch() { return new Patch(1); }
            public Sha256 checksum() { return new Sha256(CORE_BINARY); }
        }

        record V9_20261110_0() implements PhysicalVersion<AcmeCore> {
            public Monotonic monotonic() { return new Monotonic(9); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 10)); }
            public Patch patch() { return new Patch(0); }
        }
    }

    // ---- a valid application: a dependency-only revision, then a redesign on a new line

    record Ledger() implements Application {

        record V1_20261101_0() implements PhysicalVersion<Ledger> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 1)); }
            public Patch patch() { return new Patch(0); }
        }

        record V1_20261101_1() implements VirtualVersion<Ledger, V1_20261101_0> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 1)); }
            public Patch patch() { return new Patch(1); }
        }

        record V2_20261201_0() implements PhysicalVersion<Ledger> {
            public Monotonic monotonic() { return new Monotonic(2); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 12, 1)); }
            public Patch patch() { return new Patch(0); }
        }
    }

    // ---- units that each break one rule

    record MisnamedLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<MisnamedLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(3); }
        }
    }

    record DuplicateLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<DuplicateLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
        }
        record Again() implements PhysicalVersion<DuplicateLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
        }
    }

    record SharedDateLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<SharedDateLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
        }
        record V2_20261001_0() implements PhysicalVersion<SharedDateLib> {
            public Monotonic monotonic() { return new Monotonic(2); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
        }
    }

    record VirtualAcrossLinesLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<VirtualAcrossLinesLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
        }
        record V2_20261002_0() implements VirtualVersion<VirtualAcrossLinesLib, V1_20261001_0> {
            public Monotonic monotonic() { return new Monotonic(2); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 2)); }
            public Patch patch() { return new Patch(0); }
        }
    }

    record VirtualBeforeBaseLib() implements Library {
        record V1_20261005_0() implements PhysicalVersion<VirtualBeforeBaseLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 5)); }
            public Patch patch() { return new Patch(0); }
        }
        record V1_20261001_0() implements VirtualVersion<VirtualBeforeBaseLib, V1_20261005_0> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
        }
    }

    record ChecksumMismatchLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<ChecksumMismatchLib>, IncludeChecksum {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
            public Sha256 checksum() { return new Sha256("a".repeat(64)); }
        }
        record V1_20261001_1() implements VirtualVersion<ChecksumMismatchLib, V1_20261001_0>, IncludeChecksum {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
            public Sha256 checksum() { return new Sha256("b".repeat(64)); }
        }
    }

    record UncheckedBaseLib() implements Library {
        record V1_20261001_0() implements PhysicalVersion<UncheckedBaseLib> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(0); }
        }
        record V1_20261001_1() implements VirtualVersion<UncheckedBaseLib, V1_20261001_0>, IncludeChecksum {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
            public Sha256 checksum() { return new Sha256("c".repeat(64)); }
        }
    }

    /** Declares a version of AcmeCore inside the wrong unit. */
    record MisplacedLib() implements Library {
        record V8_20261201_0() implements PhysicalVersion<AcmeCore> {
            public Monotonic monotonic() { return new Monotonic(8); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 12, 1)); }
            public Patch patch() { return new Patch(0); }
        }
    }

    /** Its base is a version of the right unit but is not declared inside it. */
    record V3_20261001_0() implements PhysicalVersion<StrayBaseLib> {
        public Monotonic monotonic() { return new Monotonic(3); }
        public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
        public Patch patch() { return new Patch(0); }
    }

    record StrayBaseLib() implements Library {
        record V3_20261001_1() implements VirtualVersion<StrayBaseLib, V3_20261001_0> {
            public Monotonic monotonic() { return new Monotonic(3); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 1)); }
            public Patch patch() { return new Patch(1); }
        }
    }

    private static final VersionChecker CHECKER = new VersionChecker();

    private static void assertViolation(Class<? extends DeploymentUnit> unit, String fragment) {
        List<Violation> violations = CHECKER.check(unit);
        assertTrue(violations.stream().anyMatch(v -> v.message().contains(fragment)),
                "expected a violation containing '" + fragment + "' but got " + violations);
    }

    // ---- structure

    @Test
    void aVersionIsEitherPhysicalOrVirtual() {
        assertTrue(Version.class.isSealed());
        assertEquals(Set.of(PhysicalVersion.class, VirtualVersion.class),
                Set.of(Version.class.getPermittedSubclasses()));
        assertFalse(PhysicalVersion.class.isSealed());
        assertFalse(VirtualVersion.class.isSealed());
    }

    @Test
    void aVersionsTypeIsItsIdentity() {
        assertEquals(new AcmeCore.V8_20261106_0(), new AcmeCore.V8_20261106_0());
        assertFalse(new AcmeCore.V8_20261106_0().equals(new AcmeCore.V8_20261106_1()));
    }

    @Test
    void versionsOrderByLineThenDateThenPatch() {
        VersionOrder order = new VersionOrder();
        assertTrue(order.compare(new AcmeCore.V8_20261001_0(), new AcmeCore.V8_20261106_0()) < 0);
        assertTrue(order.compare(new AcmeCore.V8_20261106_0(), new AcmeCore.V8_20261106_1()) < 0);
        assertTrue(order.compare(new AcmeCore.V8_20261106_1(), new AcmeCore.V9_20261110_0()) < 0);
        assertEquals(0, order.compare(new AcmeCore.V9_20261110_0(), new AcmeCore.V9_20261110_0()));
        assertTrue(order.sameLine(new AcmeCore.V8_20261001_0(), new AcmeCore.V8_20261106_1()));
        assertFalse(order.sameLine(new AcmeCore.V8_20261106_1(), new AcmeCore.V9_20261110_0()));
    }

    @Test
    void theNumberPartsRejectBadValues() {
        assertThrows(IllegalArgumentException.class, () -> new Monotonic(-1));
        assertThrows(IllegalArgumentException.class, () -> new Patch(-1));
        assertThrows(NullPointerException.class, () -> new ReleaseDate(null));
        assertThrows(IllegalArgumentException.class, () -> new Sha256("ABC"));
        assertThrows(IllegalArgumentException.class, () -> new Sha256("A".repeat(64)));
    }

    @Test
    void theModelAndExampleVersionsMeetTheOntologyContracts() {
        OntologyEnforcer enforcer = new OntologyEnforcer();
        for (Class<?> type : List.of(Version.class, PhysicalVersion.class, VirtualVersion.class,
                Monotonic.class, ReleaseDate.class, Patch.class, Sha256.class, IncludeChecksum.class,
                Violation.class, VersionOrder.class, VersionChecker.class,
                AcmeCore.V8_20261106_0.class, AcmeCore.V8_20261106_1.class, Ledger.V1_20261101_1.class)) {
            assertEquals(List.of(), enforcer.enforce(type), type.getName());
        }
    }

    // ---- the checker: valid units

    @Test
    void aValidLibraryHasNoViolations() {
        assertEquals(List.of(), CHECKER.check(AcmeCore.class));
    }

    @Test
    void anApplicationUsesTheSameSchemeIncludingANewLineForARedesign() {
        assertEquals(List.of(), CHECKER.check(Ledger.class));
    }

    // ---- the checker: each rule

    @Test
    void theNameMustSpellTheNumber() {
        assertViolation(MisnamedLib.class, "is numbered 1.20261001.3 so it must be named V1_20261001_3");
    }

    @Test
    void twoVersionsCannotShareANumber() {
        assertViolation(DuplicateLib.class, "are both numbered 1.20261001.0");
    }

    @Test
    void aDateBelongsToOneLine() {
        assertViolation(SharedDateLib.class, "20261001 is used by lines 1 and 2");
    }

    @Test
    void aVirtualVersionStaysOnItsBasesLine() {
        assertViolation(VirtualAcrossLinesLib.class, "is on line 2 but its base 1.20261001.0 is on line 1");
    }

    @Test
    void aVirtualVersionComesAfterItsBase() {
        assertViolation(VirtualBeforeBaseLib.class, "1.20261001.0 must come after its base 1.20261005.0");
    }

    @Test
    void aVirtualVersionsChecksumMustBeItsBases() {
        assertViolation(ChecksumMismatchLib.class, "its checksum differs from its base 1.20261001.0");
        assertViolation(UncheckedBaseLib.class, "includes a checksum but its base 1.20261001.0 does not");
    }

    @Test
    void aVersionIsDeclaredInsideItsOwnUnit() {
        assertViolation(MisplacedLib.class, "is a version of " + AcmeCore.class.getName());
        assertViolation(StrayBaseLib.class, "its base must be declared in " + StrayBaseLib.class.getName());
    }
}
