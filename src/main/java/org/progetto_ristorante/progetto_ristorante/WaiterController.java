package org.progetto_ristorante.progetto_ristorante;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WaiterController {

    protected final WaiterModel model;
    protected final int CUSTOMER_PORT = 1316;  // port to communicate with customers
    protected final int CHEF_PORT     = 1315;  // port to communicate with the chef
    protected final ServerSocket waiterSocket; // socket to communicate with customers

    public WaiterController(WaiterModel waiterModel) { // constructor
        this.model = waiterModel;
        try { // creates a socket to communicate with customers
            waiterSocket = new ServerSocket(CUSTOMER_PORT);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void main(String[] args) throws IOException {
        WaiterModel waiterModel = new WaiterModel();
        WaiterController waiterController = new WaiterController(waiterModel);
        waiterController.startServer();
    }

    public void startServer() throws IOException {
            while (true) { // accepts a connection and creates a thread to manage the request
                Socket acceptedCustomer = waiterSocket.accept();
                WaiterModel.WaiterHandler waiterHandler = new WaiterModel.WaiterHandler(acceptedCustomer, CHEF_PORT);
                Thread waiterThread = new Thread(waiterHandler);
                waiterThread.start();
            }
    }
}