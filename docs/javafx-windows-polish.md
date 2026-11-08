# Making a JavaFX app look native on Windows

**In short:** a JavaFX app can look like a proper Windows application with no native wrapper,
no installer and no admin rights. Three small changes are enough:

| Default JavaFX app | Change | Result |
|---|---|---|
| Plain white title bar, separate from the app | Extended title bar (§2) | Your colours and menus in the title bar, with real Windows buttons |
| Java cup in the title bar and taskbar | PNG icon files (§3) | Your icon |
| Grouped on the taskbar as "Java" | App ID (§4) | Its own taskbar entry |

We tried this on the FxSuite launcher, where each environment (Prod, UAT, dev) gets its own
colour, icon and taskbar entry. Nothing below depends on FxSuite.

**You need:** JavaFX 25 for the title bar, and Java 22 or later for the app ID. Everything here
was tested on Windows 11 with Oracle JDK 25.

---

## 1. Start in the right order

Two of these settings only work if they are made early, so use a plain `Main` class and do them
before JavaFX starts:

```java
public final class Main {            // a plain class, not an Application subclass
    public static void main(String[] args) {
        System.setProperty("javafx.enablePreview", "true");
        System.setProperty("javafx.suppressPreviewWarning", "true");
        WindowsIdentity.setAppId("Acme.Editor");      // §4
        Application.launch(EditorApp.class, args);
    }
}
```

- **Preview flag:** JavaFX reads it once, so it must be set before the first preview feature is
  touched. Setting it in `main()` means the jar needs no command-line flags, so it should also
  work when double-clicked.
- **App ID:** Windows requires it before the process opens its first window.

---

## 2. Title bar

JavaFX 25 can extend your content into the title bar while Windows keeps handling the caption
buttons, dragging and resizing. Unlike `StageStyle.UNDECORATED`, you don't rebuild any of that
yourself.

Build the scene like this, before the stage is shown:

```java
stage.initStyle(StageStyle.EXTENDED);

HeaderBar header = new HeaderBar();
header.setLeading(menuBar);                      // clickable
header.setCenter(title);
HeaderBar.setDragType(title, HeaderDragType.DRAGGABLE_SUBTREE);   // drags the window
HeaderBar.setPrefButtonHeight(stage, 40);
header.setPrefHeight(40);
header.setMinHeight(40);
header.setStyle("-fx-background-color: " + accent + ";");

VBox root = new VBox(header, content);
VBox.setVgrow(content, Priority.ALWAYS);

Scene scene = new Scene(root, 900, 600);
scene.setFill(Color.web(accent).deriveColor(0, 1, 0.35, 1));      // see below
stage.setScene(scene);
```

**Dragging:** the empty background of the bar drags the window. Anything you put in the bar is
clickable unless you mark it draggable, so title text needs `DRAGGABLE_SUBTREE`.

**Button colour:** the minimise, maximise and close symbols take their colour from the
brightness of the **scene fill**, not the bar. Below 0.5 they turn white, which is why the code
sets the fill to a deep shade of the accent. The root covers the fill, so it never shows. Without
this, a mid-tone bar such as orange counts as light and gets grey symbols. This comes from reading
JavaFX 25's internals, so re-check it when you upgrade.

**Menus in the bar** need a transparent background and light text:

```css
.title-bar { -fx-font-family: "Segoe UI"; }
.title-bar .menu-bar { -fx-background-color: transparent; -fx-padding: 0; }
.title-bar .menu-bar > .container > .menu-button { -fx-background-color: transparent; }
.title-bar .menu-bar > .container > .menu-button > .label { -fx-text-fill: white; }
.title-bar .menu-bar > .container > .menu-button:hover,
.title-bar .menu-bar > .container > .menu-button:showing {
    -fx-background-color: rgba(255, 255, 255, 0.18);
}
```

**Plan for the preview API:** `EXTENDED` and `HeaderBar` are preview features in JavaFX 25, so
they may change. Keep them in one class, and only use them when they are available. Otherwise,
show the same content as a coloured strip under the normal title bar:

```java
boolean extended = Boolean.getBoolean("javafx.enablePreview")
        && Platform.isSupported(ConditionalFeature.EXTENDED_WINDOW);
```

---

## 3. Icon

```java
stage.getIcons().add(new Image(Main.class.getResource("/icon-32.png").toExternalForm()));
```

- **Load the icon from an image file.** An `Image` generated in memory (`WritableImage`, a
  `snapshot()`) fails silently: there is no error and `getIcons()` lists it, but Windows still
  shows the Java cup.
- **Include a 32×32 PNG, and design for that size.** JavaFX passes Windows a single 32×32 icon,
  so bold shapes work better than fine detail. A 256×256 PNG is only useful on other platforms.

**To cover every window**, including dialogs and windows in code you don't own, add the icon as
windows appear. This also leaves alone any stage that sets its own:

```java
Window.getWindows().addListener((ListChangeListener<Window>) change -> {
    while (change.next()) {
        for (Window window : change.getAddedSubList()) {
            if (window instanceof Stage stage && stage.getIcons().isEmpty()) {
                stage.getIcons().add(icon());
            }
        }
    }
});
```

---

## 4. Taskbar identity

Every JavaFX app runs as `javaw.exe`, so Windows has no way to tell them apart. An
AppUserModelID tells it which windows belong together, and one Win32 call sets it. Java's Foreign
Function API can make that call without any native code of your own:

```java
public final class WindowsIdentity {
    /** Call before the first window opens. Never stops the app from starting. */
    public static void setAppId(String appId) {
        if (!System.getProperty("os.name", "").startsWith("Windows")) return;
        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup shell32 = SymbolLookup.libraryLookup("shell32", Arena.global());
            MethodHandle set = Linker.nativeLinker().downcallHandle(
                    shell32.findOrThrow("SetCurrentProcessExplicitAppUserModelID"),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            int hresult = (int) set.invokeExact(arena.allocateFrom(appId, StandardCharsets.UTF_16LE));
            if (hresult != 0) System.err.println("App ID not set: 0x" + Integer.toHexString(hresult));
        } catch (Throwable t) {
            System.err.println("App ID not set: " + t);
        }
    }
}
```

Windows with the same ID share a taskbar entry, and different IDs stay apart. Use one ID per
logical application, such as `Acme.Editor`, or `Acme.Editor.Prod` and `Acme.Editor.Uat` if those
must never stack together.

To allow the native call without a warning, add this to the jar manifest:

```
Enable-Native-Access: ALL-UNNAMED
```

The manifest entry only applies when starting with `java -jar`. With `-cp`, pass
`--enable-native-access=ALL-UNNAMED` instead.

**For apps you can't edit**, start them through a small bootstrap class that sets the identity
and then calls the real `main`. FxSuite does this in `AppBootstrap`, so apps get their icon and
taskbar entry with no changes.

---

## 5. Why not a C# wrapper?

A wrapper only helps if it hosts the JVM inside its own process. If it just starts `javaw.exe`,
the window still belongs to `javaw.exe`, so the wrapper's icon and identity don't apply. It would
also be an unsigned `.exe`, which locked-down company machines may block. The Java-only approach
avoids both problems.

---

## 6. What was tested

| Result | Status |
|---|---|
| Coloured title bar with menus in a running window | Confirmed |
| Custom icons on the taskbar for `EXTENDED` windows | Confirmed |
| Separate taskbar entries: 5 windows became 4 buttons, with the 2 Production launchers combined | Confirmed |
| Title bar appears when the jar is started with `javaw -jar` and no flags | Confirmed (not an actual double-click) |
| Dragging, and double-clicking the title bar to maximise | **Not tested with a mouse** |
| Windows 11 snap layouts flyout on the maximise button | **Not tested** |
| Pinning to the taskbar | **Not solved:** a pinned window also needs a shortcut carrying the same app ID |
| Windows 10, other JDKs | **Not tested** |

Test windows with no app ID did not combine on the taskbar, and we didn't find out why. Set an ID
explicitly rather than relying on the default grouping.

**Testing tip:** caption buttons aren't in the scene graph, so `Scene.snapshot()` won't show them.
Check them on a real screen, with the workstation unlocked, because a screen capture of a locked
machine only shows the lock screen.
