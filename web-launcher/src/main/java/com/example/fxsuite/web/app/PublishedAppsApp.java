package com.example.fxsuite.web.app;

import hue.captains.singapura.js.homing.core.AppLink;
import hue.captains.singapura.js.homing.core.AppModule;
import hue.captains.singapura.js.homing.core.Widget;
import hue.captains.singapura.js.homing.studio.base.widget.SingleWidgetMPA;

/**
 * MPA shell hosting {@link PublishedAppsWidget} — what is published in the artifact
 * repository, and which version each environment resolves to.
 *
 * <p>URL: {@code /app?app=published-apps}.</p>
 */
public final class PublishedAppsApp extends SingleWidgetMPA<PublishedAppsApp.Params, PublishedAppsApp> {

    public static final PublishedAppsApp INSTANCE = new PublishedAppsApp();
    private PublishedAppsApp() {}

    public record Params() implements AppModule._Param {}
    public record appMain() implements AppModule._AppMain<Params, PublishedAppsApp> {}
    public record link() implements AppLink<PublishedAppsApp> {}

    @Override public String simpleName() { return "published-apps"; }
    @Override public Class<Params> paramsType() { return Params.class; }
    @Override public String title() { return "Published versions"; }

    @Override
    protected AppModule._AppMain<Params, PublishedAppsApp> appMain() {
        return new appMain();
    }

    @Override
    protected Widget<?, ?> widget() {
        return PublishedAppsWidget.INSTANCE;
    }
}
