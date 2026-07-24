package com.example.fxsuite.hostmgr.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader/writer — just enough for signed desired-state manifests and status
 * reports, with no external dependency.
 *
 * <p>The host agent is meant to be a single jar dropped on many hosts, so it carries no
 * library. The manifest surface is small (objects, arrays, strings, numbers, booleans,
 * null), which is exactly what this parses. Anything the agent verifies cryptographically is
 * checked <i>before</i> it reaches this parser, so the parser is never a trust boundary.</p>
 */
public final class Json {

    private Json() {}

    // ── parsing ──────────────────────────────────────────────────────────

    /** Parse into Map/List/String/Double/Boolean/null. */
    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWs();
        Object v = p.value();
        p.skipWs();
        if (!p.atEnd()) throw new IllegalArgumentException("trailing content at " + p.pos);
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object o) {
        if (!(o instanceof Map)) throw new IllegalArgumentException("expected object, got " + kind(o));
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Object o) {
        if (!(o instanceof List)) throw new IllegalArgumentException("expected array, got " + kind(o));
        return (List<Object>) o;
    }

    public static String str(Map<String, Object> o, String key) {
        Object v = o.get(key);
        return v == null ? null : v.toString();
    }

    public static long lng(Map<String, Object> o, String key, long dflt) {
        Object v = o.get(key);
        return v instanceof Number n ? n.longValue() : dflt;
    }

    public static boolean bool(Map<String, Object> o, String key, boolean dflt) {
        Object v = o.get(key);
        return v instanceof Boolean b ? b : dflt;
    }

    private static String kind(Object o) {
        return o == null ? "null" : o.getClass().getSimpleName();
    }

    private static final class Parser {
        final String s;
        int pos;

        Parser(String s) { this.s = s; }

        boolean atEnd() { return pos >= s.length(); }

        void skipWs() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }

        Object value() {
            skipWs();
            if (atEnd()) throw err("unexpected end");
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't', 'f' -> bool();
                case 'n' -> nul();
                default -> number();
            };
        }

        Map<String, Object> object() {
            expect('{');
            Map<String, Object> m = new LinkedHashMap<>();
            skipWs();
            if (peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                if (peek() != '"') throw err("expected key string");
                String key = string();
                skipWs();
                expect(':');
                m.put(key, value());
                skipWs();
                char c = next();
                if (c == '}') return m;
                if (c != ',') throw err("expected , or }");
            }
        }

        List<Object> array() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWs();
            if (peek() == ']') { pos++; return list; }
            while (true) {
                list.add(value());
                skipWs();
                char c = next();
                if (c == ']') return list;
                if (c != ',') throw err("expected , or ]");
            }
        }

        String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) throw err("unterminated string");
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = next();
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > s.length()) throw err("bad \\u escape");
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw err("bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        Object number() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') pos++;
                else break;
            }
            if (pos == start) throw err("unexpected char '" + peek() + "'");
            return Double.parseDouble(s.substring(start, pos));
        }

        Boolean bool() {
            if (s.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (s.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            throw err("invalid literal");
        }

        Object nul() {
            if (s.startsWith("null", pos)) { pos += 4; return null; }
            throw err("invalid literal");
        }

        char peek() { return atEnd() ? '\0' : s.charAt(pos); }
        char next() { if (atEnd()) throw err("unexpected end"); return s.charAt(pos++); }
        void expect(char c) { if (next() != c) throw err("expected '" + c + "'"); }
        IllegalArgumentException err(String m) { return new IllegalArgumentException("JSON: " + m + " at " + pos); }
    }

    // ── writing ──────────────────────────────────────────────────────────

    /** A tiny builder so callers emit valid JSON without hand-concatenating quotes. */
    public static String escape(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public static String quote(String raw) {
        return raw == null ? "null" : "\"" + escape(raw) + "\"";
    }
}
