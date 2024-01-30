package org.progetto_ristorante.progetto_ristorante;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
public class ChefModel {
    private final int WAITER_PORT = 1315; // port to communicate with the waiter

    public void startServer() { // creates a thread
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

    public static String getOrder(Socket acceptedOrder) throws IOException { // gets a customer's order
        BufferedReader takeOrder = new BufferedReader(new InputStreamReader(acceptedOrder.getInputStream()));
        return takeOrder.readLine();
    }

    public static void giveOrder(Socket acceptedOrder, String order) throws IOException { // sends a ready order to the customer
        PrintWriter sendOrder = new PrintWriter(acceptedOrder.getOutputStream(), true);
        sendOrder.println(order);
    }

    public static class ChefHandler implements Runnable {

        protected final Socket accepted; // identifies which waiter is connected

        public ChefHandler(Socket accepted) { // constructor
            this.accepted = accepted;
        }

        public void run() { // main of the thread
            String order; // customer's order
            while (true) {
                try { // gets an order, prepares it and sends it back
                    order = getOrder(accepted);
                    if (order == null) {
                        break;
                    }
                    giveOrder(accepted, order);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
