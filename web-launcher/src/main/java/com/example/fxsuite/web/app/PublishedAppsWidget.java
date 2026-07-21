package com.example.fxsuite.web.app;

import hue.captains.singapura.js.homing.core.Importable;
import hue.captains.singapura.js.homing.core.ModuleImports;
import hue.captains.singapura.js.homing.core.Widget;
import hue.captains.singapura.js.homing.studio.base.widget.DocWidget;

import java.util.List;

/**
 * Shows everything published in the artifact repository (app, version, artifact
 * hash) alongside the per-environment version policy, so it is obvious which
 * version each environment will actually launch.
 */
public final class PublishedAppsWidget extends DocWidget<PublishedAppsWidget.Params, PublishedAppsWidget> {

    public static final PublishedAppsWidget INSTANCE = new PublishedAppsWidget();
    private PublishedAppsWidget() {}

    public record Params() implements Widget._Param {}

    private record mountInto() implements Widget._MountInto<Params, PublishedAppsWidget> {}

    @Override public String simpleName() { return "published-apps-widget"; }
    @Override public Class<Params> paramsType() { return Params.class; }
    @Override public String title() { return "Published versions"; }

    @Override
    protected Widget._MountInto<Params, PublishedAppsWidget> mountInto() {
        return new mountInto();
    }

    @Override
    protected List<ModuleImports<? extends Importable>> bodyImports() {
        return List.of();
    }

    @Override
    protected List<String> bodyJs() {
        return List.of(
                "    var owner = Object.freeze({ toString: function(){ return 'publishedApps'; } });",
                "    var b = branch.createBranch('published-apps');",
                "    b.activate(owner);",
                "",
                "    ",
                "",
                "    var host = b.createElement('host', 'div');",
                "    host.style.cssText = 'max-width:820px;font:14px system-ui,sans-serif;';",
                "    parent.appendChild(host);",
                "",
                "    var policy = document.createElement('p');",
                "    policy.style.cssText = 'color:#5b6478;';",
                "    policy.innerHTML = 'Version policy — <b>prod</b>: pinned · <b>uat</b>: release '",
                "                     + 'candidate · <b>dev*</b>: latest published.';",
                "    host.appendChild(policy);",
                "",
                "    var table = document.createElement('table');",
                "    table.style.cssText = 'width:100%;border-collapse:collapse;background:#fff;';",
                "    table.innerHTML = '<thead><tr>'",
                "        + '<th style=\"text-align:left;padding:8px 10px;border-bottom:2px solid #d3dae8\">App</th>'",
                "        + '<th style=\"text-align:left;padding:8px 10px;border-bottom:2px solid #d3dae8\">Version</th>'",
                "        + '<th style=\"text-align:left;padding:8px 10px;border-bottom:2px solid #d3dae8\">SHA-256</th>'",
                "        + '</tr></thead><tbody></tbody>';",
                "    host.appendChild(table);",
                "",
                "    var body = table.querySelector('tbody');",
                "    body.innerHTML = '<tr><td colspan=3 style=\"padding:10px;color:#5b6478\">loading…</td></tr>';",
                "",
                "    fetch('/catalog')",
                "        .then(function(r){ return r.json(); })",
                "        .then(function(data){",
                "            var items = data.items || [];",
                "            if (!items.length) {",
                "                body.innerHTML = '<tr><td colspan=3 style=\"padding:10px;color:#5b6478\">'",
                "                               + 'nothing published</td></tr>';",
                "                return;",
                "            }",
                "            body.innerHTML = items.map(function(i){",
                "                return '<tr>'",
                "                     + '<td style=\"padding:8px 10px;border-bottom:1px solid #eef1f8\">' + i.app + '</td>'",
                "                     + '<td style=\"padding:8px 10px;border-bottom:1px solid #eef1f8;'",
                "                     + 'font-family:ui-monospace,Consolas,monospace\">' + i.ver + '</td>'",
                "                     + '<td style=\"padding:8px 10px;border-bottom:1px solid #eef1f8;'",
                "                     + 'font-family:ui-monospace,Consolas,monospace;color:#5b6478\">'",
                "                     + i.sha256.slice(0,16) + '…</td></tr>';",
                "            }).join('');",
                "        })",
                "        .catch(function(e){",
                "            body.innerHTML = '<tr><td colspan=3 style=\"padding:10px;color:#b71c1c\">'",
                "                           + 'could not load catalogue: ' + e.message + '</td></tr>';",
                "        });"
        );
    }
}
