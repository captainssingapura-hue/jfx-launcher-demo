package com.example.fxsuite.launcher;

import com.example.fxsuite.launcher.token.LaunchToken;
import com.example.fxsuite.launcher.token.TokenVerifier;

import java.nio.file.Path;
import java.util.List;

/**
 * Process entry point and gatekeeper for the FxSuite launcher.
 *
 * <p>This jar is thin and JavaFX-free, and ships <b>no app jars</b>. In launch
 * mode it verifies the request, fetches the exact app version named in the
 * (signed) token from the pinned repository — downloading + caching on first use
 * — checks the bytes against the token's signed hash, and spawns the app in its
 * own process. Two responsibilities, selected by the first argument:</p>
 * <ul>
 *   <li>{@code --register} / {@code --unregister} — install/remove the per-user
 *       {@code fxsuite://} protocol handler;</li>
 *   <li>an {@code fxsuite://…} URL — the OS invokes us this way after a web
 *       click.</li>
 * </ul>
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String first = args[0].trim();
        switch (first) {
            case "--register" -> ProtocolRegistrar.register();
            case "--unregister" -> ProtocolRegistrar.unregister();
            case "--help", "-h", "/?" -> printUsage();
            default -> launch(args[0]);
        }
    }

    /** Launch mode: {@code raw} is (expected to be) an {@code fxsuite://} URL. */
    private static void launch(String raw) {
        DiagLog.log("launch invoked with arg: " + raw);
        try {
            LaunchUri uri = LaunchUri.parse(raw);

            // Security gate: a valid, unexpired, app-bound, server-signed token is
            // required. It also names the version and the expected jar hash.
            LaunchToken token = new TokenVerifier().verify(uri.token(), uri.appId());
            DiagLog.log("token OK: app='" + token.app() + "' ver='" + token.ver()
                    + "' (jti=" + token.jti() + ")");

            // Resolve the exact version from the pinned repo (cache or download),
            // verifying the downloaded bytes against the token's signed hash.
            Path appJar = new AppFetcher().fetch(token.app(), token.ver(), token.sha256());

            // Entry point comes from the verified jar itself.
            String mainClass = Manifests.mainClass(appJar);

            AppSpawner.spawn(appJar, mainClass, List.of(token.app(), token.ver()));
        } catch (LaunchException e) {
            DiagLog.log("rejected launch: " + e.getMessage());
            UserAlert.error(e.getMessage());
        }
    }

    private static void printUsage() {
        System.out.println("""
                FxSuite master launcher (gatekeeper)

                Usage:
                  java -jar master-launcher.jar --register       Install the fxsuite:// handler (HKCU)
                  java -jar master-launcher.jar --unregister     Remove the handler
                  java -jar master-launcher.jar fxsuite://launch/<app>?tok=<token>
                                                                 Launch an app version named in the token
                                                                 (normally done by the OS on a web click)
                  java -jar master-launcher.jar --help           This message

                Apps are resolved dynamically from the repository named in
                launcher.properties (repo.base); nothing is hardcoded here.""");
    }
}
