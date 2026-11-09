package com.example.fxsuite.launch;

import com.example.fxsuite.deployment.Resolution;
import com.example.fxsuite.deployment.Resolver;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.deployment.VersionUnit;
import com.example.fxsuite.deployment.Violation;
import com.example.fxsuite.deployment.Warning;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Launching an application release, in three steps any front end can drive: {@link #prepare}
 * works out what would run without touching the disk, {@link #install} copies exactly those
 * jars into the release's own directory, and {@link #start} starts it in its own JVM with
 * that directory as its classpath.
 */
public final class AppLauncher {

    /**
     * Where jars come from, and where releases are installed.
     *
     * @param repositories Maven-layout repositories, searched in order for each jar
     * @param appsDir      each release is installed in {@code <appsDir>/<app>/<version>}
     */
    public record Settings(List<Path> repositories, Path appsDir) {

        /**
         * {@code extra} if given, then {@code dist/acme-repo} under the current directory
         * (where the sample estate publishes), then the local Maven repository; releases go
         * to {@code %LOCALAPPDATA%\fxsuite\apps}.
         */
        public static Settings defaults(final Optional<Path> extra, final Optional<Path> appsDir) {
            final List<Path> repositories = new ArrayList<>();
            extra.ifPresent(repositories::add);
            repositories.add(Path.of("dist", "acme-repo").toAbsolutePath());
            repositories.add(Path.of(System.getProperty("user.home"), ".m2", "repository"));
            final String local = System.getenv("LOCALAPPDATA");
            final Path apps = appsDir.orElse((local != null ? Path.of(local)
                    : Path.of(System.getProperty("user.home"), ".local", "share")).resolve("fxsuite").resolve("apps"));
            return new Settings(List.copyOf(repositories), apps);
        }
    }

    /**
     * What launching a release would run.
     *
     * @param releases    everything the app runs with
     * @param warnings    resolution warnings; empty for a nearest-wins classpath, which skips resolution
     * @param conflicts   if any, the release cannot be launched
     * @param jars        the exact jars, the app's own first
     * @param dir         where the release is installed
     * @param label       what the app calls itself while it runs
     */
    public record Prepared(Version<?> app, boolean nearestWins, List<Version<?>> releases, List<Warning> warnings,
                           List<Violation> conflicts, List<Coordinates> jars, Path dir, String label) {

        public boolean canLaunch() {
            return conflicts.isEmpty();
        }

        public boolean isInstalled() {
            return Files.isRegularFile(dir.resolve("installed.txt"));
        }

        public Path log() {
            return dir.resolve("app.log");
        }
    }

    private final Settings settings;
    private final RuntimePlan plan = new RuntimePlan();
    private final VersionText text = new VersionText();

    public AppLauncher(final Settings settings) {
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
    }

    /**
     * @param nearestWins build the classpath the way a build tool's "nearest declaration
     *                    wins" rule would, instead of the model's resolution, to show what
     *                    goes wrong; installed in a directory of its own
     */
    public Prepared prepare(final Version<?> app, final boolean nearestWins) {
        final List<Version<?>> releases;
        final List<Warning> warnings;
        final List<Violation> conflicts;
        if (nearestWins) {
            releases = plan.nearestWins(app);
            warnings = List.of();
            conflicts = List.of();
        } else {
            final Resolution resolution = new Resolver().resolve(app);
            releases = List.copyOf(resolution.resolved().versions());
            warnings = resolution.warnings().items();
            conflicts = resolution.conflicts().items();
        }
        final List<Coordinates> jars = conflicts.isEmpty() ? plan.jars(app, releases) : List.of();
        final String unit = new VersionUnit().of(app.getClass()).orElseThrow().getSimpleName();
        final Path dir = settings.appsDir().resolve(unit).resolve(text.number(app) + (nearestWins ? "-nearest-wins" : ""));
        final String label = text.named(app) + (nearestWins ? " [nearest-wins classpath]" : "");
        return new Prepared(app, nearestWins, releases, warnings, conflicts, jars, dir, label);
    }

    /**
     * @param reinstall install again whatever is there, accepting a repository whose bytes
     *                  for a release have changed
     * @throws IllegalStateException if the release's dependencies conflict
     * @see Installer
     */
    public Installer.Installation install(final Prepared prepared, final boolean reinstall) throws IOException {
        if (!prepared.canLaunch()) {
            throw new IllegalStateException(text.named(prepared.app()) + " does not resolve: " + prepared.conflicts());
        }
        return new Installer().install(settings.repositories(), prepared.dir(), prepared.jars(), reinstall);
    }

    /** Starts the installed release; its output goes to {@link Prepared#log()}. */
    public Process start(final Prepared prepared, final Installer.Installation installation) throws IOException {
        final String mainClass;
        try (JarFile appJar = new JarFile(installation.jars().get(0).toFile())) {
            mainClass = appJar.getManifest().getMainAttributes().getValue("Main-Class");
        }
        final List<String> command = List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "--enable-native-access=ALL-UNNAMED",
                "-Dacme.release=" + prepared.label(),
                "-cp", installation.jars().stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)),
                mainClass);
        return new ProcessBuilder(command)
                .directory(prepared.dir().toFile())
                .redirectErrorStream(true)
                .redirectOutput(prepared.log().toFile())
                .start();
    }
}
