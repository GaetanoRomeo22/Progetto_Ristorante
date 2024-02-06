package org.progetto_ristorante.progetto_ristorante;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class ServerSocketProxy implements ServerSocketHandler {

    private final ServerSocket serverSocket; // server socket

    ServerSocketProxy(int port) throws IOException { // constructor (creates a server socket)
        this.serverSocket = new ServerSocket(port);
    }

    public Socket accept() throws IOException { // accepts a connection
        return serverSocket.accept();
    }

    public void close() throws IOException { // closes the socket
        serverSocket.close();
    }
}
