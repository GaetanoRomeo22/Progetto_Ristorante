package org.progetto_ristorante.progetto_ristorante;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ReceptionistModel {
    private static final int MAX_TABLES = 20; // max number of tables of the restaurant
    private static final int MAX_SEATS = 100; // max number of seats of the restaurant
    private int availableSeats; // number of current available seats
    private int availableTables; // number of current available tables
    private final int[] tables; // array to manage tables (0 means free and 1 occupied)
    private final Semaphore semaphore = new Semaphore(1); // semaphore to synchronize the access to the receptionist
    private static final Random rand = new Random(Instant.now().getEpochSecond()); // used to generate random numbers by a specified seed

    public ReceptionistModel() { // constructor
        availableSeats = MAX_SEATS; // sets the number of available seats
        availableTables = MAX_TABLES; // sets the number of available tables
        tables = new int[MAX_TABLES]; // allocates an array of MAX_TABLES that indicates restaurant's menu
        for (int i = 0; i < MAX_TABLES; i++) // sets each table as free
            tables[i] = 0;
    }

    public int assignTable(int requiredSeats, PrintWriter giveTableNumber) { // assigns a table to a customer if possible
        try {
            semaphore.acquire(); // gets the semaphore to manage a request
            if (availableTables > 0 && availableSeats >= requiredSeats) { // checks if there are available seats and tables
                int tableNumber = findFreeTable(requiredSeats); // assigns first free table to the customer
                if (tableNumber != 0) { // if there is at least a free table and enough seats
                    giveTableNumber.println(tableNumber); // assigns the table to the customer
                    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // creates a scheduler to manage the release of the tables
                    scheduler.schedule(() -> releaseTable(requiredSeats, tableNumber), rand.nextInt(), TimeUnit.SECONDS); // the scheduler release tables periodically
                    return tableNumber;
                }
            }
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
        } finally { // releases the semaphore to manage next request
            semaphore.release();
        } return 0;
    }

    public  void releaseTable(int requiredSeats, int tableNumber) { // releases a table, setting it as free and increasing available seats
        tables[tableNumber - 1] = 0;
        availableTables++;
        availableSeats += requiredSeats;
    }

    private int findFreeTable(int requiredSeats) { // assigns first free table to the customer or returns 0 if there aren't enough seats or a free table
        for (int i = 0; i < MAX_TABLES; i++) {
            if (tables[i] == 0) { // if the table is free
                tables[i] = 1;  // sets the table as occupied
                availableTables--; // decreases available tables
                availableSeats -= requiredSeats; // decreases available seats
                return i + 1; // returns table number
            }
        }
        return 0;
    }

    public  int generateWaitingTime() { // generates a random number to simulates the time the customer has to wait
        return rand.nextInt(5) + 1;
    }
}
