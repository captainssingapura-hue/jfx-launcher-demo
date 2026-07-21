package com.example.fxsuite.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LaunchUriTest {

    @Test
    void parsesWellFormedLaunchUrl() throws Exception {
        LaunchUri u = LaunchUri.parse("fxsuite-prod://launch/hello");
        assertEquals("prod", u.env());
        assertEquals("launch", u.action());
        assertEquals("hello", u.appId());
    }

    @Test
    void carriesTheEnvironmentFromTheScheme() throws Exception {
        assertEquals("uat", LaunchUri.parse("fxsuite-uat://launch/hello").env());
        assertEquals("dev3", LaunchUri.parse("fxsuite-dev3://launch/hello").env());
    }

    @Test
    void extractsTokenFromQuery() throws Exception {
        LaunchUri u = LaunchUri.parse("fxsuite-prod://launch/hello?tok=abc.def.ghi");
        assertEquals("hello", u.appId());
        assertEquals("abc.def.ghi", u.token());
    }

    @Test
    void tokenIsNullWhenAbsent() throws Exception {
        assertNull(LaunchUri.parse("fxsuite-prod://launch/hello").token());
        assertNull(LaunchUri.parse("fxsuite-prod://launch/hello?other=x").token());
    }

    @Test
    void rejectsBareOrUnknownScheme() {
        assertThrows(LaunchException.class, () -> LaunchUri.parse("https://launch/hello"));
        assertThrows(LaunchException.class, () -> LaunchUri.parse("fxsuite://launch/hello"));
        assertThrows(LaunchException.class, () -> LaunchUri.parse("fxsuite-://launch/hello"));
    }

    @Test
    void rejectsWrongAction() {
        assertThrows(LaunchException.class, () -> LaunchUri.parse("fxsuite-prod://delete/hello"));
    }

    @Test
    void rejectsPathTraversal() {
        assertThrows(LaunchException.class, () -> LaunchUri.parse("fxsuite-prod://launch/../etc"));
        assertThrows(LaunchException.class, () -> LaunchUri.parse("fxsuite-prod://launch/a/b"));
    }

    @Test
    void rejectsIllegalCharacters() {
        assertThrows(LaunchException.class, () -> LaunchUri.parse("fxsuite-prod://launch/He%20llo"));
        assertThrows(LaunchException.class, () -> LaunchUri.parse("fxsuite-prod://launch/UPPER"));
    }

    @Test
    void rejectsEmptyAndNull() {
        assertThrows(LaunchException.class, () -> LaunchUri.parse(""));
        assertThrows(LaunchException.class, () -> LaunchUri.parse(null));
        assertThrows(LaunchException.class, () -> LaunchUri.parse("fxsuite-prod://launch/"));
    }
}
