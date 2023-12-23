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
    final static int MAX_TABLES = 20;                    // number of restaurant's tables
    final static Object lock = new Object();             // lock to synchronize the access to the receptionist by many customers
    static Random rand = new Random();                   // used to generate random numbers
    static int availableSeats = 100,                     // number of available seats for customers
            availableTables = 20,                        // number of available tables for customers
            requiredSeats,                               // number of customer's required seats
            tableNumber;                                 // customer's table number
    static int [] tables = new int[MAX_TABLES];          // 0 in a cell means free table, 1 means occupied table
    static BufferedReader readSeatsNumber;               // used to read customer requested seats
    static PrintWriter giveTableNumber;                  // used to assign a table to the customer
    public static void main(String [] args) throws IOException {
        final int PORT = 1313;                           // used for the communication with customers

        // creates a socket to communicate with customers
        try (ServerSocket receptionSocket = new ServerSocket(PORT)) {
            do {

                // waits for a customer
                Socket acceptedClient = receptionSocket.accept();

                // synchronizes the access to the receptionist by many customers
                synchronized (lock) {

                    // reads customer's required seats
                    readSeatsNumber = new BufferedReader(new InputStreamReader(acceptedClient.getInputStream()));
                    giveTableNumber = new PrintWriter(acceptedClient.getOutputStream(), true);
                    requiredSeats = Integer.parseInt(readSeatsNumber.readLine());

                    // checks if there are enough available tables and seats
                    if (availableTables > 0 && availableSeats >= requiredSeats) {

                        // assigns a table to the customer and updates number of available tables and seats
                        assignTable();
                        acceptedClient.close();

                        // creates a scheduler to plan the periodic releasing of tables
                        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                        scheduler.schedule(() -> releaseTable(requiredSeats, tableNumber), 5, TimeUnit.SECONDS);
                    } else {
                        giveTableNumber.println(-1);
                        acceptedClient.close();

                        // creare una seconda connessione per comunicare il tempo di attesa
                        try (Socket waitingTimeSocket = receptionSocket.accept()) {
                            PrintWriter waitingTimeWriter = new PrintWriter(waitingTimeSocket.getOutputStream(), true);
                            int waitingTime = randomWaitingTime();
                            System.out.println("(Receptionist) Il tempo finché un tavolo si liberi è " + waitingTime + " minuti");
                            waitingTimeWriter.println(waitingTime);
                        } catch (IOException exc) {
                            System.out.println("(Receptionist) Impossibile comunicare con il cliente");
                            throw new RuntimeException(exc);
                        }
                    }
                }
            } while (true);
        } catch (IOException exc) {
            System.out.println("(Receptionist) Impossibile comunicare con i clienti");
            throw new RuntimeException(exc);
        } finally {

            // closes used resources
            readSeatsNumber.close();
            giveTableNumber.close();
        }
    }

    public static int randomWaitingTime() {
        Random random = new Random();
        return random.nextInt(30) + 1;
    }

    // allows receptionist to assign a seat to the customer and to update available seats and tables
    public static void assignTable() {

        // updates the number of available tables and of available seats
        availableTables--;
        availableSeats -= requiredSeats;

        // assigns a table to the customer generating a random number not already assigned
        do {
            tableNumber = rand.nextInt(MAX_TABLES);
        }
        while (tables[tableNumber] == 1);

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


