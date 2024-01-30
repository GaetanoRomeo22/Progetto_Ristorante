package org.progetto_ristorante.progetto_ristorante;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class WaiterModel {

    public static class WaiterHandler implements Runnable {
        protected final Socket customerSocket;    // socket to communicate with the customer
        protected final int CHEF_PORT;            // port to communicate with the chef
        BufferedReader readOrder;                 // used to get a customer's order
        BufferedReader readReadyOrder;            // used to get an order from the chef once ready
        PrintWriter sendOrder;                    // used to send an order to the chef to prepare it
        PrintWriter sendReadyOrder;               // used to send an order to the customer who ordered it once ready

        public WaiterHandler(Socket accepted, int CHEF_PORT) { // constructor
            this.customerSocket = accepted;
            this.CHEF_PORT = CHEF_PORT;
        }

        public void run() { // main of the thread
            try (Socket chefSocket = new Socket(InetAddress.getLocalHost(), CHEF_PORT)) { // creates a socket to communicate with the chef

                // sets the objects to read and write information through the socket
                readOrder = new BufferedReader(new InputStreamReader(customerSocket.getInputStream()));
                sendOrder = new PrintWriter(chefSocket.getOutputStream(), true);
                readReadyOrder = new BufferedReader(new InputStreamReader(chefSocket.getInputStream()));
                sendReadyOrder = new PrintWriter(customerSocket.getOutputStream(), true);

                String order;   // order to prepare
                do { // gets a customer's order
                    order = readOrder.readLine();
                    if (order == null) { // checks if customer has finished ordering
                        break;
                    }
                    sendOrder.println(order); // sends the order to the chef
                    order = readReadyOrder.readLine(); // gets the order from the chef once ready
                    sendReadyOrder.println(order); // sends the order to the customer who ordered it
                } while (true);
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            } finally { // once customer has finished, close the connection and each used object
                closeConnections();
            }
        }

        private void closeConnections() { // once customer has finished, close the connection and each used object
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
