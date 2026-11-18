package com.example.fxsuite.avatar.roads;

/**
 * One course of what a strip of a road is built of, top down: what it is, and how thick ({@code thick}, cm). A
 * strip's courses, one on another, are its pavement; the level under the last of them is its formation.
 */
public record Course(Stuff stuff, double thick) {

    /** What a course is made of. */
    public enum Stuff {
        /** The surface course: what wheels run on. */
        ASPHALT,
        /** The binder course under it. */
        BINDER,
        /** Crushed stone, bound or not: what carries the load. */
        BASE,
        /** Coarser stone under the base: what spreads it. */
        SUBBASE,
        /** Precast or poured: kerbs, a slab. */
        CONCRETE,
        /** Flags or blocks: a footway's surface. */
        PAVING,
        /** The bed paving is laid on. */
        SAND,
        /** Loose stone: a lane's or a path's surface. */
        GRAVEL,
        /** Earth for things to grow in. */
        TOPSOIL,
        /** Grass, on its topsoil. */
        TURF;

        /** As it is written: lower case. */
        public String written() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public Course {
        if (stuff == null || !(thick > 0)) {
            throw new IllegalArgumentException("a course is of something, and some thickness");
        }
    }
}
