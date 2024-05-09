package org.progetto_ristorante.progetto_ristorante;

import java.io.IOException;
import java.net.Socket;

public class WaiterController { // manages waiter's work

    protected final WaiterModel model; // reference to the Model
    private final ServerSocketHandler waiterSocket; // socket to communicate with customers

    public WaiterController(WaiterModel waiterModel) { // constructor
        this.model = waiterModel;
        int CUSTOMER_PORT = 1316; // port to communicate with customers
        try { // creates a socket to communicate with customers
            waiterSocket = new ServerSocketProxy(CUSTOMER_PORT);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void main() throws IOException {
        WaiterModel waiterModel = new WaiterModel();
        WaiterController waiterController = new WaiterController(waiterModel);
        waiterController.startServer();
    }

    public void startServer() throws IOException {
        while (true) { // accepts a connection and creates a thread to manage the request
            Socket acceptedCustomer = waiterSocket.accept();
            SocketHandler customerSocket = new SocketProxy(acceptedCustomer);
            int CHEF_PORT = 1315; // port to communicate with the chef
            WaiterModel.WaiterHandler waiterHandler = new WaiterModel.WaiterHandler(customerSocket, CHEF_PORT); // creates a handler to manage the request
            Thread waiterThread = new Thread(waiterHandler); // creates a thread
            waiterThread.start(); // starts the thread
        }
    }
}