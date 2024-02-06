package org.progetto_ristorante.progetto_ristorante;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class WaiterModel {
    public static class WaiterHandler implements Runnable {
        private final SocketHandler customerSocket; // socket to communicate with the customer
        private final int CHEF_PORT;                // port to communicate with the chef
        private BufferedReader readOrder;           // used to get a customer's order
        protected BufferedReader readReadyOrder;    // used to get an order from the chef once ready
        protected PrintWriter sendOrder;            // used to send an order to the chef to prepare it
        private PrintWriter sendReadyOrder;         // used to send an order to the customer who ordered it once ready

        public WaiterHandler(SocketHandler accepted, int CHEF_PORT) { // constructor
            this.customerSocket = accepted;
            this.CHEF_PORT = CHEF_PORT;
        }

        private String sendOrderToChef(SocketHandler chefSocket) throws IOException { // gets an order from a customer and sends it to the chef to prepare it
            readOrder = customerSocket.getReader();
            sendOrder = chefSocket.getWriter();
            String order = readOrder.readLine();
            sendOrder.println(order);
            return order;
        }

        private void sendOrderToCustomer(SocketHandler chefSocket) throws IOException { // gets an order from the chef and sends it to the customer who ordered it
            readReadyOrder = chefSocket.getReader();
            sendReadyOrder = customerSocket.getWriter();
            String order = readReadyOrder.readLine();
            sendReadyOrder.println(order);
        }

        public void run() { // main of the thread
            try (SocketHandler chefSocket = new SocketProxy(new Socket(InetAddress.getLocalHost(), CHEF_PORT))) { // creates a socket to communicate with the chef
                String order; // customer's order
                do {
                    order = sendOrderToChef(chefSocket); // gets a customer's order and sends it to the chef
                    if (order == null) { // checks if customer has finished ordering
                        break;
                    }
                    sendOrderToCustomer(chefSocket); // gets a prepared order from the chef and sends it to the customer
                } while (true);
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            } finally { // closes used resources and connection
                closeConnection();
            }
        }

        private void closeConnection() { // once customer has finished, close the connection and each used resource
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
