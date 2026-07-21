package com.example.fxsuite.web;

import com.example.fxsuite.web.app.EnvLaunchApp;
import com.example.fxsuite.web.app.PublishedAppsApp;

import hue.captains.singapura.js.homing.core.AppModule;
import hue.captains.singapura.js.homing.studio.base.Studio;
import hue.captains.singapura.js.homing.studio.base.app.StudioBrand;

import java.util.List;

/**
 * The launcher studio. Home is the {@link LauncherCatalogue}; the studio's pages are
 * proper MPAs (registered here so the framework serves their JS) rather than
 * hand-written HTML.
 */
public record LauncherStudio() implements Studio<LauncherCatalogue> {

    public static final LauncherStudio INSTANCE = new LauncherStudio();

    @Override public LauncherCatalogue home() { return LauncherCatalogue.INSTANCE; }

    @Override public List<AppModule<?, ?>> apps() {
        return List.of(EnvLaunchApp.INSTANCE, PublishedAppsApp.INSTANCE);
    }

    @Override public StudioBrand standaloneBrand() {
        return new StudioBrand("FxSuite · Launcher", LauncherCatalogue.class);
    }
}
