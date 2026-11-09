package com.example.fxsuite.launch;

import com.example.fxsuite.deployment.DeploymentUnit;
import com.example.fxsuite.deployment.Exposure;
import com.example.fxsuite.deployment.Lineage;
import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.deployment.Violation;
import com.example.fxsuite.deployment.VirtualVersion;
import com.example.fxsuite.deployment.Warning;
import com.example.fxsuite.estate.DemoEstate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The command line for {@link AppLauncher}: launches one release of a demo-estate application.
 *
 * <ol>
 *   <li>resolves its dependency graph with the deployment model, refusing a conflict;</li>
 *   <li>works out the exact jars: a virtual release runs its base's jar, a bundle brings its
 *       members;</li>
 *   <li>copies them from Maven-layout repositories into a directory of its own,
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
            System.err.println("usage: estate-launcher <App> <version> [--nearest-wins] [--install-only] [--reinstall]"
                    + " [--wait] [--repo <dir>] [--apps-dir <dir>]\n       estate-launcher --list");
            return 1;
        }
        final Optional<Version<?>> found = catalogue.find(positional.get(0), positional.get(1));
        if (found.isEmpty()) {
            System.err.println("no release " + positional.get(0) + " " + positional.get(1) + "; see --list");
            return 1;
        }
        final Version<?> app = found.get();
        final AppLauncher launcher = new AppLauncher(AppLauncher.Settings.defaults(
                option(args, "--repo").map(Path::of), option(args, "--apps-dir").map(Path::of)));
        final AppLauncher.Prepared prepared = launcher.prepare(app, args.contains("--nearest-wins"));

        System.out.println(text.named(app) + (app instanceof VirtualVersion<?, ?>
                ? "   (virtual: runs the binary of " + text.named(new Lineage().physicalOf(app)) + ")" : ""));
        if (prepared.nearestWins()) {
            System.out.println("\nNEAREST WINS, not the model's resolution: closest declaration of each unit,"
                    + " lowest at equal depth");
        }
        for (final Violation conflict : prepared.conflicts()) {
            System.err.println("CONFLICT " + conflict.message());
        }
        if (!prepared.canLaunch()) {
            System.err.println("Refusing to launch a release whose dependencies do not resolve.");
            return 2;
        }
        for (final Warning warning : prepared.warnings()) {
            System.out.println("\nWARNING " + warning.message());
        }

        for (final Exposure exposure : prepared.exposures()) {
            System.out.println("\nINSECURE " + exposure.advisory().name() + " " + exposure.advisory().severity() + ": "
                    + exposure.advisory().summary() + "\n  " + exposure.jar().coordinates() + " via "
                    + text.named(exposure.bundle()) + ", fixed in " + exposure.fixedIn());
        }

        System.out.println("\nRuns with " + prepared.releases().size() + " releases:");
        prepared.releases().stream().sorted(Comparator.comparing(text::named))
                .forEach(v -> System.out.println("  " + text.named(v)));
        System.out.println("\nJars (" + prepared.jars().size() + "):");
        prepared.jars().forEach(jar -> System.out.println("  " + jar));

        final Installer.Installation installation;
        try {
            installation = launcher.install(prepared, args.contains("--reinstall"));
        } catch (Installer.ReleaseChanged e) {
            System.err.println("\n" + e.getMessage() + "\nRefusing to launch. A real fix publishes a new version;"
                    + " --reinstall takes the new bytes anyway.");
            return 5;
        } catch (Installer.InUse e) {
            System.err.println("\n" + e.getMessage());
            return 6;
        } catch (Installer.MissingJars e) {
            System.err.println("\n" + e.getMessage()
                    + "\nPublish the sample estate first, from the repository root: mvn -f sample-estate/pom.xml deploy");
            return 3;
        } catch (IOException e) {
            System.err.println("\ncould not install into " + prepared.dir() + ": " + e);
            return 3;
        }
        System.out.println("\n" + prepared.dir() + "\n  " + installation.outcome());

        if (args.contains("--install-only")) {
            return 0;
        }
        try {
            final Process process = launcher.start(prepared, installation);
            System.out.println("\nStarted, pid " + process.pid() + "; its output goes to " + prepared.log());
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

    private void list() {
        for (final Class<? extends DeploymentUnit> app : catalogue.applications()) {
            System.out.println(app.getSimpleName());
            for (final Version<?> release : catalogue.releasesOf(app)) {
                System.out.println("  " + text.number(release)
                        + (release instanceof VirtualVersion<?, ?> ? "   virtual" : ""));
            }
        }
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
