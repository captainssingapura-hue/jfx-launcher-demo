package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.time.format.DateTimeFormatter;

/**
 * How versions are written in messages.
 */
public final class VersionText implements StatelessFunctionalObject {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    /** {@code 8.20261106.1} */
    public String number(final Version<?> version) {
        return version.monotonic().value() + "." + version.date().value().format(YYYYMMDD)
                + "." + version.patch().value();
    }

    /** {@code AcmeCore 8.20261106.1} */
    public String named(final Version<?> version) {
        return new VersionUnit().of(version.getClass()).map(Class::getSimpleName)
                .orElse(version.getClass().getSimpleName()) + " " + number(version);
    }

    /** {@code V8_20261106_1}: the class name a version with this number must have. */
    public String className(final Version<?> version) {
        return "V" + version.monotonic().value() + "_" + version.date().value().format(YYYYMMDD)
                + "_" + version.patch().value();
    }

    public String date(final ReleaseDate date) {
        return date.value().format(YYYYMMDD);
    }
}
