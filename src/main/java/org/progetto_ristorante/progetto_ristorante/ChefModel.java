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
                    SocketHandler orderSocket = new SocketProxy(acceptedOrder);
                    Thread chef = new Thread(new ChefHandler(orderSocket));
                    chef.start();
                }
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        });
        serverThread.start();
    }

    public static class ChefHandler implements Runnable {

        private final SocketHandler accepted; // identifies which waiter is connected

        public ChefHandler(SocketHandler accepted) { // constructor
            this.accepted = accepted;
        }

        public String getOrder() throws IOException { // gets a customer's order
            BufferedReader takeOrder = accepted.getReader();
            return takeOrder.readLine();
        }

        public void giveOrder(String order) throws IOException { // sends a ready order to the customer
            PrintWriter sendOrder = accepted.getWriter();
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
