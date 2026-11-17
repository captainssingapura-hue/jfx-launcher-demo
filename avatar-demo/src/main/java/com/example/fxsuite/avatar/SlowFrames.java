package com.example.fxsuite.avatar;

import com.example.fxsuite.avatar.look.TerrainModel;
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.MeshView;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * For measuring: each frame that comes later than {@code slow} milliseconds after the one before, a line saying
 * what was done before it, how long into the run. The gap between two frames holds the first one's own work, then
 * JavaFX sending what changed to the graphics card and drawing it, which it does on a thread of its own while the
 * next frame is worked out: so what a frame changes may cost the gap after the next one. A slow gap is put down to
 * the frame before it, its own work
 * (and of that, the drawing's: the land's tiles swapped and their haze, the water, the lines, the buildings),
 * what it did to the land, and the garbage collector's runs since; and what is left over is JavaFX's. What the
 * frame before that did to the land is said too.
 *
 * <p>Turned on and off as it runs ({@link #on}); each line written out ({@link #write}) to the console and to
 * {@link #FILE}, kept from one run to the next, each time it is turned on marked. On from the start with
 * {@code -Davatar.slow=20} (milliseconds, how slow is slow); else off, and {@link #SLOW} once turned on.
 */
final class SlowFrames {

    /** How slow a frame must be to be told of, unless said otherwise; and where the lines are kept. */
    static final double SLOW = 20;
    static final Path FILE = Path.of(System.getProperty("java.io.tmpdir"), "avatar-frames.log");

    private final double slow;
    private boolean on;
    private PrintWriter out;
    private long first = -1;
    private long last = -1;
    private String landBefore = "";
    private long gcCount;
    private long gcMillis;
    /** The frame before: its own work, the drawing's of it, what it did to the land, where the camera was. */
    private double work;
    private double drawing;
    private String land = "";
    private Vec3 at = Vec3.ZERO;
    private double speed;

    SlowFrames(final double slow) {
        this.slow = slow;
        collected();
    }

    /** From {@code -Davatar.slow}: on, so slow, if it is set; else off, {@link #SLOW} once on. */
    static SlowFrames fromProperty() {
        final String ms = System.getProperty("avatar.slow");
        final boolean set = ms != null && !ms.isBlank();
        final SlowFrames frames = new SlowFrames(set ? Double.parseDouble(ms) : SLOW);
        frames.on(set);
        return frames;
    }

    /** Whether it is on. */
    boolean on() {
        return on;
    }

    /**
     * Turned on ({@code now}) or off. On, the file opened to add to, marked; the frames counted afresh, so the
     * gap since it was last on is not taken for a slow one. Off, the file closed.
     */
    void on(final boolean now) {
        if (now == on) {
            return;
        }
        on = now;
        if (now) {
            first = -1;
            last = -1;
            collected();
            try {
                out = new PrintWriter(Files.newBufferedWriter(FILE, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND), true);
            } catch (final IOException e) {
                out = null;
                System.err.println("frames: cannot write " + FILE + ": " + e.getMessage());
            }
            write("--- on at " + LocalDateTime.now().withNano(0) + ", frames slower than " + slow + " ms");
        } else {
            write("--- off at " + LocalDateTime.now().withNano(0));
            if (out != null) {
                out.close();
                out = null;
            }
        }
    }

    /** A line written out: to the console, and to the file while it is on. */
    void write(final String line) {
        System.out.println(line);
        if (out != null) {
            out.println(line);
        }
    }

    /**
     * A frame began at {@code now} (nanoseconds); a line about the frame before, if the gap since it was slow, or
     * null. {@code scene} counts the meshes being drawn, only then.
     */
    String began(final long now, final Node scene) {
        final long gcBefore = gcCount;
        final long gcMsBefore = gcMillis;
        collected();
        String line = null;
        if (last >= 0) {
            final double gap = (now - last) / 1e6;
            if (gap > slow) {
                final long runs = gcCount - gcBefore;
                final long gcMs = gcMillis - gcMsBefore;
                line = String.format(Locale.ROOT, "%7.1f s ", (now - first) / 1e9) + String.format(Locale.ROOT,
                        "slow %.1f ms | work %.1f ms, drawing's %.1f | land %s; before that %s | gc %s | left to JavaFX %.1f ms"
                                + " | meshes %d | at %.0f,%.0f m, %.0f km/h",
                        gap, work, drawing, land.isEmpty() ? "nothing" : land, landBefore.isEmpty() ? "nothing" : landBefore,
                        runs == 0 ? "none" : runs + " run" + (runs > 1 ? "s" : "") + " " + gcMs + " ms",
                        Math.max(0, gap - work - gcMs), meshes(scene), at.x() / 100, at.z() / 100,
                        speed * 0.036);
            }
        }
        if (first < 0) {
            first = now;
        }
        last = now;
        return line;
    }

    /**
     * The frame just done: its own work and the drawing's of it, in nanoseconds; what it did to the land (null for
     * none); where the camera was.
     */
    void done(final long work, final long drawing, final TerrainModel.Work land, final Vec3 at) {
        this.landBefore = this.land;
        this.work = work / 1e6;
        this.drawing = drawing / 1e6;
        this.at = at;
        if (land == null) {
            this.land = "";
            this.speed = 0;
            return;
        }
        this.speed = land.speed;
        final StringBuilder text = new StringBuilder();
        if (land.swaps > 0) {
            text.append(String.format(Locale.ROOT, "%d swap%s %s (%d new), %,d heights", land.swaps,
                    land.swaps > 1 ? "s" : "", land.levels, land.fresh, land.swapped));
        }
        if (land.hazes > 0) {
            text.append(text.isEmpty() ? "" : "; ").append(String.format(Locale.ROOT, "haze %d tile%s, %,d heights",
                    land.hazes, land.hazes > 1 ? "s" : "", land.hazed));
        }
        text.append(text.isEmpty() ? "" : "; ").append(String.format(Locale.ROOT, "%d pieces, %d in view", land.pieces, land.inView));
        this.land = text.toString();
    }

    /** Where the camera was last, and how fast it went. */
    String where() {
        return String.format(Locale.ROOT, "at %.0f,%.0f m, %.0f km/h", at.x() / 100, at.z() / 100, speed * 0.036);
    }

    /** The garbage collectors' runs and time, all told, so far. */
    private void collected() {
        long count = 0;
        long millis = 0;
        for (final GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            count += Math.max(0, gc.getCollectionCount());
            millis += Math.max(0, gc.getCollectionTime());
        }
        gcCount = count;
        gcMillis = millis;
    }

    /** How many meshes under {@code node} are being drawn. */
    private static int meshes(final Node node) {
        if (node == null || !node.isVisible()) {
            return 0;
        }
        if (node instanceof MeshView) {
            return 1;
        }
        int n = 0;
        if (node instanceof Parent parent) {
            for (final Node child : parent.getChildrenUnmodifiable()) {
                n += meshes(child);
            }
        }
        return n;
    }
}
