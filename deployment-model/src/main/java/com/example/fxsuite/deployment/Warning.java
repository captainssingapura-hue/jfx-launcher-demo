package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * Something that works but should be fixed, such as a dependency requested at two
 * versions.
 */
public record Warning(String message) implements Scalar {
}
