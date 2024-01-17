package org.progetto_ristorante.progetto_ristorante;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class Prova {

    // customer's table's number
    protected static int table;

    // Method to allow the customer to specify how many seats they need and get a table if available
    private static int getTable(Socket receptionSocket) throws IOException {

        // used to get customer's required seats and to send it to the receptionist
        BufferedReader checkSeats = new BufferedReader(new InputStreamReader(receptionSocket.getInputStream()));
        PrintWriter sendSeats = new PrintWriter(receptionSocket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Di quanti posti hai bisogno ?");
        int requiredSeats = Integer.parseInt(reader.readLine());

        // says how many seats he requires to the receptionist
        sendSeats.println(requiredSeats);

        // gets the table number from the receptionist if it's possible
        int tableNumber = Integer.parseInt(checkSeats.readLine());

        // closes used resources
        checkSeats.close();
        sendSeats.close();
        receptionSocket.close();

        return tableNumber;
    }

    private static void getRequiredSeats() {
        try {
            final int RECEPTIONIST_PORT = 1313; // used to communicate with the receptionist

            // creates a socket to communicate with the receptionist
            Socket receptionSocket = new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT);

            // says how many seats he needs to the receptionist and gets a table
            table = getTable(receptionSocket);
            System.out.println("Ottengo il tavolo " + table);

            // closes connection with the receptionist
            receptionSocket.close();
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void getOrder() {
        final int WAITER_PORT = 1316; // used to communicate with the waiter

        try {
            // creates a socket to communicate with the waiter
            Socket waiterSocket = new Socket(InetAddress.getLocalHost(), WAITER_PORT);

            // used to get a customer's order and to send it to a waiter
            BufferedReader eatOrder = new BufferedReader(new InputStreamReader(waiterSocket.getInputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter takeOrder = new PrintWriter(waiterSocket.getOutputStream(), true);

            System.out.println("Cosa ordini ?");
            String order = reader.readLine();

            // sends the order to the waiter
            takeOrder.println(order);

            // waits for the order and eats it
            order = eatOrder.readLine();
            System.out.println("Mangio " + order);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void main(String[] args) {
        getRequiredSeats();
        for (int i = 0; i < 3; i++)
            getOrder();
    }
}
