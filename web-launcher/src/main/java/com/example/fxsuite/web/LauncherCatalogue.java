package com.example.fxsuite.web;

import hue.captains.singapura.js.homing.studio.base.Doc;
import hue.captains.singapura.js.homing.studio.base.DocProvider;
import hue.captains.singapura.js.homing.studio.base.app.Entry;
import hue.captains.singapura.js.homing.studio.base.app.L0_Catalogue;
import hue.captains.singapura.js.homing.studio.base.app.L1_Catalogue;

import java.util.List;

/**
 * L0 root of the launcher studio. A deliberately tiny catalogue: one leaf, the
 * {@link LauncherHomeDoc} dashboard. Implements {@link DocProvider} so that doc
 * is reachable through the studio's DocRegistry (required for Entry.OfDoc
 * reachability validation at boot).
 */
public record LauncherCatalogue()
        implements L0_Catalogue<LauncherCatalogue>, DocProvider {

    public static final LauncherCatalogue INSTANCE = new LauncherCatalogue();

    @Override public String name()    { return "FxSuite Launcher"; }
    @Override public String summary() {
        return "Web dashboard of native JavaFX apps, launched over the fxsuite:// protocol.";
    }
    @Override public String badge()   { return "STUDIO"; }
    @Override public String icon()    { return "🚀"; }

    @Override public List<Entry<LauncherCatalogue>> leaves() {
        return List.of(Entry.of(this, LauncherHomeDoc.INSTANCE));
    }

    @Override public List<? extends L1_Catalogue<LauncherCatalogue, ?>> subCatalogues() {
        return List.of();
    }

    @Override public List<Doc> docs() {
        return List.of(LauncherHomeDoc.INSTANCE);
    }
}
