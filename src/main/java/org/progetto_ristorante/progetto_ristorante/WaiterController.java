package org.progetto_ristorante.progetto_ristorante;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WaiterController {

    public static void main(String[] args) {
        WaiterController waiterController = new WaiterController();
        waiterController.startServer();
    }

    public void startServer() {
        final int CUSTOMER_PORT = 1316; // port to communicate with customers
        final int CHEF_PORT     = 1315; // port to communicate with the chef
        try (ServerSocket serverSocket = new ServerSocket(CUSTOMER_PORT)) { // creates a socket to communicate with customers
            while (true) { // accepts a connection and creates a thread to manage the request
                Socket acceptedCustomer = serverSocket.accept();
                WaiterModel.WaiterHandler waiterHandler = new WaiterModel.WaiterHandler(acceptedCustomer, CHEF_PORT);
                Thread waiterThread = new Thread(waiterHandler);
                waiterThread.start();
            }
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }
}