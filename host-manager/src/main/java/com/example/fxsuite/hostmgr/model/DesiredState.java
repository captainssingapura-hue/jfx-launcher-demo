package com.example.fxsuite.hostmgr.model;

import com.example.fxsuite.hostmgr.json.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The whole declared intent for a host (or for every host, when {@code host} is {@code "*"}).
 *
 * <p>{@code generation} increases each time an operator changes the declared state; the agent
 * reports the generation it last converged, so the control plane can see which hosts are
 * caught up.</p>
 */
public record DesiredState(long generation, String host, List<AppSpec> apps) {

    /** Whether this manifest applies to the given host id. */
    public boolean appliesTo(String hostId) {
        return host == null || host.equals("*") || host.equals(hostId);
    }

    /** Only the apps that should actually be running. */
    public List<AppSpec> enabledApps() {
        List<AppSpec> out = new ArrayList<>();
        for (AppSpec a : apps) if (a.enabled()) out.add(a);
        return out;
    }

    public static DesiredState parse(String json) {
        Map<String, Object> o = Json.object(Json.parse(json));
        List<AppSpec> apps = new ArrayList<>();
        Object appsNode = o.get("apps");
        if (appsNode != null) {
            for (Object item : Json.array(appsNode)) apps.add(AppSpec.from(Json.object(item)));
        }
        return new DesiredState(Json.lng(o, "generation", 0), Json.str(o, "host"), apps);
    }
}
