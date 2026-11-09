package com.example.fxsuite.launch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * Puts exactly a plan's jars into an application release's own directory, taking each from
 * the first of the given Maven-layout repositories that has it: {@code <dir>/lib/} holds
 * copies of the jars, and {@code <dir>/installed.txt} records each jar's coordinates and
 * SHA-256.
 *
 * <p>An existing installation is reused only when it was made from the same plan and every
 * jar still has the recorded checksum. Otherwise, for instance if a jar was modified, it is
 * installed again from the repositories, and the reason is reported.
 *
 * <p>A release never changes. If a repository now holds different bytes for a jar this
 * installation already has, that is refused with {@link ReleaseChanged} rather than quietly
 * running either version; installing again must then be asked for explicitly.
 */
public final class Installer {

    private static final String RECORD = "installed.txt";

    /** What was done, and the jars to put on the classpath, in plan order. */
    public record Installation(List<Path> jars, String outcome) {
    }

    /** A repository holds different bytes for a release than the ones already installed. */
    public static final class ReleaseChanged extends IOException {
        public ReleaseChanged(final List<String> changed) {
            super("a release's bytes must never change, but the repository now has different bytes for:\n  "
                    + String.join("\n  ", changed));
        }
    }

    /** Some jars the plan needs are in none of the repositories. */
    public static final class MissingJars extends IOException {
        public MissingJars(final List<String> missing, final List<Path> repositories) {
            super("not in any repository " + repositories + ":\n  " + String.join("\n  ", missing));
        }
    }

    public Installation install(final List<Path> repositories, final Path dir, final List<Coordinates> plan)
            throws IOException {
        return install(repositories, dir, plan, false);
    }

    /**
     * @param reinstall install again from the repositories whatever is there, accepting
     *                  changed bytes
     */
    public Installation install(final List<Path> repositories, final Path dir, final List<Coordinates> plan,
                                final boolean reinstall) throws IOException {
        final Path lib = dir.resolve("lib");
        final List<Path> jars = plan.stream().map(c -> lib.resolve(c.fileName())).toList();
        final List<String> expected = plan.stream().map(Coordinates::toString).toList();

        final String reason = reinstall ? "asked to reinstall" : reasonToReinstall(dir.resolve(RECORD), lib, expected);
        if (reason == null) {
            refuseChangedReleases(repositories, dir.resolve(RECORD));
            return new Installation(jars, "reused the existing installation; every jar matches its checksum");
        }

        final List<Path> sources = new ArrayList<>();
        final List<String> missing = new ArrayList<>();
        for (final Coordinates jar : plan) {
            final Path source = repositories.stream().map(repository -> repository.resolve(jar.repositoryPath()))
                    .filter(Files::isRegularFile).findFirst().orElse(null);
            if (source == null) {
                missing.add(jar.toString());
            }
            sources.add(source);
        }
        if (!missing.isEmpty()) {
            throw new MissingJars(missing, repositories);
        }

        // Move the old jars aside in one step first. On Windows that fails if the app is still
        // running with them open, so nothing is touched until we know they can all go.
        final Path old = dir.resolve("lib.old");
        delete(old);
        if (Files.isDirectory(lib)) {
            try {
                Files.move(lib, old);
            } catch (IOException e) {
                throw new InUse(dir);
            }
        }
        Files.createDirectories(lib);
        final List<String> record = new ArrayList<>();
        for (int i = 0; i < plan.size(); i++) {
            final Coordinates jar = plan.get(i);
            final Path copy = lib.resolve(jar.fileName());
            Files.copy(sources.get(i), copy, StandardCopyOption.REPLACE_EXISTING);
            record.add(jar + " " + sha256(copy));
        }
        Files.write(dir.resolve(RECORD), record);
        delete(old);
        return new Installation(jars, "installed " + plan.size() + " jars (" + reason + ")");
    }

    /** The installation's jars are open, normally because the app is running. */
    public static final class InUse extends IOException {
        public InUse(final Path dir) {
            super(dir + " is in use, normally because this release is still running; close it and try again."
                    + " Nothing was changed.");
        }
    }

    /** Compares each installed jar's recorded checksum with the repository's current bytes. */
    private void refuseChangedReleases(final List<Path> repositories, final Path record) throws IOException {
        final List<String> changed = new ArrayList<>();
        for (final String line : Files.readAllLines(record)) {
            final Coordinates jar = parse(line.substring(0, line.lastIndexOf(' ')));
            final String checksum = line.substring(line.lastIndexOf(' ') + 1);
            final Path source = repositories.stream().map(repository -> repository.resolve(jar.repositoryPath()))
                    .filter(Files::isRegularFile).findFirst().orElse(null);
            // a jar the repositories no longer have is fine: the installed copy is intact
            if (source != null && !sha256(source).equals(checksum)) {
                changed.add(jar + "  (" + source + ")");
            }
        }
        if (!changed.isEmpty()) {
            throw new ReleaseChanged(changed);
        }
    }

    /** Null when the installation can be reused as it is. */
    private String reasonToReinstall(final Path record, final Path lib, final List<String> expected) throws IOException {
        if (!Files.isRegularFile(record)) {
            return "first installation";
        }
        final List<String> lines = Files.readAllLines(record);
        final List<String> recorded = lines.stream().map(line -> line.substring(0, line.lastIndexOf(' '))).toList();
        if (!recorded.equals(expected)) {
            return "the plan changed";
        }
        for (final String line : lines) {
            final Coordinates jar = parse(line.substring(0, line.lastIndexOf(' ')));
            final String checksum = line.substring(line.lastIndexOf(' ') + 1);
            final Path copy = lib.resolve(jar.fileName());
            if (!Files.isRegularFile(copy)) {
                return jar.fileName() + " was missing";
            }
            if (!sha256(copy).equals(checksum)) {
                return jar.fileName() + " had been modified";
            }
        }
        return null;
    }

    private Coordinates parse(final String text) {
        final String[] parts = text.split(":");
        return new Coordinates(parts[0], parts[1], parts[2], parts.length > 3 ? parts[3] : "");
    }

    private void delete(final Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (final Path file : files.toList()) {
                Files.delete(file);
            }
        }
        Files.delete(dir);
    }

    public String sha256(final Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] buffer = new byte[64 * 1024];
            for (int n = in.read(buffer); n > 0; n = in.read(buffer)) {
                digest.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
