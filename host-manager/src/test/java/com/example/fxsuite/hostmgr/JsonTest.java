package com.example.fxsuite.hostmgr;

import com.example.fxsuite.hostmgr.json.Json;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The manifest parser is load-bearing — it reads what the agent has cryptographically verified. */
class JsonTest {

    @Test
    void parsesNestedManifestShape() {
        String json = """
            {"generation":7,"host":"*","apps":[
              {"appId":"svc","version":"1.2.0","sha256":"abc","env":"prod","enabled":true},
              {"appId":"svc","version":"0.9.0","sha256":"def","env":"uat","enabled":false}
            ]}""";
        Map<String, Object> o = Json.object(Json.parse(json));
        assertEquals(7L, Json.lng(o, "generation", 0));
        assertEquals("*", Json.str(o, "host"));
        List<Object> apps = Json.array(o.get("apps"));
        assertEquals(2, apps.size());
        Map<String, Object> first = Json.object(apps.get(0));
        assertEquals("svc", Json.str(first, "appId"));
        assertTrue(Json.bool(first, "enabled", false));
        assertFalse(Json.bool(Json.object(apps.get(1)), "enabled", true));
    }

    @Test
    void handlesEscapesAndEmptyContainers() {
        Map<String, Object> o = Json.object(Json.parse("{\"a\":\"line\\nbreak\",\"b\":[],\"c\":{}}"));
        assertEquals("line\nbreak", Json.str(o, "a"));
        assertTrue(Json.array(o.get("b")).isEmpty());
        assertTrue(Json.object(o.get("c")).isEmpty());
    }

    @Test
    void nullAndNumberForms() {
        Map<String, Object> o = Json.object(Json.parse("{\"n\":null,\"i\":42,\"f\":-3.5e2}"));
        assertNull(o.get("n"));
        assertEquals(42L, Json.lng(o, "i", 0));
        assertEquals(-350L, Json.lng(o, "f", 0));
    }

    @Test
    void rejectsTrailingGarbageAndTruncation() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":1} trailing"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":\"unterminated"));
    }

    @Test
    void quoteRoundTripsThroughParse() {
        String weird = "a\"b\\c\nd";
        String doc = "{\"k\":" + Json.quote(weird) + "}";
        assertEquals(weird, Json.str(Json.object(Json.parse(doc)), "k"));
    }
}
