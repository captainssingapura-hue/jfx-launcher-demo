package com.example.fxsuite.launch;

import com.example.fxsuite.deployment.DeploymentUnit;
import com.example.fxsuite.deployment.Lineage;
import com.example.fxsuite.deployment.Resolution;
import com.example.fxsuite.deployment.Resolver;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.deployment.VersionUnit;
import com.example.fxsuite.deployment.Violation;
import com.example.fxsuite.deployment.VirtualVersion;
import com.example.fxsuite.deployment.Warning;
import com.example.fxsuite.estate.DemoEstate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Launches one release of a demo-estate application:
 *
 * <ol>
 *   <li>resolves its dependency graph with the deployment model, refusing a conflict;</li>
 *   <li>works out the exact jars: a virtual release runs its base's jar, a bundle brings its
 *       members;</li>
 *   <li>copies them from a Maven-layout repository into a directory of its own,
 *       {@code <apps dir>/<app>/<version>/lib}, checking each against its recorded checksum;</li>
 *   <li>starts the app in its own JVM with exactly those jars as its classpath; its output
 *       goes to {@code app.log} beside them.</li>
 * </ol>
 *
 * <pre>
 * estate-launcher --list
 * estate-launcher Ledger 1.20261101.1 [--nearest-wins] [--install-only] [--reinstall] [--wait]
 *                 [--repo &lt;dir&gt;] [--apps-dir &lt;dir&gt;]
 * </pre>
 *
 * Each jar comes from the first repository that has it: {@code --repo} if given, then
 * {@code dist/acme-repo} under the current directory, where
 * {@code mvn -f sample-estate/pom.xml deploy} publishes the estate's own jars, then the local
 * Maven repository ({@code ~/.m2/repository}) for the external ones. The apps directory
 * defaults to {@code %LOCALAPPDATA%\fxsuite\apps}.
 *
 * <p>{@code --nearest-wins} builds the classpath the way a build tool's "nearest declaration
 * wins" rule would, instead of the model's resolution, into a directory of its own, to show
 * what goes wrong.
 */
public final class EstateLauncher {

    private final Catalogue catalogue = new Catalogue(new DemoEstate().units());
    private final RuntimePlan plan = new RuntimePlan();
    private final VersionText text = new VersionText();

    public static void main(final String[] args) {
        System.exit(new EstateLauncher().run(List.of(args)));
    }

    int run(final List<String> args) {
        if (args.contains("--list")) {
            list();
            return 0;
        }
        final List<String> positional = args.stream().filter(a -> !a.startsWith("--")).filter(a -> !isOptionValue(args, a))
                .toList();
        if (positional.size() != 2) {
            System.err.println("usage: estate-launcher <App> <version> [--nearest-wins] [--install-only] [--reinstall] [--wait]"
                    + " [--repo <dir>] [--apps-dir <dir>]\n       estate-launcher --list");
            return 1;
        }
        final Optional<Version<?>> found = catalogue.find(positional.get(0), positional.get(1));
        if (found.isEmpty()) {
            System.err.println("no release " + positional.get(0) + " " + positional.get(1) + "; see --list");
            return 1;
        }
        final Version<?> app = found.get();
        final boolean nearestWins = args.contains("--nearest-wins");
        final List<Path> repositories = new ArrayList<>();
        option(args, "--repo").map(Path::of).ifPresent(repositories::add);
        repositories.add(Path.of("dist", "acme-repo").toAbsolutePath());
        repositories.add(Path.of(System.getProperty("user.home"), ".m2", "repository"));
        final Path appsDir = option(args, "--apps-dir").map(Path::of).orElse(defaultAppsDir());

        System.out.println(text.named(app) + (app instanceof VirtualVersion<?, ?>
                ? "   (virtual: runs the binary of " + text.named(new Lineage().physicalOf(app)) + ")" : ""));

        final Collection<? extends Version<?>> releases;
        if (nearestWins) {
            releases = plan.nearestWins(app);
            System.out.println("\nNEAREST WINS, not the model's resolution: closest declaration of each unit,"
                    + " lowest at equal depth");
        } else {
            final Resolution resolution = new Resolver().resolve(app);
            for (final Violation conflict : resolution.conflicts().items()) {
                System.err.println("CONFLICT " + conflict.message());
            }
            if (!resolution.isValid()) {
                System.err.println("Refusing to launch a release whose dependencies do not resolve.");
                return 2;
            }
            releases = resolution.resolved().versions();
            for (final Warning warning : resolution.warnings().items()) {
                System.out.println("\nWARNING " + warning.message());
            }
        }

        System.out.println("\nRuns with " + releases.size() + " releases:");
        releases.stream().<Version<?>>map(v -> v).sorted(Comparator.comparing(text::named))
                .forEach(v -> System.out.println("  " + text.named(v)));

        final List<Coordinates> jars = plan.jars(app, new ArrayList<>(releases));
        System.out.println("\nJars (" + jars.size() + "):");
        jars.forEach(jar -> System.out.println("  " + jar));

        final String unit = new VersionUnit().of(app.getClass()).orElseThrow().getSimpleName();
        final Path dir = appsDir.resolve(unit).resolve(text.number(app) + (nearestWins ? "-nearest-wins" : ""));
        final Installer.Installation installation;
        try {
            installation = new Installer().install(repositories, dir, jars, args.contains("--reinstall"));
        } catch (Installer.ReleaseChanged e) {
            System.err.println("\n" + e.getMessage() + "\nRefusing to launch. A real fix publishes a new version;"
                    + " --reinstall takes the new bytes anyway.");
            return 5;
        } catch (Installer.InUse e) {
            System.err.println("\n" + e.getMessage());
            return 6;
        } catch (Installer.MissingJars e) {
            System.err.println("\n" + e.getMessage()
                    + "\nBuild and install the sample estate first: mvn -f sample-estate/pom.xml install");
            return 3;
        } catch (IOException e) {
            System.err.println("\ncould not install into " + dir + ": " + e);
            return 3;
        }
        System.out.println("\n" + dir + "\n  " + installation.outcome());

        if (args.contains("--install-only")) {
            return 0;
        }
        try {
            final String label = text.named(app) + (nearestWins ? " [nearest-wins classpath]" : "");
            final Process process = start(label, dir, installation.jars());
            System.out.println("\nStarted, pid " + process.pid() + "; its output goes to " + dir.resolve("app.log"));
            if (args.contains("--wait")) {
                return process.waitFor();
            }
            return 0;
        } catch (IOException e) {
            System.err.println("\ncould not start: " + e);
            return 4;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 4;
        }
    }

    /** @param label what the app calls itself: the release, and how its classpath was made */
    private Process start(final String label, final Path dir, final List<Path> jars) throws IOException {
        final String mainClass;
        try (JarFile appJar = new JarFile(jars.get(0).toFile())) {
            mainClass = appJar.getManifest().getMainAttributes().getValue("Main-Class");
        }
        final List<String> command = List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "--enable-native-access=ALL-UNNAMED",
                "-Dacme.release=" + label,
                "-cp", jars.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)),
                mainClass);
        return new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(dir.resolve("app.log").toFile())
                .start();
    }

    private void list() {
        for (final Class<? extends DeploymentUnit> app : catalogue.applications()) {
            System.out.println(app.getSimpleName());
            for (final Version<?> release : catalogue.releasesOf(app)) {
                System.out.println("  " + text.number(release)
                        + (release instanceof VirtualVersion<?, ?> ? "   virtual" : ""));
            }
        }
    }

    private Path defaultAppsDir() {
        final String local = System.getenv("LOCALAPPDATA");
        return (local != null ? Path.of(local) : Path.of(System.getProperty("user.home"), ".local", "share"))
                .resolve("fxsuite").resolve("apps");
    }

    private Optional<String> option(final List<String> args, final String name) {
        final int at = args.indexOf(name);
        return at >= 0 && at + 1 < args.size() ? Optional.of(args.get(at + 1)) : Optional.empty();
    }

    private boolean isOptionValue(final List<String> args, final String value) {
        final int at = args.indexOf(value);
        return at > 0 && List.of("--repo", "--apps-dir").contains(args.get(at - 1));
    }
}
