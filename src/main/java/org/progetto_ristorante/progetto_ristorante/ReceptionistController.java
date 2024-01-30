package org.progetto_ristorante.progetto_ristorante;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceptionistController {
    private final ReceptionistModel model;
    private static final int CUSTOMER_PORT = 1313; // port used to communicate with customers
    private final ServerSocket receptionSocket;    // socket used to communicate with customers

    public ReceptionistController(ReceptionistModel model) { // constructor
        this.model = model;
        try { // creates a socket to communicate with customers
            receptionSocket = new ServerSocket(CUSTOMER_PORT);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public void startServer() throws IOException { // accepts a connection from a customer and manages it
        while (true) {
            Socket acceptedClient = receptionSocket.accept();
            processRequest(acceptedClient);
        }
    }

    private void processRequest(Socket acceptedClient) { // manages a customer's request
        try { // used to get customer's required seats and to assign a table's number to him
            BufferedReader readSeatsNumber = new BufferedReader(new InputStreamReader(acceptedClient.getInputStream()));
            PrintWriter giveTableNumber = new PrintWriter(acceptedClient.getOutputStream(), true);

            int requiredSeats = Integer.parseInt(readSeatsNumber.readLine()); // gets customer's required seats
            int tableNumber = model.assignTable(requiredSeats, giveTableNumber); // assigns the table to the customer

            if (tableNumber == -1) { // if the table isn't available
                giveTableNumber.println(-1);
                readSeatsNumber.close();
                giveTableNumber.close();
                acceptedClient.close();

                try (Socket waitingTimeSocket = receptionSocket.accept()) { // creates a socket to accept a connection
                    PrintWriter waitingTimeWriter = new PrintWriter(waitingTimeSocket.getOutputStream(), true); // used to communicate to the customer the time he has to wait
                    int waitingTime = model.generateWaitingTime(); // generates a random time
                    waitingTimeWriter.println(waitingTime); // communicates the time to the customer
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            } else {
                readSeatsNumber.close();
                giveTableNumber.close();
                acceptedClient.close();
            }
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void main(String[] args) throws IOException {
        ReceptionistModel model = new ReceptionistModel();
        ReceptionistController controller = new ReceptionistController(model);
        controller.startServer();
    }
}