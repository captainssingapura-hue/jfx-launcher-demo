package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * A kind of unit that other units may depend on: a library or a bundle. Applications are
 * deliberately not dependable, so a dependency on an application does not compile.
 */
public sealed interface Dependable extends Stateless permits Library, ExternalBundle {
}
