package org.progetto_ristorante.progetto_ristorante;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
public class ChefModel {
    private final int WAITER_PORT = 1315;  // port to communicate with the waiter

    public void startServer() { // creates a thread chef
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(WAITER_PORT)) { // creates a socket to communicate with the waiter
                while (true) { // accepts a connection and creates a thread to manage the request
                    Socket acceptedOrder = serverSocket.accept();
                    Thread chef = new Thread(new ChefHandler(acceptedOrder));
                    chef.start();
                }
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        });
        serverThread.start();
    }

    public static class ChefHandler implements Runnable {

        protected final Socket accepted; // identifies which waiter is connected

        public ChefHandler(Socket accepted) { // constructor
            this.accepted = accepted;
        }

        public String getOrder() throws IOException { // gets a customer's order
            BufferedReader takeOrder = new BufferedReader(new InputStreamReader(accepted.getInputStream()));
            return takeOrder.readLine();
        }

        public void giveOrder(String order) throws IOException { // sends a ready order to the customer
            PrintWriter sendOrder = new PrintWriter(accepted.getOutputStream(), true);
            sendOrder.println(order);
        }

        public void run() { // main of the thread
            String order; // customer's order
            while (true) {
                try { // gets an order
                    order = getOrder();
                    if (order == null) {
                        break;
                    }
                    String finalOrder = order;
                    giveOrder(finalOrder);
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            }
        }
    }
}
