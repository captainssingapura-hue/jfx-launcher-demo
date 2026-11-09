package com.example.fxsuite.launch;

/**
 * Where a jar lives in a repository laid out like Maven's.
 *
 * @param classifier empty when the jar has none
 */
public record Coordinates(String group, String artifact, String version, String classifier) {

    /** {@code artifact-version.jar} or {@code artifact-version-classifier.jar} */
    public String fileName() {
        return artifact + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + ".jar";
    }

    /** The jar's path inside a Maven-layout repository, with {@code /} separators. */
    public String repositoryPath() {
        return group.replace('.', '/') + "/" + artifact + "/" + version + "/" + fileName();
    }

    @Override
    public String toString() {
        return group + ":" + artifact + ":" + version + (classifier.isEmpty() ? "" : ":" + classifier);
    }
}
