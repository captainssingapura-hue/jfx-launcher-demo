package com.example.fxsuite.launcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Entry point of every app the launcher spawns: sets the process's {@link AppIdentity}, then
 * hands over to the app's real {@code main}.
 *
 * <p>It lives in the environment jar, which is on every spawned classpath, so an app gets its
 * taskbar identity with no code of its own. That includes apps fetched from the repository,
 * and a stage that sets its own icon keeps it.</p>
 *
 * <p>Args: {@code <main class> <app args…>}. The ID comes from {@code -Dfxsuite.appId}.</p>
 */
public final class AppBootstrap {

    private AppBootstrap() {}

    public static void main(String[] args) throws Throwable {
        String appId = System.getProperty("fxsuite.appId");
        // The spawner folds this process's stdout into the environment's log.
        if (appId != null) System.out.println(AppIdentity.apply(appId));

        Method main = Class.forName(args[0]).getMethod("main", String[].class);
        try {
            main.invoke(null, (Object) Arrays.copyOfRange(args, 1, args.length));
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
