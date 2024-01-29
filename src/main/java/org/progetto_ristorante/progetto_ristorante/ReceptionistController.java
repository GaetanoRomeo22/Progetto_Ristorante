package org.progetto_ristorante.progetto_ristorante;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;


public class ReceptionistController {
    private final ReceptionistModel model;
    private static final int PORT = 1313;
     private ServerSocket receptionSocket;


    public ReceptionistController(ReceptionistModel model) {
        this.model = model;
        try {
            receptionSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() throws IOException {
            while (true) {
                Socket acceptedClient = receptionSocket.accept();
                processRequest(acceptedClient);
            }
    }

    private void processRequest(Socket acceptedClient) {
        try {
            BufferedReader readSeatsNumber = new BufferedReader(new InputStreamReader(acceptedClient.getInputStream()));
            PrintWriter giveTableNumber = new PrintWriter(acceptedClient.getOutputStream(), true);

            int requiredSeats = Integer.parseInt(readSeatsNumber.readLine());

            int tableNumber = model.assignTable(requiredSeats, giveTableNumber);

            if (tableNumber == -1) {
                // Communicate to the customer that no table is available
                giveTableNumber.println(-1);
                readSeatsNumber.close();
                giveTableNumber.close();
                acceptedClient.close();

                // Apre una nuova connessione per inviare il tempo di attesa al cliente
                try (Socket waitingTimeSocket = receptionSocket.accept()) {
                    // Utilizzato per inviare al cliente il tempo di attesa
                    PrintWriter waitingTimeWriter = new PrintWriter(waitingTimeSocket.getOutputStream(), true);

                    // Genera un tempo di attesa casuale
                    int waitingTime = model.generateWaitingTime();

                    // Comunica al cliente il tempo di attesa
                    waitingTimeWriter.println(waitingTime);
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            }else {
                readSeatsNumber.close();
                giveTableNumber.close();
                acceptedClient.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ReceptionistModel model = new ReceptionistModel();
        ReceptionistController controller = new ReceptionistController(model);
        controller.startServer();
    }
}