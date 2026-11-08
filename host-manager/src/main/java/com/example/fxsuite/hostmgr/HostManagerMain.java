package com.example.fxsuite.hostmgr;

import com.example.fxsuite.hostmgr.control.ControlClient;
import com.example.fxsuite.hostmgr.control.ControlServer;
import com.example.fxsuite.hostmgr.supervisor.DirectSupervisor;
import com.example.fxsuite.hostmgr.supervisor.Supervisor;
import com.example.fxsuite.hostmgr.supervisor.SystemdUserSupervisor;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * One jar, two modes:
 *
 * <pre>
 *   control --port=9099 --desired=desired.json
 *       Run the centralized control plane: serve the signed desired state and the ops board.
 *
 *   agent --control=http://host:9099 [--host=&lt;id&gt;] [--supervisor=direct|systemd]
 *         [--repo=http://host:8090] [--work=&lt;dir&gt;] [--interval=5]
 *       Run the on-host agent loop: pull → reconcile → report.
 * </pre>
 *
 * <p>The supervisor choice is the permission lane: {@code systemd} for lane A (Linux, user
 * units), {@code direct} for lane C (anywhere, no privilege). Everything else is identical
 * between them.</p>
 */
public final class HostManagerMain {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { usage(); return; }
        Map<String, String> o = parse(args);
        switch (args[0]) {
            case "control" -> control(o);
            case "agent" -> agent(o);
            default -> usage();
        }
    }

    private static void control(Map<String, String> o) throws Exception {
        int port = Integer.parseInt(o.getOrDefault("port", "9099"));
        Path desired = Path.of(o.getOrDefault("desired", "desired.json")).toAbsolutePath();
        new ControlServer(port, desired).start();
        Thread.currentThread().join();
    }

    private static void agent(Map<String, String> o) throws Exception {
        String controlBase = o.getOrDefault("control", "http://localhost:9099");
        String repoBase = o.getOrDefault("repo", "http://localhost:8090");
        String host = o.getOrDefault("host", hostname());
        long interval = Long.parseLong(o.getOrDefault("interval", "5")) * 1000L;
        Path work = Path.of(o.getOrDefault("work",
                System.getenv().getOrDefault("LOCALAPPDATA", System.getProperty("java.io.tmpdir"))
                        + "/fxsuite/hostmgr")).toAbsolutePath();
        Path appsRoot = work.resolve("apps");

        Supervisor supervisor = switch (o.getOrDefault("supervisor", "direct")) {
            case "systemd" -> new SystemdUserSupervisor(appsRoot, true);
            case "direct" -> new DirectSupervisor(work);
            default -> throw new IllegalArgumentException("supervisor must be direct or systemd");
        };

        HostAgent agent = new HostAgent(host, new ControlClient(controlBase),
                new Artifacts(appsRoot, repoBase), supervisor, interval);
        agent.run();
    }

    private static String hostname() {
        try { return InetAddress.getLocalHost().getHostName(); } catch (Exception e) { return "unknown-host"; }
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> o = new HashMap<>();
        for (String a : args) {
            if (a.startsWith("--")) {
                int eq = a.indexOf('=');
                if (eq > 0) o.put(a.substring(2, eq), a.substring(eq + 1));
                else o.put(a.substring(2), "true");
            }
        }
        return o;
    }

    private static void usage() {
        System.out.println("""
                FxSuite host manager — declarative deployment + day-to-day ops

                  control --port=9099 --desired=desired.json
                  agent   --control=http://host:9099 [--host=<id>] [--supervisor=direct|systemd]
                          [--repo=http://host:8090] [--work=<dir>] [--interval=5]
                """);
    }
}
