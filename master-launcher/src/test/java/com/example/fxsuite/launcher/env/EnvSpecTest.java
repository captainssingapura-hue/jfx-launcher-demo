package com.example.fxsuite.launcher.env;

import com.example.fxsuite.launcher.LaunchException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules that used to be implicit in whether {@code launcher-env.properties} carried an
 * {@code env=} line, now stated as types and pinned here.
 */
class EnvSpecTest {

    private static final class Prod extends SingletonEnv {
        Prod() { super("prod", "Production"); }
        @Override public String repoBase() { return "http://repo/prod"; }
        @Override public List<BundledApp> bundledApps() { return List.of(); }
    }

    private static final class Dev extends MultiplexedEnv {
        Dev() { super("dev", "Development", Pattern.compile("dev[1-9][0-9]?"), "dev1..dev99"); }
        @Override public String repoBase() { return "http://repo/dev"; }
        @Override public List<BundledApp> bundledApps() { return List.of(); }
    }

    // --- singleton -------------------------------------------------------

    @Test
    void singletonKnowsItsEnvironmentWithoutAnArgument() throws Exception {
        assertEquals("prod", new Prod().resolve(null));
        assertEquals("prod", new Prod().resolve("  "));
    }

    @Test
    void singletonAcceptsAnArgumentNamingItself() throws Exception {
        assertEquals("prod", new Prod().resolve(" prod "));
    }

    @Test
    void singletonRefusesToServeAnotherEnvironment() {
        LaunchException e = assertThrows(LaunchException.class, () -> new Prod().resolve("dev1"));
        assertTrue(e.getMessage().contains("dev1"), e.getMessage());
    }

    @Test
    void singletonNeedsNoEnvArgumentWhenRegistered() {
        assertFalse(new Prod().requiresEnvArgument());
        assertFalse(new Prod().multiplexed());
    }

    // --- multiplexed -----------------------------------------------------

    @Test
    void multiplexedRefusesToGuessWhichEnvironmentItIs() {
        assertThrows(LaunchException.class, () -> new Dev().resolve(null));
        assertThrows(LaunchException.class, () -> new Dev().resolve(""));
    }

    @Test
    void multiplexedServesAnyMemberOfItsFamily() throws Exception {
        assertEquals("dev1", new Dev().resolve("dev1"));
        assertEquals("dev42", new Dev().resolve(" dev42 "));
    }

    @Test
    void multiplexedRefusesAnythingOutsideItsFamily() {
        // the mis-registration that matters: the shared dev build claiming to be Production
        assertThrows(LaunchException.class, () -> new Dev().resolve("prod"));
        assertThrows(LaunchException.class, () -> new Dev().resolve("dev0"));
        assertThrows(LaunchException.class, () -> new Dev().resolve("devil"));
        assertFalse(new Dev().accepts("prod"));
    }

    @Test
    void multiplexedMustCarryAnEnvArgumentWhenRegistered() {
        assertTrue(new Dev().requiresEnvArgument());
        assertTrue(new Dev().multiplexed());
    }

    // --- bundled apps ----------------------------------------------------

    @Test
    void bundledAppNamesItsEntryPointFromTheClassItself() {
        BundledApp a = BundledApp.of("hello", EnvSpecTest.class, "1.0.0");
        assertEquals(EnvSpecTest.class.getName(), a.mainClassName());
        assertEquals("hello", a.appId());
        assertEquals("1.0.0", a.version());
    }
}
