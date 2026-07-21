package com.example.fxsuite.alt.relay;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A tiny WebSocket server: accepts connections, upgrades them, and runs a
 * blocking read loop per connection on its own thread. All policy (auth,
 * registry, message handling) lives in the {@link Handler}.
 */
final class WsServer {

    interface Handler {
        void onConnect(WsConnection c) throws IOException;
        void onMessage(WsConnection c, String message);
        void onClose(WsConnection c);
    }

    private final int port;
    private final Handler handler;

    WsServer(int port, Handler handler) {
        this.port = port;
        this.handler = handler;
    }

    void start() throws IOException {
        ServerSocket ss = new ServerSocket(port);
        Thread acceptor = new Thread(() -> {
            while (true) {
                try {
                    Socket s = ss.accept();
                    Thread t = new Thread(() -> serve(s));
                    t.setDaemon(true);
                    t.start();
                } catch (IOException e) {
                    break;
                }
            }
        }, "ws-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
        System.out.println("[relay] agent WebSocket server on ws://localhost:" + port + "/agent");
    }

    private void serve(Socket s) {
        WsConnection c = null;
        try {
            c = new WsConnection(s);
            if (!c.handshake()) { s.close(); return; }
            handler.onConnect(c);
            String msg;
            while ((msg = c.readText()) != null) {
                handler.onMessage(c, msg);
            }
        } catch (IOException e) {
            // client disconnected / io error — fall through to close
        } finally {
            if (c != null) handler.onClose(c);
            try { s.close(); } catch (IOException ignored) {}
        }
    }
}
