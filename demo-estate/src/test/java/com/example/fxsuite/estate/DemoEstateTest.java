package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Application;
import com.example.fxsuite.deployment.BundleRelease;
import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.DeploymentUnit;
import com.example.fxsuite.deployment.ExternalArtifact;
import com.example.fxsuite.deployment.ExternalBundle;
import com.example.fxsuite.deployment.Lineage;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.Resolution;
import com.example.fxsuite.deployment.Resolver;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionChecker;
import com.example.fxsuite.deployment.VulnerabilityScanner;
import com.example.fxsuite.deployment.Warning;

import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoEstateTest {

    /** A change someone might propose for Atlas: it would need AcmeCore on lines 8 and 9. */
    record AtlasProposal() implements Application {
        record V1_20261125_0() implements PhysicalVersion<AtlasProposal> {
            public Monotonic monotonic() { return new Monotonic(1); }
            public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 25)); }
            public Patch patch() { return new Patch(0); }
            public Dependencies dependencies() {
                return new Dependencies(new AcmeNet.V6_20261112_1(), new AcmeText.V3_20261020_1());
            }
        }
    }

    private static final List<Class<? extends DeploymentUnit>> UNITS = new DemoEstate().units();
    private static final VersionChecker CHECKER = new VersionChecker();
    private static final Resolver RESOLVER = new Resolver();

    private static Optional<Version<?>> resolved(Version<?> root, Class<?> unit) {
        return RESOLVER.resolve(root).resolved().on(unit).map(v -> (Version<?>) v);
    }

    private static List<Version<?>> releasesOf(Class<?> unit) {
        List<Version<?>> versions = new ArrayList<>();
        for (Class<?> type : unit.getDeclaredClasses()) {
            try {
                versions.add((Version<?>) type.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
        return versions;
    }

    // ---- the estate as a whole

    @Test
    void theCatalogueListsEachUnitOnce() {
        assertEquals(16, UNITS.size());
        assertEquals(UNITS.size(), new HashSet<>(UNITS).size());
    }

    @Test
    void everyUnitPassesTheChecker() {
        for (Class<? extends DeploymentUnit> unit : UNITS) {
            assertEquals(List.of(), CHECKER.check(unit), unit.getSimpleName());
        }
    }

    @Test
    void everyReleaseMeetsTheOntologyContracts() {
        OntologyEnforcer enforcer = new OntologyEnforcer();
        for (Class<? extends DeploymentUnit> unit : UNITS) {
            assertEquals(List.of(), enforcer.enforce(unit), unit.getName());
            for (Class<?> version : unit.getDeclaredClasses()) {
                assertEquals(List.of(), enforcer.enforce(version), version.getName());
            }
        }
    }

    @Test
    void theOnlyWarningIsTheDiamondInLedgersFirstRelease() {
        List<String> warnings = UNITS.stream()
                .flatMap(u -> CHECKER.warnings(u).stream())
                .map(Warning::message)
                .toList();
        assertEquals(List.of("Ledger 1.20261101.0: AcmeCore is requested at "
                + "8.20261001.0 (by AcmeText 3.20261020.0), 8.20261015.0 (by AcmeNet 5.20261016.0); "
                + "using 8.20261015.0. Move the others to 8.20261015.0 so every release runs with the "
                + "version it declares"), warnings);
    }

    @Test
    void eachExternalJarBelongsToExactlyOneBundle() {
        Map<String, Class<?>> owner = new HashMap<>();
        for (Class<? extends DeploymentUnit> unit : UNITS) {
            if (!ExternalBundle.class.isAssignableFrom(unit)) {
                continue;
            }
            for (Version<?> release : releasesOf(unit)) {
                for (ExternalArtifact artifact : ((BundleRelease) release).members().artifacts()) {
                    Class<?> earlier = owner.putIfAbsent(artifact.group() + ":" + artifact.artifact(), unit);
                    assertTrue(earlier == null || earlier == unit,
                            artifact.coordinates() + " is in both " + earlier + " and " + unit);
                }
            }
        }
    }

    @Test
    void noAppRunsWithTwoVersionsOfTheSameJar() {
        for (Class<? extends DeploymentUnit> unit : UNITS) {
            if (!Application.class.isAssignableFrom(unit)) {
                continue;
            }
            for (Version<?> app : releasesOf(unit)) {
                Map<String, ExternalArtifact> jars = new HashMap<>();
                for (Version<?> release : RESOLVER.resolve(app).resolved().versions()) {
                    if (release instanceof BundleRelease bundle) {
                        for (ExternalArtifact jar : bundle.members().artifacts()) {
                            ExternalArtifact other = jars.putIfAbsent(jar.key(), jar);
                            assertTrue(other == null || other.equals(jar), app + ": " + jar + " and " + other);
                        }
                    }
                }
            }
        }
    }

    // ---- the scenarios

    @Test
    void theDiamondResolvesToTheHigherAcmeCore() {
        assertEquals(Optional.of(new AcmeCore.V8_20261015_0()), resolved(new Ledger.V1_20261101_0(), AcmeCore.class));
    }

    @Test
    void theSecurityFloorReachesLedgerThroughTheLibrariesInBetween() {
        Version<?> ledger = new Ledger.V1_20261101_1();
        assertEquals(Optional.of(new AcmeCore.V8_20261106_1()), resolved(ledger, AcmeCore.class));
        assertEquals(Optional.of(new AcmeText.V3_20261020_1()), resolved(ledger, AcmeText.class));
        assertTrue(RESOLVER.resolve(ledger).warnings().isEmpty());
        // A dependency-only revision: Ledger's binary is unchanged.
        assertEquals(new Ledger.V1_20261101_0(), new Lineage().physicalOf(ledger));
    }

    @Test
    void ledgerAndAtlasRunDifferentLinesSideBySide() {
        Version<?> ledger = new Ledger.V1_20261101_1();
        Version<?> atlas = new Atlas.V1_20261121_1();
        assertEquals(Optional.of(new AcmeCore.V8_20261106_1()), resolved(ledger, AcmeCore.class));
        assertEquals(Optional.of(new AcmeCore.V9_20261120_0()), resolved(atlas, AcmeCore.class));
        // Jackson 2 for Ledger, Jackson 3 for Atlas
        assertEquals(1, resolved(ledger, AcmeJson.class).orElseThrow().monotonic().value());
        assertEquals(2, resolved(atlas, AcmeJson.class).orElseThrow().monotonic().value());
    }

    @Test
    void theJacksonGuavaBundleGetsGuavaFromTheGuavaBundle() {
        Version<?> atlas = new Atlas.V1_20261121_1();
        assertEquals(Optional.of(new AcmeGuava.V1_20261103_0()), resolved(atlas, AcmeGuava.class));
        assertEquals(Optional.of(new AcmeJsonGuava.V1_20261105_0()), resolved(atlas, AcmeJsonGuava.class));
        assertTrue(new AcmeGuava.V1_20261103_0().members().artifacts()
                .contains(new ExternalArtifact("com.google.guava", "guava", "33.5.0-jre")));
    }

    @Test
    void ledgerAndCourierShareAContractLine() {
        Version<?> ledger = resolved(new Ledger.V1_20261101_1(), AcmeContract.class).orElseThrow();
        Version<?> courier = resolved(new Courier.V1_20261107_0(), AcmeContract.class).orElseThrow();
        assertEquals(ledger.monotonic(), courier.monotonic());
    }

    @Test
    void aBundleRefreshIsVirtualEvenWithANewDate() {
        assertEquals(new AcmeFx.V1_20261002_0(), new Lineage().physicalOf(new AcmeFx.V1_20261102_0()));
        assertEquals(new AcmeJson.V2_20261005_0(), new Lineage().physicalOf(new AcmeJson.V2_20261105_0()));
    }

    @Test
    void eachAppResolvesWithoutConflicts() {
        for (Class<? extends DeploymentUnit> unit : UNITS) {
            if (!Application.class.isAssignableFrom(unit)) {
                continue;
            }
            for (Version<?> version : releasesOf(unit)) {
                Resolution resolution = RESOLVER.resolve(version);
                assertTrue(resolution.isValid(), version + ": " + resolution.conflicts());
            }
        }
    }

    @Test
    void mixingAcmeCoreLinesIsCaughtFromTheVersionsAlone() {
        Resolution resolution = RESOLVER.resolve(new AtlasProposal.V1_20261125_0());
        assertFalse(resolution.isValid());
        assertTrue(resolution.conflicts().items().get(0).message().contains("needs AcmeCore on more than one line"),
                resolution.conflicts().toString());
    }
    // ---- security: advisories from OSV, fixed only by virtual releases

    private static final List<com.example.fxsuite.deployment.Advisory> ADVISORIES = new KnownAdvisories().all();

    private static List<String> exposures(Version<?> release) {
        return new VulnerabilityScanner().scan(release, ADVISORIES).stream()
                .map(e -> e.jar().artifact() + " " + e.jar().version() + " " + e.advisory().id()).toList();
    }

    @Test
    void thePhysicalJacksonBundleIsVulnerableAndItsVirtualRefreshIsNot() {
        assertEquals(List.of(
                "jackson-core 2.22.2 GHSA-7hhh-6rmp-j9qf",
                "jackson-core 2.22.2 GHSA-p6pp-m3f8-5c89",
                "jackson-databind 2.22.2 GHSA-cxp5-3px4-pw24",
                "jackson-databind 2.22.2 GHSA-wv8q-qhhj-9h54"), exposures(new AcmeJson.V1_20260915_0()));
        assertEquals(List.of(), exposures(new AcmeJson.V1_20261020_0()));
        assertTrue(new AcmeJson.V1_20261020_0() instanceof com.example.fxsuite.deployment.VirtualVersion<?, ?>);
    }

    @Test
    void guavaIsFixedOnlyByItsVirtualRelease() {
        assertEquals(List.of("guava 33.4.8-jre GHSA-xxph-c9ww-hj94"), exposures(new AcmeGuava.V1_20261001_0()));
        assertEquals(List.of("guava 33.5.0-jre GHSA-xxph-c9ww-hj94"), exposures(new AcmeGuava.V1_20261103_0()));
        assertEquals(List.of(), exposures(new AcmeGuava.V1_20261124_0()));
        assertEquals(new AcmeGuava.V1_20261001_0(), new Lineage().physicalOf(new AcmeGuava.V1_20261124_0()));
    }

    @Test
    void ledgerBecomesSecureThroughVirtualReleasesAloneOnTheSameBinary() {
        assertEquals(5, exposures(new Ledger.V1_20261101_0()).size(), "Jackson 2.22.2 and Guava");
        assertEquals(List.of("guava 33.4.8-jre GHSA-xxph-c9ww-hj94"), exposures(new Ledger.V1_20261101_1()));
        assertEquals(List.of(), exposures(new Ledger.V1_20261101_2()));
        assertEquals(new Ledger.V1_20261101_0(), new Lineage().physicalOf(new Ledger.V1_20261101_2()));
        assertTrue(RESOLVER.resolve(new Ledger.V1_20261101_2()).warnings().isEmpty());
    }

    @Test
    void atlasBecomesSecureByAVirtualRelease() {
        assertEquals(List.of("guava 33.5.0-jre GHSA-xxph-c9ww-hj94"), exposures(new Atlas.V1_20261121_1()));
        assertEquals(List.of(), exposures(new Atlas.V1_20261121_2()));
        assertTrue(RESOLVER.resolve(new Atlas.V1_20261121_2()).warnings().isEmpty());
    }

    @Test
    void courierStaysInsecureBecauseItNeedsARebuild() {
        assertEquals(List.of("guava 33.5.0-jre GHSA-xxph-c9ww-hj94"), exposures(new Courier.V1_20261107_0()));
    }

    @Test
    void helloCarriesNoKnownVulnerability() {
        assertEquals(List.of(), exposures(new Hello.V1_20261003_1()));
    }
}
