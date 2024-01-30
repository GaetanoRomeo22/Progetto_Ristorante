package org.progetto_ristorante.progetto_ristorante;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WaiterController {

    public void startServer() {
        final int PORT_TO_CUSTOMER = 1316,  // used to communicate with customers
                  PORT_TO_CHEF = 1315;      // used to communicate with the chef

        // creates a socket to communicate with customers
        try (ServerSocket serverSocket = new ServerSocket(PORT_TO_CUSTOMER)) {
            while (true) {

                // accepts a connection
                Socket acceptedCustomer = serverSocket.accept();

                WaiterModel.WaiterHandler waiterHandler = new WaiterModel.WaiterHandler(acceptedCustomer, PORT_TO_CHEF);
                Thread waiterThread = new Thread(waiterHandler);
                waiterThread.start();
            }
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void main(String[] args) {
        WaiterController waiterController = new WaiterController();
        waiterController.startServer();
    }

}