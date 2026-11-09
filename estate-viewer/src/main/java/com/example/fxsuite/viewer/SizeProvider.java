package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Version;

/**
 * How big a release's artifact is, in bytes: for a library or application, the jar of the
 * physical release it runs; for a bundle, the jars it brings, together.
 */
public interface SizeProvider {

    long bytes(Version<?> release);

    /** Where the sizes come from, for the legend: {@code simulated}, {@code measured}. */
    String source();
}
