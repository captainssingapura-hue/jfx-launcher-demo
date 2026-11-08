package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * An ordered list of values, fixed at construction. Equal when the items are equal in the
 * same order.
 */
public final class Sequence<S extends Scalar> implements Vector<S> {

    private final List<S> items;

    public Sequence(final List<? extends S> items) {
        this.items = List.copyOf(items);
    }

    public List<S> items() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Sequence<?> s && items.equals(s.items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
