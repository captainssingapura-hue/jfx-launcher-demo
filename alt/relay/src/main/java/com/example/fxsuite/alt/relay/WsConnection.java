package com.example.fxsuite.alt.relay;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * One WebSocket connection to an agent — a minimal, dependency-free RFC 6455
 * server endpoint. Handles the HTTP upgrade handshake and text/close/ping frames;
 * good enough for the small JSON messages this PoC exchanges (no fragmentation,
 * no compression).
 */
final class WsConnection {

    private static final String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    String path = "/";
    final Map<String, String> query = new HashMap<>();

    WsConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = socket.getOutputStream();
    }

    /** Complete the HTTP→WebSocket upgrade. Returns false if it isn't a valid WS request. */
    boolean handshake() throws IOException {
        String requestLine = readLine();
        if (requestLine == null) return false;
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) return false;

        String target = parts[1];
        int q = target.indexOf('?');
        path = q >= 0 ? target.substring(0, q) : target;
        if (q >= 0) parseQuery(target.substring(q + 1));

        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = readLine()) != null && !line.isEmpty()) {
            int c = line.indexOf(':');
            if (c > 0) headers.put(line.substring(0, c).trim().toLowerCase(), line.substring(c + 1).trim());
        }
        String key = headers.get("sec-websocket-key");
        if (key == null) return false;

        String accept = Base64.getEncoder().encodeToString(sha1(key + WS_MAGIC));
        String resp = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        out.write(resp.getBytes(StandardCharsets.US_ASCII));
        out.flush();
        return true;
    }

    /** Block for the next text message; returns null on close/EOF. Answers pings internally. */
    String readText() throws IOException {
        while (true) {
            int b0 = in.read();
            if (b0 == -1) return null;
            int opcode = b0 & 0x0F;

            int b1 = in.read();
            if (b1 == -1) return null;
            boolean masked = (b1 & 0x80) != 0;
            long len = b1 & 0x7F;
            if (len == 126) {
                len = ((long) (in.read() & 0xff) << 8) | (in.read() & 0xff);
            } else if (len == 127) {
                len = 0;
                for (int i = 0; i < 8; i++) len = (len << 8) | (in.read() & 0xff);
            }

            byte[] mask = new byte[4];
            if (masked) readFully(mask);
            byte[] payload = new byte[(int) len];
            readFully(payload);
            if (masked) for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i & 3];

            switch (opcode) {
                case 0x1 -> { return new String(payload, StandardCharsets.UTF_8); }   // text
                case 0x8 -> { return null; }                                          // close
                case 0x9 -> sendFrame(0xA, payload);                                  // ping -> pong
                default -> { /* pong/continuation — ignore */ }
            }
        }
    }

    synchronized void sendText(String s) throws IOException {
        sendFrame(0x1, s.getBytes(StandardCharsets.UTF_8));
    }

    private synchronized void sendFrame(int opcode, byte[] payload) throws IOException {
        ByteArrayOutputStream f = new ByteArrayOutputStream();
        f.write(0x80 | opcode);   // FIN + opcode
        int n = payload.length;   // server frames are never masked
        if (n < 126) {
            f.write(n);
        } else if (n < 65536) {
            f.write(126);
            f.write((n >> 8) & 0xff);
            f.write(n & 0xff);
        } else {
            f.write(127);
            for (int i = 7; i >= 0; i--) f.write((n >>> (8 * i)) & 0xff);
        }
        f.write(payload);
        out.write(f.toByteArray());
        out.flush();
    }

    void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    // --- helpers ---------------------------------------------------------

    private void parseQuery(String q) {
        for (String pair : q.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) query.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
    }

    private String readLine() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1 && c != '\n') b.write(c);
        if (c == -1 && b.size() == 0) return null;
        byte[] a = b.toByteArray();
        int len = a.length;
        if (len > 0 && a[len - 1] == '\r') len--;
        return new String(a, 0, len, StandardCharsets.US_ASCII);
    }

    private void readFully(byte[] a) throws IOException {
        int off = 0;
        while (off < a.length) {
            int r = in.read(a, off, a.length - off);
            if (r == -1) throw new EOFException();
            off += r;
        }
    }

    private static byte[] sha1(String s) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
