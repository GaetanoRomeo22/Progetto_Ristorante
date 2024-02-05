package org.progetto_ristorante.progetto_ristorante;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceptionistController {
    protected final ReceptionistModel model;
    protected static final int CUSTOMER_PORT = 1313; // port used to communicate with customers
    protected final ServerSocket receptionSocket;    // socket used to communicate with customers

    public ReceptionistController(ReceptionistModel model) { // constructor
        this.model = model;
        try { // creates a socket to communicate with customers
            receptionSocket = new ServerSocket(CUSTOMER_PORT);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void main(String[] args) throws IOException {
        ReceptionistModel model = new ReceptionistModel();
        ReceptionistController controller = new ReceptionistController(model);
        controller.startServer();
    }

    public void startServer() throws IOException { // accepts a connection from a customer and manages it
        while (true) {
            Socket acceptedClient = receptionSocket.accept();
            processRequest(acceptedClient);
        }
    }

    private void processRequest(Socket acceptedClient) throws IOException { // manages a customer's request
        BufferedReader readSeatsNumber; // used to get customer's required seats
        PrintWriter giveTableNumber; // used to assign a table's number to the customer
        try { readSeatsNumber = new BufferedReader(new InputStreamReader(acceptedClient.getInputStream())); // reads data sent from client through the socket
             giveTableNumber = new PrintWriter(acceptedClient.getOutputStream(), true); // writes data to the client through the socket
            int requiredSeats = Integer.parseInt(readSeatsNumber.readLine()); // gets customer's required seats
            int tableNumber = model.assignTable(requiredSeats, giveTableNumber); // assigns the table to the customer
            if (tableNumber == 0) { // if the table isn't available
                giveTableNumber.println(0);
                try (Socket waitingTimeSocket = receptionSocket.accept()) { // creates a second socket to say to the customer the time to wait
                    PrintWriter waitingTimeWriter = new PrintWriter(waitingTimeSocket.getOutputStream(), true); // used to communicate to the customer the time he has to wait
                    int waitingTime = model.generateWaitingTime(); // generates a random time
                    waitingTimeWriter.println(waitingTime); // communicates the time to the customer
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            }
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }

        // closes used resources and connection
        readSeatsNumber.close();
        giveTableNumber.close();
        acceptedClient.close();
    }
}