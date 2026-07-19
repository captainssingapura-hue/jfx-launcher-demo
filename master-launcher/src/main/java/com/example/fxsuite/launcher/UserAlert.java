package com.example.fxsuite.launcher;

import javax.swing.JOptionPane;

/**
 * Minimal user feedback for the windowless gatekeeper.
 *
 * <p>Since the launcher no longer has JavaFX, rejection messages use a Swing
 * dialog from {@code java.desktop} (part of the JDK — nothing bundled). Shown
 * modally on the calling thread so the JVM stays alive until the user dismisses
 * it; failures to show are swallowed so they can never break the launch path.</p>
 */
public final class UserAlert {

    private UserAlert() {}

    public static void error(String message) {
        try {
            JOptionPane.showMessageDialog(
                    null, message, "FxSuite — launch rejected", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable t) {
            DiagLog.log("could not show rejection dialog: " + t);
        }
    }
}
