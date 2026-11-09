package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Every release of an {@link ExternalBundle} implements this: the external jars it brings.
 * A virtual bundle release declares its full set too, since moving members forward is
 * what such a release is for.
 *
 * <p>Members also include the members' own runtime dependencies, pinned, so nothing is
 * left to a build tool's own resolution.
 */
public interface BundleRelease extends Stateless {

    Members members();
}
