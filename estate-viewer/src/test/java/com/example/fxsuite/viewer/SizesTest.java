package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.ExternalArtifact;
import com.example.fxsuite.estate.AcmeCore;
import com.example.fxsuite.estate.AcmeFx;
import com.example.fxsuite.estate.AcmeGuava;
import com.example.fxsuite.estate.AcmeOntology;
import com.example.fxsuite.estate.Ledger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SizesTest {

    private static final LogScale SCALE = new LogScale();
    private static final SimulatedSizes SIZES = new SimulatedSizes();

    @Test
    void theScaleIsLogarithmicAndClampedAtBothEnds() {
        assertEquals(LogScale.SMALLEST, SCALE.factor(0));
        assertEquals(LogScale.SMALLEST, SCALE.factor(4 * 1024));
        assertEquals(LogScale.LARGEST, SCALE.factor(32L * 1024 * 1024));
        assertEquals(LogScale.LARGEST, SCALE.factor(1L << 40));
        // the geometric midpoint, 362 KB, is drawn halfway between
        assertEquals((LogScale.SMALLEST + LogScale.LARGEST) / 2, SCALE.factor(370_727), 0.001);
        // equal ratios add equal amounts
        double tenfold = SCALE.factor(100_000) - SCALE.factor(10_000);
        assertEquals(tenfold, SCALE.factor(1_000_000) - SCALE.factor(100_000), 1e-9);
    }

    @Test
    void aVirtualReleaseIsTheSizeOfTheBinaryItRuns() {
        assertEquals(SIZES.bytes(new Ledger.V1_20261101_0()), SIZES.bytes(new Ledger.V1_20261101_2()));
    }

    @Test
    void aBundleIsTheSizeOfItsJarsTogether() {
        long jars = new AcmeGuava.V1_20261124_0().members().artifacts().stream().mapToLong(SIZES::jar).sum();
        assertEquals(jars, SIZES.bytes(new AcmeGuava.V1_20261124_0()));
        // a refresh moves members, so a virtual bundle release can differ in size
        assertTrue(SIZES.bytes(new AcmeFx.V1_20261102_0()) > 8_000_000, "JavaFX is several MB");
        assertTrue(SIZES.bytes(new AcmeOntology.V1_20260901_0()) < 10_000, "jOntology is a few KB");
    }

    @Test
    void sizesAreTheSameOnEveryRunAndPlausible() {
        assertEquals(new SimulatedSizes().bytes(new AcmeCore.V8_20261001_0()), SIZES.bytes(new AcmeCore.V8_20261001_0()));
        long core = SIZES.bytes(new AcmeCore.V8_20261001_0());
        assertTrue(core >= 8_000 && core <= 250_000, "a library: " + core);
        long guava = SIZES.jar(new ExternalArtifact("com.google.guava", "guava", "33.7.2-jre"));
        assertTrue(guava > 3_000_000 && guava < 3_300_000, "Guava is about 3.1 MB: " + guava);
    }
}
