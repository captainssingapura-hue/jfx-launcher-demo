package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.Advisory;
import com.example.fxsuite.deployment.AffectedRange;
import com.example.fxsuite.deployment.Sequence;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * The published advisories that affect jars the demo estate's bundles bring: a snapshot
 * taken from OSV (osv.dev) on 2026-11-09, so the demo gives the same answers offline. Each
 * advisory keeps every affected range OSV lists for it.
 */
public final class KnownAdvisories implements StatelessFunctionalObject {

    /** When the snapshot was taken. */
    public String asOf() {
        return "2026-11-09";
    }

    public List<Advisory> all() {
        return List.of(
                new Advisory("GHSA-7hhh-6rmp-j9qf", "CVE-2026-89425", "HIGH",
                        "jackson-core: UTF8DataInputJsonParser._reportInvalidToken() missing maxErrorTokenLength limit -> unbounded StringBuilder growth (DoS)",
                        new Sequence<>(List.of(
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-core", "2.19.0", "2.21.7"),
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-core", "2.22.0", "2.22.3"),
                                new AffectedRange("tools.jackson.core", "jackson-core", "3.0.0", "3.1.7"),
                                new AffectedRange("tools.jackson.core", "jackson-core", "3.2.0", "3.2.3"),
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-core", "2.8.0", "2.18.11")))),
                new Advisory("GHSA-cxp5-3px4-pw24", "CVE-2026-91777", "HIGH",
                        "jackson-databind quadratic forward-reference completion",
                        new Sequence<>(List.of(
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-databind", "2.19.0", "2.21.7"),
                                new AffectedRange("tools.jackson.core", "jackson-databind", "3.0.0", "3.1.7"),
                                new AffectedRange("tools.jackson.core", "jackson-databind", "3.2.0", "3.2.3"),
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-databind", "2.5.0", "2.18.11"),
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-databind", "2.22.0", "2.22.3")))),
                new Advisory("GHSA-p6pp-m3f8-5c89", "CVE-2026-89407", "HIGH",
                        "jackson-core: ReDoS: quadratic backtracking in NumberInput.PATTERN_FLOAT via looksLikeValidNumber()",
                        new Sequence<>(List.of(
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-core", "2.17.0", "2.18.11"),
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-core", "2.19.0", "2.21.7"),
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-core", "2.22.0", "2.22.3"),
                                new AffectedRange("tools.jackson.core", "jackson-core", "3.0.0", "3.1.7"),
                                new AffectedRange("tools.jackson.core", "jackson-core", "3.2.0", "3.2.2")))),
                new Advisory("GHSA-wv8q-qhhj-9h54", "CVE-2026-91776", "HIGH",
                        "jackson-databind retains every unknown raw type ID",
                        new Sequence<>(List.of(
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-databind", "2.0.0", "2.18.11"),
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-databind", "2.19.0", "2.21.7"),
                                new AffectedRange("com.fasterxml.jackson.core", "jackson-databind", "2.22.0", "2.22.3"),
                                new AffectedRange("tools.jackson.core", "jackson-databind", "3.0.0", "3.1.7"),
                                new AffectedRange("tools.jackson.core", "jackson-databind", "3.2.0", "3.2.3")))),
                new Advisory("GHSA-xxph-c9ww-hj94", "CVE-2026-102554", "HIGH",
                        "Google Guava: CompactHashMap/CompactHashSet/MapMakerInternalMap deserialization triggers uncontrolled memory allocation",
                        new Sequence<>(List.of(
                                new AffectedRange("com.google.guava", "guava", "4.0", "33.7.2")))));
    }
}
