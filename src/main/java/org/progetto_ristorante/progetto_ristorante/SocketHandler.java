package org.progetto_ristorante.progetto_ristorante;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

interface SocketHandler extends AutoCloseable {
    BufferedReader getReader() throws IOException; // gets socket's input stream reader
    PrintWriter getWriter() throws IOException; // gets socket's output stream reader
    void close() throws IOException; // closes the socket
}
