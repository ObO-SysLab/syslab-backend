package net.diveon.backend.support;

import java.io.IOException;
import java.net.ServerSocket;

/** A free local port for disposable test servers (2026-11-09). Windows sometimes refuses the first bind with
 * "Bad address: listen"; trying again after a short pause has always worked, so the tests retry up to three times. */
public final class TestPorts {
    private TestPorts() {}
    public static int freePort() throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try (var socket = new ServerSocket(0)) { return socket.getLocalPort(); }
            catch (IOException error) { last = error; Thread.sleep(100); }
        }
        throw last;
    }
}
