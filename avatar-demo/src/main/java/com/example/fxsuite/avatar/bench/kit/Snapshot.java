package com.example.fxsuite.avatar.bench.kit;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * A bench's window saved as a picture, and the bench closed: for a bench run by a command, to see what it shows without
 * a hand on it. {@code -Dbench.snapshot=out.png} where to save it; {@code -Dbench.settle} how long to wait first, in
 * ms (900 if not said).
 */
public final class Snapshot {

    private Snapshot() {
    }

    /** Saves {@code scene} to {@code -Dbench.snapshot}, if set, once it has settled, and exits. */
    public static void ifAsked(final Scene scene) {
        final String path = System.getProperty("bench.snapshot");
        if (path == null) {
            return;
        }
        final PauseTransition wait = new PauseTransition(Duration.millis(Long.getLong("bench.settle", 900)));
        wait.setOnFinished(e -> {
            try {
                final WritableImage image = scene.snapshot(null);
                final PixelReader pixels = image.getPixelReader();
                final BufferedImage out = new BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < out.getHeight(); y++) {
                    for (int x = 0; x < out.getWidth(); x++) {
                        out.setRGB(x, y, pixels.getArgb(x, y));
                    }
                }
                ImageIO.write(out, "png", new File(path));
                System.out.println("saved " + path);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
        });
        wait.play();
    }
}
