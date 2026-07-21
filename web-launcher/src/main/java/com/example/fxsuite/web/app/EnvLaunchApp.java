package com.example.fxsuite.web.app;

import hue.captains.singapura.js.homing.core.AppLink;
import hue.captains.singapura.js.homing.core.AppModule;
import hue.captains.singapura.js.homing.core.Widget;
import hue.captains.singapura.js.homing.studio.base.widget.SingleWidgetMPA;

/**
 * MPA shell hosting {@link EnvLaunchWidget} — the page from which a user launches
 * a desktop app into a chosen environment.
 *
 * <p>URL: {@code /app?app=env-launch}. {@link SingleWidgetMPA} is the one-widget
 * specialisation of {@code StandardMPA}, which is the right shape here: the page
 * is a single widget rather than a widget-switching shell.</p>
 *
 * <p>Replaces the former hand-written {@code site/authorized.html}, so the page now
 * lives inside the studio chrome (breadcrumbs, themes) and is reachable from the
 * catalogue rather than from a separate ad-hoc HTTP server.</p>
 */
public final class EnvLaunchApp extends SingleWidgetMPA<EnvLaunchApp.Params, EnvLaunchApp> {

    public static final EnvLaunchApp INSTANCE = new EnvLaunchApp();
    private EnvLaunchApp() {}

    public record Params() implements AppModule._Param {}
    public record appMain() implements AppModule._AppMain<Params, EnvLaunchApp> {}
    public record link() implements AppLink<EnvLaunchApp> {}

    @Override public String simpleName() { return "env-launch"; }
    @Override public Class<Params> paramsType() { return Params.class; }
    @Override public String title() { return "Launch by environment"; }

    @Override
    protected AppModule._AppMain<Params, EnvLaunchApp> appMain() {
        return new appMain();
    }

    @Override
    protected Widget<?, ?> widget() {
        return EnvLaunchWidget.INSTANCE;
    }
}
