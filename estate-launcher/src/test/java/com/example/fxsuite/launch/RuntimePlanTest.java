package com.example.fxsuite.launch;

import com.example.fxsuite.deployment.Resolver;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.estate.AcmeCore;
import com.example.fxsuite.estate.AcmeJsonGuava;
import com.example.fxsuite.estate.Atlas;
import com.example.fxsuite.estate.DemoEstate;
import com.example.fxsuite.estate.Ledger;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimePlanTest {

    private static final RuntimePlan PLAN = new RuntimePlan();

    private static List<String> jarsFor(Version<?> app) {
        return PLAN.jars(app, new Resolver().resolve(app).resolved().versions()).stream()
                .map(Coordinates::toString).toList();
    }

    @Test
    void aUnitsJarIsNamedAfterItInLowerCaseWithHyphens() {
        assertEquals("acme-core", PLAN.artifactOf(AcmeCore.class));
        assertEquals("acme-json-guava", PLAN.artifactOf(AcmeJsonGuava.class));
        assertEquals("ledger", PLAN.artifactOf(Ledger.class));
    }

    @Test
    void aVirtualReleaseRunsItsBasesJar() {
        List<String> jars = jarsFor(new Ledger.V1_20261101_1());
        // the app's own jar comes first, and is the physical base's
        assertEquals("com.example.acme:ledger:1.20261101.0", jars.get(0));
        // AcmeText 3.20261020.1 is virtual, so its base's jar; AcmeCore carries the fix
        assertTrue(jars.contains("com.example.acme:acme-text:3.20261020.0"), jars.toString());
        assertTrue(jars.contains("com.example.acme:acme-core:8.20261106.1"), jars.toString());
        assertFalse(jars.stream().anyMatch(j -> j.endsWith("3.20261020.1")), jars.toString());
    }

    @Test
    void bundlesBringTheirMembers() {
        List<String> jars = jarsFor(new Ledger.V1_20261101_1());
        assertTrue(jars.contains("com.fasterxml.jackson.core:jackson-databind:2.22.4"), jars.toString());
        assertTrue(jars.contains("org.openjfx:javafx-graphics:25:win"), jars.toString());
        assertTrue(jars.contains("com.google.guava:guava:33.4.8-jre"), jars.toString());
        assertTrue(jars.contains("io.github.captainssingapura-hue.lang.ontology:core:0.1.0"), jars.toString());
        // 7 internal jars (Ledger and six libraries) and 13 external ones
        assertEquals(20, jars.size(), jars.toString());
    }

    @Test
    void noAppGetsTwoVersionsOfOneJar() {
        for (Version<?> app : List.<Version<?>>of(new Ledger.V1_20261101_0(), new Ledger.V1_20261101_1(),
                new Atlas.V1_20261112_0(), new Atlas.V1_20261121_1())) {
            List<String> keys = jarsFor(app).stream().map(j -> j.substring(0, j.indexOf(':', j.indexOf(':') + 1)))
                    .toList();
            Set<String> distinct = new HashSet<>(keys);
            assertEquals(keys.size(), distinct.size(), app + ": " + keys);
        }
    }

    @Test
    void atlasRunsJackson3AndAcmeCore9() {
        List<String> jars = jarsFor(new Atlas.V1_20261121_1());
        assertTrue(jars.contains("tools.jackson.core:jackson-databind:3.2.4"), jars.toString());
        assertTrue(jars.contains("tools.jackson.datatype:jackson-datatype-guava:3.2.4"), jars.toString());
        assertTrue(jars.contains("com.example.acme:acme-core:9.20261120.0"), jars.toString());
        assertTrue(jars.contains("com.google.guava:guava:33.5.0-jre"), jars.toString());
    }

    @Test
    void nearestWinsPicksTheLowerAcmeCoreForLedger() {
        Set<Object> picks = new HashSet<>(PLAN.nearestWins(new Ledger.V1_20261101_0()));
        assertTrue(picks.contains(new AcmeCore.V8_20261001_0()));
        assertFalse(picks.contains(new AcmeCore.V8_20261015_0()));
        // the model's resolution picks the higher one
        assertTrue(new Resolver().resolve(new Ledger.V1_20261101_0()).resolved().versions()
                .contains(new AcmeCore.V8_20261015_0()));
    }

    @Test
    void theCatalogueFindsReleasesByName() {
        Catalogue catalogue = new Catalogue(new DemoEstate().units());
        assertEquals(new Ledger.V1_20261101_1(), catalogue.find("Ledger", "1.20261101.1").orElseThrow());
        assertTrue(catalogue.find("Ledger", "9.99999999.9").isEmpty());
        assertEquals(List.of("Hello", "Ledger", "Atlas", "Courier"), catalogue.applications().stream()
                .map(Class::getSimpleName).collect(Collectors.toList()));
    }
}
