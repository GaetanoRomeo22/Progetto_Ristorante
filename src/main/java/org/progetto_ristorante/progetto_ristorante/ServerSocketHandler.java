package org.progetto_ristorante.progetto_ristorante;

import java.io.IOException;
import java.net.Socket;

interface ServerSocketHandler extends AutoCloseable {
    Socket accept() throws IOException; // accepts a connection

    void close() throws IOException; // closes the socket
}
