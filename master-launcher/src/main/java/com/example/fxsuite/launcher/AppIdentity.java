package com.example.fxsuite.launcher;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Gives this process its own identity on the Windows taskbar, instead of appearing as Java.
 *
 * <p>Every JavaFX app here is a {@code javaw.exe} process, so left alone Windows shows the Java
 * cup and treats them all as one application. Two things change that, neither of which needs a
 * native wrapper or any change to the app itself:</p>
 * <ul>
 *   <li>an <b>app ID</b>, which tells Windows which windows belong together, so Production
 *       and UAT are separate applications on the taskbar;</li>
 *   <li>the <b>environment's icon</b>, applied to every stage that has not chosen its own.</li>
 * </ul>
 *
 * <p>Must run before the process opens a window: Windows requires the app ID to be set first.
 * Neither step is allowed to stop an app starting, so a failure is reported, not thrown.</p>
 */
public final class AppIdentity {

    /** One per environment jar. JavaFX uses the 32px icon on Windows and ignores generated ones. */
    private static final String[] ICONS = {"/fxsuite/icon-32.png", "/fxsuite/icon-256.png"};

    private AppIdentity() {}

    /** The launcher window of an environment, e.g. {@code FxSuite.Launcher.prod}. */
    public static String launcherId(String env) {
        return "FxSuite.Launcher." + env;
    }

    /** An app in an environment, e.g. {@code FxSuite.App.prod.hello}. */
    public static String appId(String env, String app) {
        return "FxSuite.App." + env + "." + app;
    }

    /** @return a line describing the outcome, for the caller's log */
    public static String apply(String appId) {
        String outcome = setAppUserModelId(appId);
        try {
            useEnvironmentIcon();
        } catch (Throwable t) {
            outcome += "; icon not applied: " + t;
        }
        return outcome;
    }

    private static String setAppUserModelId(String appId) {
        if (!System.getProperty("os.name", "").startsWith("Windows")) return "app id: not Windows";
        try (Arena arena = Arena.ofConfined()) {
            // The library stays loaded: it holds the ID for the life of the process.
            SymbolLookup shell32 = SymbolLookup.libraryLookup("shell32", Arena.global());
            MethodHandle set = Linker.nativeLinker().downcallHandle(
                    shell32.findOrThrow("SetCurrentProcessExplicitAppUserModelID"),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            int hresult = (int) set.invokeExact(arena.allocateFrom(appId, StandardCharsets.UTF_16LE));
            return hresult == 0 ? "app id: " + appId
                                : "app id not set: HRESULT 0x" + Integer.toHexString(hresult);
        } catch (Throwable t) {
            return "app id not set: " + t;
        }
    }

    /** Covers every stage the process ever opens, and leaves alone one with its own icon. */
    private static void useEnvironmentIcon() {
        if (AppIdentity.class.getResource(ICONS[0]) == null) return;   // the bare core has none
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) {
                    if (window instanceof Stage stage && stage.getIcons().isEmpty()) {
                        stage.getIcons().addAll(icons());
                    }
                }
            }
        });
    }

    private static List<Image> icons() {
        List<Image> images = new ArrayList<>();
        for (String resource : ICONS) {
            URL url = AppIdentity.class.getResource(resource);
            if (url != null) images.add(new Image(url.toExternalForm()));
        }
        return images;
    }
}
