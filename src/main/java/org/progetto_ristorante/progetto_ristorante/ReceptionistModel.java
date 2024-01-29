package org.progetto_ristorante.progetto_ristorante;

import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ReceptionistModel {
    private static final int MAX_TABLES = 20;
    private static final int MAX_SEATS = 100;

    private int availableSeats;
    private int availableTables;
    private final int[] tables;
    private final Semaphore semaphore;

    private static final Random rand = new Random();

    public ReceptionistModel() {
        semaphore = new Semaphore(1);
        availableSeats = MAX_SEATS;
        availableTables = MAX_TABLES;
        tables = new int[MAX_TABLES];
    }

    public  int assignTable(int requiredSeats, PrintWriter giveTableNumber) {
        try {
            semaphore.acquire();
            if (availableTables > 0 && availableSeats >= requiredSeats) {
                int tableNumber = findFreeTable();
                if (tableNumber != -1) {
                    tables[tableNumber] = 1;
                    availableTables--;
                    availableSeats -= requiredSeats;
                    giveTableNumber.println(tableNumber);
                    return tableNumber;
                }
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.schedule(() -> releaseTable(requiredSeats, tableNumber), rand.nextInt(), TimeUnit.SECONDS);
            }
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        } finally {
            semaphore.release();
        }
    }

    public  void releaseTable(int requiredSeats, int tableNumber) {
        tables[tableNumber] = 0;
        availableTables++;
        availableSeats += requiredSeats;
    }

    private int findFreeTable() {
        for (int i = 0; i < MAX_TABLES; i++) {
            if (tables[i] == 0) {
                return i;
            }
        }
        return -1; // No free table found
    }

    public  int generateWaitingTime() {
        return rand.nextInt(5) + 1;
    }
}
