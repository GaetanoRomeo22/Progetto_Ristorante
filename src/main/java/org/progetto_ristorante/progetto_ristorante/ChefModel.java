package org.progetto_ristorante.progetto_ristorante;

import java.io.*;
import java.net.Socket;
public class ChefModel {
    private final int WAITER_PORT = 1315;  // port to communicate with the waiter

    public void startServer() {
        Thread serverThread = new Thread(() -> { // creates chef's server thread
            try (ServerSocketHandler chefSocket = new ServerSocketProxy(WAITER_PORT)) { // creates a socket to communicate with the waiter
                while (true) {
                    Socket acceptedOrder = chefSocket.accept(); // accepts a connection
                    SocketHandler orderSocket = new SocketProxy(acceptedOrder);
                    Thread chef = new Thread(new ChefHandler(orderSocket)); // creates a thread to manage the request
                    chef.start(); // starts the thread
                }
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        });
        serverThread.start(); // starts chef's server thread
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
                    if (order == null) { // checks if customer has ended ordering
                        break;
                    }
                    giveOrder(order); // sends the order back to the waiter once ready
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            }
        }
    }
}
