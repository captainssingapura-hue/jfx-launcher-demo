package com.example.fxsuite.launcher.setup;

/**
 * One concrete change to the Windows registry, in the exact terms the OS sees it.
 *
 * <p>Plans are built from these so a user can be shown precisely what will happen
 * <i>before</i> anything is written — and so the reverse plan can be shown too.</p>
 *
 * @param kind       what will happen to this value
 * @param key        full key path, e.g. {@code HKCU\Software\Classes\fxsuite-prod}
 * @param valueName  {@code (Default)} for a key's default value, else the value name
 * @param data       the data to write (null for {@link Kind#DELETE_KEY})
 * @param previous   the value currently there, if any — what makes this reversible
 */
public record RegistryOp(Kind kind, String key, String valueName, String data, String previous) {

    public enum Kind {
        /** Create or overwrite a value. */
        SET,
        /** Remove the whole key (and its subkeys). */
        DELETE_KEY,
        /** Put back a value that existed before we overwrote it. */
        RESTORE
    }

    public static RegistryOp set(String key, String valueName, String data, String previous) {
        return new RegistryOp(Kind.SET, key, valueName, data, previous);
    }

    public static RegistryOp deleteKey(String key, String previous) {
        return new RegistryOp(Kind.DELETE_KEY, key, null, null, previous);
    }

    public static RegistryOp restore(String key, String valueName, String data) {
        return new RegistryOp(Kind.RESTORE, key, valueName, data, null);
    }

    /** A one-line, human-readable rendering for the setup UI. */
    public String describe() {
        return switch (kind) {
            case SET -> (previous == null ? "create  " : "replace ") + key
                    + "  [" + valueName + "] = " + display(data)
                    + (previous == null ? "" : "   (was: " + display(previous) + ")");
            case DELETE_KEY -> "delete  " + key + "   (and its subkeys)";
            case RESTORE -> "restore " + key + "  [" + valueName + "] = " + display(data);
        };
    }

    private static String display(String s) {
        if (s == null) return "<none>";
        return s.isEmpty() ? "\"\"  (empty string)" : s;
    }
}
