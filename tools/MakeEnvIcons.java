import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Generates the per-environment window and taskbar icons.
 *
 * <p>Each icon is the environment's colour with its initial, so environments stay
 * distinguishable by shape as well as by colour. The colours match each module's
 * {@code EnvSpec.accentColour()}.</p>
 *
 * <p>The results are committed as ordinary PNG resources: JavaFX only passes an icon to Windows
 * when it was loaded from an image file, and silently ignores one generated in memory. So this
 * runs once, when the artwork changes, and never at runtime.</p>
 *
 * <p>Run from the repo root, with no build needed (JDK single-file launch):</p>
 * <pre>{@code
 *   java tools/MakeEnvIcons.java
 * }</pre>
 */
public final class MakeEnvIcons {

    /** JavaFX uses the 32px icon on Windows; the 256px one is what macOS and Linux would pick. */
    private static final int[] SIZES = {32, 256};

    public static void main(String[] args) throws Exception {
        write("prod", "#c62828", "P");
        write("uat", "#ef6c00", "U");
        write("dev", "#1565c0", "D");
    }

    private static void write(String env, String colour, String letter) throws Exception {
        Path dir = Path.of("env", env, "src", "main", "resources", "fxsuite");
        Files.createDirectories(dir);
        for (int size : SIZES) {
            File out = dir.resolve("icon-" + size + ".png").toFile();
            ImageIO.write(render(size, Color.decode(colour), letter), "png", out);
            System.out.println("wrote " + out);
        }
    }

    private static BufferedImage render(int n, Color colour, String letter) {
        BufferedImage image = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int arc = Math.round(n * 0.28f);
        g.setColor(colour);
        g.fillRoundRect(0, 0, n, n, arc, arc);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, Math.round(n * 0.72f)));
        FontMetrics m = g.getFontMetrics();
        int x = (n - m.stringWidth(letter)) / 2;
        int y = (n - m.getHeight()) / 2 + m.getAscent();
        g.drawString(letter, x, y);
        g.dispose();
        return image;
    }
}
