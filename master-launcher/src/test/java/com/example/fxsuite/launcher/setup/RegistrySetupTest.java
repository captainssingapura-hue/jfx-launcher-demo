package com.example.fxsuite.launcher.setup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrySetupTest {

    // Reversibility: re-registering displaces FxSuite's own command, which must not be
    // mistaken for the handler that was there before FxSuite.

    @Test
    void recognisesTheCommandsFxSuiteWrites() {
        assertTrue(RegistrySetup.isOwnCommand(
                "\"C:\\Java\\bin\\javaw.exe\" -jar \"Q:\\dist\\fxsuite\\FxSuite-prod.jar\" \"%1\""));
        assertTrue(RegistrySetup.isOwnCommand(
                "\"C:\\Java\\bin\\javaw.exe\" -jar \"Q:\\dist\\FxSuite-dev.jar\" --env=dev1 \"%1\""));
        assertTrue(RegistrySetup.isOwnCommand(
                "\"C:\\Java\\bin\\javaw.exe\" -jar \"Q:\\dist\\fxsuite\\prod\\master-launcher.jar\" \"%1\""));
    }

    @Test
    void leavesAForeignHandlerRestorable() {
        assertFalse(RegistrySetup.isOwnCommand("\"C:\\Tools\\other.exe\" \"%1\""));
        assertFalse(RegistrySetup.isOwnCommand("\"C:\\Java\\bin\\javaw.exe\" -jar \"C:\\Tools\\other.jar\" \"%1\""));
    }

    @Test
    void aFolderNamedLikeUsDoesNotMakeAHandlerOurs() {
        assertFalse(RegistrySetup.isOwnCommand(
                "\"C:\\Java\\bin\\javaw.exe\" -jar \"C:\\FxSuite-tools\\other.jar\" \"%1\""));
    }

    // A handler whose Java has gone fails silently in the browser, so it has to be detectable.

    @Test
    void noticesAHandlerWhoseJavaHasGone(@TempDir Path dir) throws Exception {
        Path javaw = Files.createFile(dir.resolve("javaw.exe"));
        assertFalse(RegistrySetup.javaMissing("\"" + javaw + "\" -jar \"x.jar\" \"%1\""));
        assertTrue(RegistrySetup.javaMissing("\"" + dir.resolve("gone.exe") + "\" -jar \"x.jar\" \"%1\""));
    }

    @Test
    void doesNotCallAnUnparseableCommandBroken() {
        assertFalse(RegistrySetup.javaMissing(null));
        assertFalse(RegistrySetup.javaMissing("javaw.exe -jar x.jar \"%1\""));
    }
}
