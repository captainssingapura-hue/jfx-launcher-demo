package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Opt-in for a version: the digest of its binary, stamped into the type, so the release
 * cannot be swapped for different bytes under the same number.
 *
 * <p>A virtual version reuses its base's binary, so if it includes a checksum, its base
 * must include the same one.
 */
public interface IncludeChecksum extends Stateless {

    Sha256 checksum();
}
