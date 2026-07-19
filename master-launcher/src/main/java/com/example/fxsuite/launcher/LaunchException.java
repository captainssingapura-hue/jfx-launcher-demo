package com.example.fxsuite.launcher;

/** A user-facing reason that a launch URL was rejected. Message is safe to show. */
public class LaunchException extends Exception {
    public LaunchException(String message) {
        super(message);
    }
}
