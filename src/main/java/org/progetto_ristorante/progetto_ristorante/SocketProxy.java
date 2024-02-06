package org.progetto_ristorante.progetto_ristorante;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class SocketProxy implements SocketHandler {
    private final Socket socket; // socket

    SocketProxy(Socket socket) { // constructor (creates a socket)
        this.socket = socket;
    }

    public BufferedReader getReader() throws IOException { // gets socket's input stream reader
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public PrintWriter getWriter() throws IOException { // gets socket's output stream reader
        return new PrintWriter(socket.getOutputStream(), true);
    }

    public void close() throws IOException { // closes the socket
        socket.close();
    }
}
