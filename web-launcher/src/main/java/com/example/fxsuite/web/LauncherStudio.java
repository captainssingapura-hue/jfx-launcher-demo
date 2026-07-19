package com.example.fxsuite.web;

import hue.captains.singapura.js.homing.core.AppModule;
import hue.captains.singapura.js.homing.studio.base.Studio;
import hue.captains.singapura.js.homing.studio.base.app.StudioBrand;

import java.util.List;

/**
 * The launcher studio. Home is the {@link LauncherCatalogue}; no embedded
 * AppModules (the dashboard is pure markdown with protocol links).
 */
public record LauncherStudio() implements Studio<LauncherCatalogue> {

    public static final LauncherStudio INSTANCE = new LauncherStudio();

    @Override public LauncherCatalogue home() { return LauncherCatalogue.INSTANCE; }

    @Override public List<AppModule<?, ?>> apps() { return List.of(); }

    @Override public StudioBrand standaloneBrand() {
        return new StudioBrand("FxSuite · Launcher", LauncherCatalogue.class);
    }
}
