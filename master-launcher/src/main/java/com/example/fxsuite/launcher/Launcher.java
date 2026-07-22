package com.example.fxsuite.launcher;

import com.example.fxsuite.launcher.token.LaunchToken;
import com.example.fxsuite.launcher.token.TokenVerifier;

import java.nio.file.Path;
import java.util.List;

/**
 * Process entry point and gatekeeper for the FxSuite launcher.
 *
 * <p>Each installed instance serves exactly <b>one environment</b>, declared by the
 * {@link com.example.fxsuite.launcher.env.EnvSpec} its build carries. A
 * {@link com.example.fxsuite.launcher.env.SingletonEnv} build (Prod, UAT) knows its own
 * environment; a {@link com.example.fxsuite.launcher.env.MultiplexedEnv} build (dev) is told
 * which of its family it is via {@code --env=} on the registered command line. The
 * environment therefore comes from the build + its registration — never from the launch
 * URL.</p>
 *
 * <pre>
 *   --register --env=&lt;id&gt; [--base=&lt;url&gt;]   install the fxsuite-&lt;id&gt;:// handler
 *   --unregister --env=&lt;id&gt;                 remove it
 *   --list                                   show registered environments
 *   --prune                                  drop registrations whose jar is gone
 *   fxsuite-&lt;id&gt;://launch/&lt;app&gt;?tok=…        launch (normally invoked by the OS)
 * </pre>
 */
public final class Launcher {

    private enum Mode { LAUNCH, REGISTER, UNREGISTER, LIST, PRUNE, HELP, UI }

    private Launcher() {}

    public static void main(String[] args) {
        String argEnv = null, argBase = null, url = null;
        Mode mode = null;

        for (String a : args) {
            String s = a.trim();
            if (s.startsWith("--env=")) argEnv = s.substring("--env=".length());
            else if (s.startsWith("--base=")) argBase = s.substring("--base=".length());
            else if (s.equals("--register")) mode = Mode.REGISTER;
            else if (s.equals("--unregister")) mode = Mode.UNREGISTER;
            else if (s.equals("--list")) mode = Mode.LIST;
            else if (s.equals("--prune")) mode = Mode.PRUNE;
            else if (s.equals("--help") || s.equals("-h") || s.equals("/?")) mode = Mode.HELP;
            else if (!s.startsWith("--")) url = s;      // the launch URL
        }
        // No arguments at all means someone opened the jar directly (double-click), so show
        // the launcher UI rather than printing help nobody can see.
        if (mode == null) mode = (url != null) ? Mode.LAUNCH : Mode.UI;

        EnvConfig env = EnvConfig.load(argEnv, argBase);
        DiagLog.setEnv(env.envId());   // scope the log file to this environment

        try {
            switch (mode) {
                case REGISTER -> ProtocolRegistrar.register(env.requireEnvId(), argBase);
                case UNREGISTER -> ProtocolRegistrar.unregister(env.requireEnvId());
                case LIST -> ProtocolRegistrar.list();
                case PRUNE -> ProtocolRegistrar.prune();
                case HELP -> printUsage();
                case UI -> javafx.application.Application.launch(
                        com.example.fxsuite.launcher.ui.LauncherUiApp.class);
                case LAUNCH -> launch(url, env);
            }
        } catch (LaunchException e) {
            DiagLog.log("error: " + e.getMessage());
            // A console invocation reports to the console. Only the paths the OS starts —
            // a protocol URL or a double-click — have no console to report to, and there a
            // modal dialog is the message; on the CLI it would just hang the command.
            if (mode == Mode.LAUNCH || mode == Mode.UI) UserAlert.error(e.getMessage());
            else System.err.println("error: " + e.getMessage());
        }
    }

    /** Launch mode: {@code raw} is (expected to be) an {@code fxsuite-<env>://} URL. */
    private static void launch(String raw, EnvConfig env) {
        DiagLog.log("launch invoked [" + env.envId() + "] with arg: " + raw);
        try {
            String ownEnv = env.requireEnvId();
            LaunchUri uri = LaunchUri.parse(raw);

            // The scheme carries the environment too; refuse anything not addressed to us.
            if (!ownEnv.equals(uri.env())) {
                throw new LaunchException("This launcher serves environment '" + ownEnv
                        + "' but the link was for '" + uri.env() + "'.");
            }

            // Security gate: signature (this environment's key), env / app binding,
            // version and artifact hash, expiry.
            LaunchToken token = new TokenVerifier(ownEnv).verify(uri.token(), ownEnv, uri.appId());
            DiagLog.log("token OK: env='" + token.env() + "' app='" + token.app()
                    + "' ver='" + token.ver() + "' (jti=" + token.jti() + ")");

            // A managed app carried by this launcher launches directly from its own jar —
            // no download. Anything not bundled falls back to the pinned repo.
            var bundled = env.bundledApp(token.app());
            if (bundled.isPresent()) {
                DiagLog.log("launching bundled app '" + token.app() + "' (bundled v"
                        + bundled.get().version() + ", requested v" + token.ver() + ")");
                AppSpawner.spawnBundled(ownEnv, bundled.get().mainClassName(),
                        List.of(token.app(), token.ver()));
            } else {
                Path appJar = new AppFetcher(env).fetch(token.app(), token.ver(), token.sha256());
                String mainClass = Manifests.mainClass(appJar);
                AppSpawner.spawn(ownEnv, appJar, mainClass, List.of(token.app(), token.ver()));
            }
        } catch (LaunchException e) {
            DiagLog.log("rejected launch: " + e.getMessage());
            UserAlert.error(e.getMessage());
        }
    }

    private static void printUsage() {
        System.out.println("""
                FxSuite launcher — one instance per environment

                Usage:
                  java -jar master-launcher.jar --register --env=<id> [--base=<url>]
                  java -jar master-launcher.jar --unregister --env=<id>
                  java -jar master-launcher.jar --list
                  java -jar master-launcher.jar --prune
                  java -jar master-launcher.jar fxsuite-<id>://launch/<app>?tok=<token>
                  java -jar master-launcher.jar --help

                Singleton environments (prod, uat) have a dedicated build that knows its
                own environment, so --env is unnecessary and a conflicting one is refused.
                Multiplexed dev environments share one build and must pass --env=devN
                (optionally --base) at registration.""");
    }
}
