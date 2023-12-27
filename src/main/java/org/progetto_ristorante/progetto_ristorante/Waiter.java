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

            // creates a socket to communicate with the chef
            try (Socket chefSocket = new Socket(InetAddress.getLocalHost(), PORT_TO_CHEF)) {

                while (true) {
                    Socket acceptedCustomer = serverSocket.accept();

                    // Creazione di un nuovo thread per gestire la richiesta
                    Thread waiter = new Thread(new WaiterHandler(acceptedCustomer, chefSocket));
                    waiter.start();
                }
            } catch (IOException exc) {
                System.out.println("(Cameriere Principale) Impossibile comunicare con il cuoco");
                throw new RuntimeException(exc);
            }
        } catch (IOException exc) {
            System.out.println("(Cameriere Principale) Impossibile comunicare con il cliente");
            throw new RuntimeException(exc);
        }
    }


    public static class WaiterHandler implements Runnable {

        protected final Socket customerSocket,        // identifies which client is connected
                               chefSocket;            // socket to communicate with the chef
        BufferedReader readOrder,                     // used to read an order from a customer
                       readReadyOrder;                // used to get a ready order from the chef
        PrintWriter sendOrder,                        // used to send an order to the chef to prepare it
                    sendReadyOrder;                   // used to send a ready order to the customer who's ordered it

        // constructor
        public WaiterHandler(Socket accepted, Socket chefSocket) {
            this.customerSocket = accepted;
            this.chefSocket = chefSocket;
        }

        public void run() {
            try{
                readOrder = new BufferedReader(new InputStreamReader(customerSocket.getInputStream()));
                sendOrder = new PrintWriter(chefSocket.getOutputStream(), true);
                readReadyOrder = new BufferedReader(new InputStreamReader(chefSocket.getInputStream()));
                sendReadyOrder = new PrintWriter(customerSocket.getOutputStream(), true);
                String order;

                do {

                    // gets a customer's order
                    order = readOrder.readLine();

                    // if the customer has done
                    if (order == null || order.equalsIgnoreCase("fine")) {
                        break;
                    }

                    // sends the order to the chef to prepare it, waits and sends it back to the customer once it's ready
                    processOrder(order, sendOrder, readReadyOrder, sendReadyOrder);

                } while (true);

            } catch (IOException exc) {
                System.out.println("(Cameriere " + Thread.currentThread().threadId() + ") Errore lettura/scrittura dalla socket");
                throw new RuntimeException(exc);
            } finally {

                // closes the connection when the customer has done
                closeConnections();
            }
        }


        private void processOrder(String order, PrintWriter sendOrder, BufferedReader readReadyOrder, PrintWriter sendReadyOrder) throws IOException {

            // sends the order to the chef to prepare it
            System.out.println("(Cameriere " + Thread.currentThread().threadId() + ")" + order + ", mando l'ordine allo chef per prepararlo e attendo");
            sendOrder.println(order);

            // gets the order once it's ready
            order = readReadyOrder.readLine();

            // if the customer has done
            if (order == null || order.equalsIgnoreCase("fine")) {
                System.out.println("(Cameriere " + Thread.currentThread().threadId() + ")" + "Il cliente se ne è andato");
                sendOrder.println(order);
            }

            // else sends the order back to the customer
            else {
                System.out.println("(Cameriere " + Thread.currentThread().threadId() + ")" + order + " pronto, lo porto al cliente");
                sendReadyOrder.println(order);
            }
        }

        private void closeConnections() {
            try {
                System.out.println("(Cameriere " + Thread.currentThread().threadId() + ") Sto chiudendo la socket");
                customerSocket.close();
                readOrder.close();
                sendReadyOrder.close();
            } catch (IOException exc) {
                System.out.println("(Cameriere) Impossibile chiudere la connessione");
                throw new RuntimeException(exc);
            }
        }
    }
}
