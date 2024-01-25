package org.progetto_ristorante.progetto_ristorante;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.*;

public class Receptionist {
    final static int MAX_TABLES = 20;                               // number of restaurant's tables
    final static Semaphore semaphore = new Semaphore(1);    // semaphore to synchronize processes
    static Random rand = new Random();                              // used to generate random numbers
    static int availableSeats = 100,                                // number of available seats for customers
               availableTables = 20,                                // number of available tables for customers
               requiredSeats,                                       // number of customer's required seats
               tableNumber;                                         // customer's table number
    static int [] tables = new int[MAX_TABLES];                     // 0 in a cell means free table, 1 means occupied table
    static BufferedReader readSeatsNumber;                          // used to read customer requested seats
    static PrintWriter giveTableNumber;                             // used to assign a table to the customer

    public static void main(String [] args) throws IOException {
        final int PORT = 1313;                           // used for the communication with customers

        // creates a socket to communicate with customers
        try (ServerSocket receptionSocket = new ServerSocket(PORT)) {
            do {

                // waits for a customer
                Socket acceptedClient = receptionSocket.accept();

                // synchronizes the access to the receptionist by many customers
                try {

                    // acquire the semaphore to manage client's request
                    semaphore.acquire();

                    // used to get customer's required seats
                    readSeatsNumber = new BufferedReader(new InputStreamReader(acceptedClient.getInputStream()));

                    // used to send to the customer his table number
                    giveTableNumber = new PrintWriter(acceptedClient.getOutputStream(), true);

                    // gets customer's required seats from the interface and parses it into an integer
                    requiredSeats = Integer.parseInt(readSeatsNumber.readLine());

                    // checks if there are enough available tables and seats
                    if (availableTables > 0 && availableSeats >= requiredSeats) {

                        // assigns a table to the customer and updates number of available tables and seats
                        assignTable();

                        // closes the connection with the customer
                        acceptedClient.close();

                        // creates a scheduler to plan the periodic releasing of tables
                        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                        scheduler.schedule(() -> releaseTable(requiredSeats, tableNumber), rand.nextInt(), TimeUnit.SECONDS);
                    } else {

                        // if there aren't available seats, communicates it to the customer
                        giveTableNumber.println(-1);

                        // closes the connection with the customer
                        acceptedClient.close();

                        // creates a second connection to get if the customer wants to wait to get seats or wants to leave
                        try (Socket waitingTimeSocket = receptionSocket.accept()) {

                            // used to communicate to the customer the time he has to wait
                            PrintWriter waitingTimeWriter = new PrintWriter(waitingTimeSocket.getOutputStream(), true);

                            // generates a random waiting time
                            int waitingTime = rand.nextInt(5) + 1;

                            // communicates it to the customer
                            waitingTimeWriter.println(waitingTime);
                        } catch (IOException exc) {
                            throw new RuntimeException(exc);
                        }
                    }
                } catch (InterruptedException exc) {
                    throw new RuntimeException(exc);
                } finally { // releases the semaphore to manage next customer's request
                    semaphore.release();
                }
            } while (true);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        } finally { // closes used resources
            readSeatsNumber.close();
            giveTableNumber.close();
        }
    }

    // allows receptionist to assign a seat to the customer and to update available seats and tables
    public static void assignTable() {

        // updates the number of available tables and of available seats
        availableTables--;
        availableSeats -= requiredSeats;

        // assigns a npt assigned table to the customer generating a random number
        do {
            tableNumber = rand.nextInt(MAX_TABLES);
        } while (tables[tableNumber] == 1);

        // sets the table as occupied
        tables[tableNumber] = 1;

        // sends the table number to che customer
        giveTableNumber.println(tableNumber);
    }

    // allows the receptionist to release a table and to update available seats and tables
    public static void releaseTable(int requiredSeats, int tableNumber) {

        // updates the number of available tables and of available seats
        availableTables++;
        availableSeats += requiredSeats;

        // sets the table as free
        tables[tableNumber] = 0;
    }
}