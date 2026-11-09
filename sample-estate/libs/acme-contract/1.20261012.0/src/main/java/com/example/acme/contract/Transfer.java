package com.example.acme.contract;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * Money moving from one account to another.
 */
public record Transfer(String from, String to, long cents) implements Scalar {
}
