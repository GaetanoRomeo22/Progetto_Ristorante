package org.progetto_ristorante.progetto_ristorante;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WaiterController {
    private final int PORT_TO_CUSTOMER = 1316;
    private final int PORT_TO_CHEF = 1315;

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_TO_CUSTOMER)) {
            while (true) {
                Socket acceptedCustomer = serverSocket.accept();
                WaiterModel.WaiterHandler waiterHandler = new WaiterModel.WaiterHandler(acceptedCustomer, PORT_TO_CHEF);
                Thread waiterThread = new Thread(waiterHandler);
                waiterThread.start();
            }
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void main(String[] args){
        WaiterController waiterController = new WaiterController();
        waiterController.startServer();
    }

}