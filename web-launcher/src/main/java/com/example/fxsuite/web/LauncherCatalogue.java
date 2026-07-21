package com.example.fxsuite.web;

import com.example.fxsuite.web.app.EnvLaunchApp;
import com.example.fxsuite.web.app.PublishedAppsApp;

import hue.captains.singapura.js.homing.studio.base.Doc;
import hue.captains.singapura.js.homing.studio.base.DocProvider;
import hue.captains.singapura.js.homing.studio.base.app.Entry;
import hue.captains.singapura.js.homing.studio.base.app.L0_Catalogue;
import hue.captains.singapura.js.homing.studio.base.app.L1_Catalogue;
import hue.captains.singapura.js.homing.studio.base.app.Navigable;

import java.util.List;

/**
 * L0 root of the launcher studio — the catalogue that organises everything the
 * studio offers: the introduction doc, and the MPAs (each a {@code StandardMPA}
 * hosting one widget) rather than ad-hoc HTML pages.
 *
 * <p>Implements {@link DocProvider} so the intro doc is reachable through the
 * studio's DocRegistry (required for Entry.OfDoc reachability validation at boot).</p>
 */
public record LauncherCatalogue()
        implements L0_Catalogue<LauncherCatalogue>, DocProvider {

    public static final LauncherCatalogue INSTANCE = new LauncherCatalogue();

    @Override public String name()    { return "FxSuite Launcher"; }
    @Override public String summary() {
        return "Launch native JavaFX apps into a chosen environment, and see what is published.";
    }
    @Override public String badge()   { return "STUDIO"; }
    @Override public String icon()    { return "🚀"; }

    @Override public List<Entry<LauncherCatalogue>> leaves() {
        return List.of(
                Entry.of(this, LauncherHomeDoc.INSTANCE),
                Entry.of(this, new Navigable<>(
                        EnvLaunchApp.INSTANCE,
                        new EnvLaunchApp.Params(),
                        "Launch by environment",
                        "Pick Production, UAT or a dev environment — the backend signs a launch "
                      + "token for that environment and the desktop app opens.")),
                Entry.of(this, new Navigable<>(
                        PublishedAppsApp.INSTANCE,
                        new PublishedAppsApp.Params(),
                        "Published versions",
                        "Everything published in the artifact repository, with the per-environment "
                      + "version policy that decides what each environment launches."))
        );
    }

    @Override public List<? extends L1_Catalogue<LauncherCatalogue, ?>> subCatalogues() {
        return List.of();
    }

    @Override public List<Doc> docs() {
        return List.of(LauncherHomeDoc.INSTANCE);
    }
}
