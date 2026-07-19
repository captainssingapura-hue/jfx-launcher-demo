package com.example.fxsuite.web;

import hue.captains.singapura.js.homing.studio.base.ClasspathMarkdownDoc;
import hue.captains.singapura.js.homing.studio.base.Reference;

import java.util.List;
import java.util.UUID;

/**
 * The launcher dashboard's home page. A markdown-body doc (its body lives at
 * {@code src/main/resources/docs/com/example/fxsuite/web/LauncherHomeDoc.md})
 * whose links use the {@code fxsuite://} scheme so a click hands the URL to the
 * registered OS protocol handler.
 */
public record LauncherHomeDoc() implements ClasspathMarkdownDoc {
    private static final UUID ID = UUID.fromString("f8a1c0de-0001-4001-8000-000000000001");
    public static final LauncherHomeDoc INSTANCE = new LauncherHomeDoc();

    @Override public UUID   uuid()    { return ID; }
    @Override public String title()   { return "FxSuite — Desktop App Launcher"; }
    @Override public String summary() {
        return "Click an app to open it as a native JavaFX desktop window via the "
             + "fxsuite:// protocol handler.";
    }
    @Override public String category(){ return "GUIDE"; }
    @Override public List<Reference> references() { return List.of(); }
}
