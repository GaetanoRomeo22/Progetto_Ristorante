package org.progetto_ristorante.progetto_ristorante;

public interface SocketInitializedListener {
    void socketInitialized(SocketHandler socket); // indicates that the socket to communicate with the waiter has been created
}
