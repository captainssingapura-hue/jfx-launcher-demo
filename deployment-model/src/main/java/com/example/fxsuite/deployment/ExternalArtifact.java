package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * One external jar, by its Maven coordinates. External libraries are not deployment
 * units; they enter the estate only as members of a bundle.
 *
 * @param classifier empty when the jar has none, otherwise e.g. {@code win} for a
 *                   platform's native jar
 */
public record ExternalArtifact(String group, String artifact, String version, String classifier)
        implements Scalar {

    public ExternalArtifact {
        if (group == null || group.isBlank() || artifact == null || artifact.isBlank()
                || version == null || version.isBlank()) {
            throw new IllegalArgumentException("group, artifact and version must not be blank: "
                    + group + ":" + artifact + ":" + version);
        }
        if (classifier == null) {
            throw new IllegalArgumentException("classifier must not be null; use an empty string for none");
        }
    }

    public ExternalArtifact(final String group, final String artifact, final String version) {
        this(group, artifact, version, "");
    }

    /** {@code group:artifact:version} or {@code group:artifact:version:classifier} */
    public String coordinates() {
        return group + ":" + artifact + ":" + version + (classifier.isEmpty() ? "" : ":" + classifier);
    }

    /** {@code artifact-version.jar} or {@code artifact-version-classifier.jar} */
    public String fileName() {
        return artifact + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + ".jar";
    }

    /** Where the jar lives in a repository laid out like Maven's, with {@code /} separators. */
    public String repositoryPath() {
        return group.replace('.', '/') + "/" + artifact + "/" + version + "/" + fileName();
    }

    /** The same jar at any version: what "one version per external" is counted by. */
    public String key() {
        return group + ":" + artifact + (classifier.isEmpty() ? "" : ":" + classifier);
    }
}
