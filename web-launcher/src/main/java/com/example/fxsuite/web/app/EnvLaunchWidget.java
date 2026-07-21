package com.example.fxsuite.web.app;

import hue.captains.singapura.js.homing.core.Importable;
import hue.captains.singapura.js.homing.core.ModuleImports;
import hue.captains.singapura.js.homing.core.Widget;
import hue.captains.singapura.js.homing.studio.base.widget.DocWidget;

import java.util.List;

/**
 * The environment launch panel: one row per environment, each of which asks the
 * backend for a freshly signed launch token and then hands the resulting
 * {@code fxsuite-<env>://} URL to the OS.
 *
 * <p>The browser-side behaviour lives in {@link #bodyJs()} — homing's widget
 * contract ships these lines to the page, so the interactive logic that used to
 * sit in a hand-written {@code <script>} block is now part of the widget.</p>
 */
public final class EnvLaunchWidget extends DocWidget<EnvLaunchWidget.Params, EnvLaunchWidget> {

    public static final EnvLaunchWidget INSTANCE = new EnvLaunchWidget();
    private EnvLaunchWidget() {}

    public record Params() implements Widget._Param {}

    private record mountInto() implements Widget._MountInto<Params, EnvLaunchWidget> {}

    @Override public String simpleName() { return "env-launch-widget"; }
    @Override public Class<Params> paramsType() { return Params.class; }
    @Override public String title() { return "Launch by environment"; }

    @Override
    protected Widget._MountInto<Params, EnvLaunchWidget> mountInto() {
        return new mountInto();
    }

    @Override
    protected List<ModuleImports<? extends Importable>> bodyImports() {
        return List.of();   // plain DOM + fetch; no framework modules needed
    }

    @Override
    protected List<String> bodyJs() {
        return List.of(
                "    var owner = Object.freeze({ toString: function(){ return 'envLaunch'; } });",
                "    var b = branch.createBranch('env-launch');",
                "    b.activate(owner);",
                "",
                "    // /token is served by the studio itself (contributed via Fixtures),",
                "    // so this is same-origin — no CORS involved.",
                "    var ENVS = [",
                "        { id:'prod', label:'Production', colour:'#c62828',",
                "          note:'dedicated install + its own signing key · version pinned' },",
                "        { id:'uat',  label:'UAT',        colour:'#ef6c00',",
                "          note:'dedicated install · release candidate' },",
                "        { id:'dev1', label:'dev1',       colour:'#1565c0',",
                "          note:'shared dev install, --env=dev1 · latest' },",
                "        { id:'dev2', label:'dev2',       colour:'#1565c0',",
                "          note:'same install, --env=dev2 · latest' }",
                "    ];",
                "",
                "    var host = b.createElement('host', 'div');",
                "    host.style.cssText = 'display:flex;flex-direction:column;gap:10px;max-width:760px;';",
                "    parent.appendChild(host);",
                "",
                "    var status = document.createElement('div');",
                "    status.style.cssText = 'min-height:1.6em;margin-top:6px;color:#5b6478;'",
                "                         + 'font:14px system-ui,sans-serif;';",
                "",
                "    ENVS.forEach(function(env){",
                "        var row = document.createElement('div');",
                "        row.style.cssText = 'display:flex;align-items:center;gap:14px;padding:11px 14px;'",
                "                          + 'border:1px solid #d3dae8;border-radius:10px;background:#fff;';",
                "        var pill = document.createElement('span');",
                "        pill.textContent = env.id.toUpperCase();",
                "        pill.style.cssText = 'font:700 12px ui-monospace,Consolas,monospace;color:#fff;'",
                "                           + 'padding:5px 10px;border-radius:999px;min-width:62px;'",
                "                           + 'text-align:center;background:' + env.colour + ';';",
                "        var meta = document.createElement('span');",
                "        meta.style.cssText = 'flex:1;color:#5b6478;font:14px system-ui,sans-serif;';",
                "        meta.innerHTML = '<b style=\"color:#16203a\">' + env.label + '</b> · '",
                "                       + '<code>fxsuite-' + env.id + '://</code><br>'",
                "                       + '<small>' + env.note + '</small>';",
                "        var btn = document.createElement('button');",
                "        btn.textContent = 'Launch';",
                "        btn.style.cssText = 'font:600 14px system-ui,sans-serif;cursor:pointer;border:0;'",
                "                          + 'border-radius:8px;padding:8px 18px;background:#2748c8;color:#fff;';",
                "        btn.addEventListener('click', function(){ launch(env.id); });",
                "        row.appendChild(pill); row.appendChild(meta); row.appendChild(btn);",
                "        host.appendChild(row);",
                "    });",
                "    host.appendChild(status);",
                "",
                "    function launch(envId) {",
                "        status.textContent = 'Requesting a launch token for ' + envId + '…';",
                "        fetch('/token?app=hello&env=' + encodeURIComponent(envId))",
                "            .then(function(r){",
                "                return r.json().then(function(j){ return { ok: r.ok, body: j }; });",
                "            })",
                "            .then(function(res){",
                "                if (!res.ok) { throw new Error(res.body.error || 'token request failed'); }",
                "                var d = res.body;",
                "                status.innerHTML = 'Launching <b>' + d.env + '</b> · hello v' + d.ver",
                "                                 + (d.dedicatedKey ? ' · <i>dedicated signing key</i>'",
                "                                                   : ' · shared dev key');",
                "                window.location.href = d.url;   // hand fxsuite-<env>:// to the OS",
                "            })",
                "            .catch(function(e){ status.textContent = 'Could not launch: ' + e.message; });",
                "    }"
        );
    }
}
