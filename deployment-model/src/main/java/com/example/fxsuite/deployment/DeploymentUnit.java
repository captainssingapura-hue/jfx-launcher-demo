package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Something the estate manages and deploys: an application, an internal library, or a
 * bundle of external libraries. Nothing else is a deployment unit; in particular an
 * external library is never deployed on its own, only as a member of a bundle.
 *
 * <p>A unit records only that it exists. Each actual unit is a stateless value, typically
 * a record with no components, so every instance of it is equal and its type is its
 * identity:
 *
 * <pre>{@code
 * public record Ledger() implements Application {}
 * }</pre>
 */
public sealed interface DeploymentUnit extends Scalar, Stateless
        permits Application, Library, ExternalBundle {
}
