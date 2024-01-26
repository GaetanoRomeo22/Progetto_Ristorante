package org.progetto_ristorante.progetto_ristorante;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Waiter {

    public static void main(String[] args) {
        final int PORT_TO_CUSTOMER = 1316,      // used for the communication with customers
                  PORT_TO_CHEF = 1315;          // used for the communication with the chef

        // creates a socket to communicate with customers
        try (ServerSocket serverSocket = new ServerSocket(PORT_TO_CUSTOMER)) {
            while (true) {

                // accepts a connection
                Socket acceptedCustomer = serverSocket.accept();

                // creates a new thread to manage the request
                Thread waiter = new Thread(new WaiterHandler(acceptedCustomer, PORT_TO_CHEF));
                waiter.start();
            }
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    // waiter thread's class
    public static class WaiterHandler implements Runnable {

        protected final Socket customerSocket;        // identifies which customer is connected
        protected final int PORT_TO_CHEF;                 // port to communicate with the chef
        BufferedReader readOrder,                     // used to read an order from a customer
                       readReadyOrder;                // used to get a ready order from the chef
        PrintWriter sendOrder,                        // used to send an order to the chef to prepare it
                    sendReadyOrder;                   // used to send a ready order to the customer who's ordered it

        // constructor
        public WaiterHandler(Socket accepted, int PORT_TO_CHEF) {
            this.customerSocket = accepted;
            this.PORT_TO_CHEF= PORT_TO_CHEF;
        }

        // thread's main (when it's created, it starts from here)
        public void run() {

            // creates a socket to communicate with the chef
            try (Socket chefSocket = new Socket(InetAddress.getLocalHost(), PORT_TO_CHEF)) {

                // used to get a customer's order and to send it to the chef to prepare it
                readOrder = new BufferedReader(new InputStreamReader(customerSocket.getInputStream()));
                sendOrder = new PrintWriter(chefSocket.getOutputStream(), true);

                // used to get a ready order from the chef and to send it back to the customer who ordered it
                readReadyOrder = new BufferedReader(new InputStreamReader(chefSocket.getInputStream()));
                sendReadyOrder = new PrintWriter(customerSocket.getOutputStream(), true);

                // customer's order
                String order;

                // the thread continues until customer has finished ordering
                do {

                    // gets a customer's order
                    order = readOrder.readLine();

                    // if the customer has done
                    if (order == null) {
                        break;
                    }

                    // sends the order to the chef to prepare it
                    sendOrder.println(order);

                    // gets the order once it's ready
                    order = readReadyOrder.readLine();

                    // sends the order back to the customer
                    sendReadyOrder.println(order);
                } while (true);
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            } finally { // closes the connection when the customer has done
                closeConnections();
            }
        }

        // closes each used connection
        private void closeConnections() {
            try {
                customerSocket.close();
                readOrder.close();
                sendReadyOrder.close();
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        }
    }
}