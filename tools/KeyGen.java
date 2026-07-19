import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Generates the FxSuite launch-token signing keypair — run once during setup so
 * the private key never lives in source control.
 *
 * <p>Writes a matched RSA-2048 pair as base64(DER):</p>
 * <ul>
 *   <li>the <b>private</b> key (PKCS#8) → web-launcher (the token issuer signs with it);</li>
 *   <li>the <b>public</b> key (X.509)  → master-launcher (the launcher verifies with it,
 *       baked into its jar as part of the trusted launcher).</li>
 * </ul>
 *
 * <p>Run from the repo root, with no build needed (JDK single-file launch):</p>
 * <pre>{@code
 *   java tools/KeyGen.java
 *   # or override the two output paths:
 *   java tools/KeyGen.java <private-out> <public-out>
 * }</pre>
 *
 * <p>Then build the modules — the keys are picked up as classpath resources.
 * Both files are git-ignored; re-run this any time to rotate the pair.</p>
 */
public class KeyGen {

    private static final String DEFAULT_PRIVATE =
            "web-launcher/src/main/resources/fxsuite/launch-signing-key.pk8.b64";
    private static final String DEFAULT_PUBLIC =
            "master-launcher/src/main/resources/fxsuite/launch-verify-key.x509.b64";

    public static void main(String[] args) throws Exception {
        Path privateOut = Path.of(args.length > 0 ? args[0] : DEFAULT_PRIVATE);
        Path publicOut  = Path.of(args.length > 1 ? args[1] : DEFAULT_PUBLIC);

        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();

        write(privateOut, kp.getPrivate().getEncoded());   // PKCS#8
        write(publicOut,  kp.getPublic().getEncoded());    // X.509

        System.out.println("Generated RSA-2048 launch-token keypair:");
        System.out.println("  private (issuer)   -> " + privateOut);
        System.out.println("  public  (verifier) -> " + publicOut);
        System.out.println("Now (re)build master-launcher and web-launcher so they pick up the new keys.");
    }

    private static void write(Path out, byte[] der) throws Exception {
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Files.writeString(out, Base64.getEncoder().encodeToString(der));
    }
}
