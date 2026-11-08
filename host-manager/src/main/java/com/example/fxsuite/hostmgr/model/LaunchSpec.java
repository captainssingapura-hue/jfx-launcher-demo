package com.example.fxsuite.hostmgr.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Everything a supervisor needs to run one unit — the resolved, verified artifact plus how to
 * start it. Built by the agent once an {@link AppSpec} has been staged (downloaded and
 * hash-checked), so a supervisor never touches the network or the trust decision.
 *
 * @param jar      the concrete, already-verified versioned artifact on disk
 * @param args     arguments passed to the app (its environment and version)
 * @param memoryMax a systemd resource limit like {@code "512M"}; ignored by supervisors that
 *                  cannot enforce it (noted when so)
 */
public record LaunchSpec(UnitId id, String version, Path jar, List<String> args, String memoryMax) {}
