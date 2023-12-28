package org.progetto_ristorante.progetto_ristorante;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class ChefController implements Initializable {

    @FXML
    private VBox chefInterface;

    @FXML
    private TextField menuOrderField,
            orderPriceField;
    @FXML
    private static Text orderStatus;

    @FXML
    private Button Conferma,
                   Fine;

    // disables automatic focus on interface's elements
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        menuOrderField.setFocusTraversable(false);
        orderPriceField.setFocusTraversable(false);
        orderStatus = new Text();
    }

    public static void updateOrderStatus(String status) {
        // Puoi utilizzare un controllo JavaFX, come un Text o una Label, per mostrare lo stato
        // In questo esempio, si presume che tu abbia un oggetto Text chiamato orderStatusText

        orderStatus.setText(status);
    }

    @FXML
    private void cook() {

        final int PORT = 1315;              // used for communication with waiters


        menuOrderField.setManaged(false);
        menuOrderField.setVisible(false);
        orderPriceField.setManaged(false);
        orderPriceField.setVisible(false);

        Fine.setManaged(false);
        Fine.setVisible(false);
        Conferma.setManaged(false);
        Conferma.setVisible(false);
        // Avvia il thread per il server socket
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    // Attende una richiesta di ordine da parte di un cameriere
                    Socket acceptedOrder = serverSocket.accept();

                    // Crea un nuovo thread per gestire la richiesta
                    Thread chef = new Thread(new ChefHandler(acceptedOrder));
                    chef.start();
                }
            } catch (IOException exc) {
                System.out.println("(Cuoco) Impossibile comunicare con il cameriere");
                throw new RuntimeException(exc);
            }
        });

        serverThread.setDaemon(true); // Imposta il thread come daemon per terminarlo con l'applicazione
        serverThread.start();
    }

    @FXML
    private void addOrder() {

        // tries to open the file in read mode
        try (FileWriter menuWriter = new FileWriter("menu.txt")) {
            PrintWriter writer = new PrintWriter(menuWriter, true);   // object to write into the file

            // reads order's name and price
            String order = menuOrderField.getText(),
                    inputPrice = orderPriceField.getText();
            inputPrice = inputPrice.replace(',', '.');

            // checks if the chef has entered an order
            if (!order.isEmpty()) {

                // checks if the chef has entered order's price
                if (!inputPrice.isEmpty()) {

                    // writes order's name and order's price into the file separated by a line
                    float price = Float.parseFloat(inputPrice);
                    writer.println(order);
                    writer.println(price);
                }

                // clears previous text
                menuOrderField.setText("");
                orderPriceField.setText("");
            }
        } catch (IOException exc) {
            System.out.println("(Cuoco) Errore scrittura menù");
            throw new RuntimeException(exc);
        }
    }

    // gets an order to prepare by a waiter
    public static String getOrder(Socket acceptedOrder) throws IOException {
        BufferedReader takeOrder = new BufferedReader(new InputStreamReader(acceptedOrder.getInputStream()));
        return takeOrder.readLine();
    }

    // simulates the preparation of an order by the chef
    public static void prepareOrder(String order) {
        System.out.println("(Cuoco) Preparo: " + order);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException exc) {
            System.out.println("(Cuoco) Errore utilizzo sleep");
            throw new RuntimeException(exc);
        }
        System.out.println("(Cuoco) " + order + " pronto");
    }

    // sends a ready order to the waiter who has required to prepare it
    public static void giveOrder(Socket acceptedOrder, String order) throws IOException {
        PrintWriter sendOrder = new PrintWriter(acceptedOrder.getOutputStream(), true);
        System.out.println("(Cuoco) Invio " + order + " al cameriere");
        sendOrder.println(order);
    }

    record ChefHandler(Socket accepted) implements Runnable {

        public void run() {
            try (Socket currentSocket = accepted) {
                // gets the order to prepare by the waiter
                String order;
                while (true) {
                    try {
                        // gets an order
                        order = getOrder(currentSocket);
                        if (order.equalsIgnoreCase("fine")) {
                            break;
                        }

                        // prepares the order
                        prepareOrder(order);

                        String finalOrder = order;
                        Platform.runLater(() -> updateOrderStatus(finalOrder + " pronto"));

                        // gives back the order to the waiter
                        giveOrder(currentSocket, order);
                    } catch (IOException exc) {
                        System.out.println("(Chef " + Thread.currentThread().getId() + ") Errore generico");
                        throw new RuntimeException(exc);
                    }
                }
            } catch (IOException exc) {
                System.out.println("(Chef " + Thread.currentThread().getId() + ") Errore chiusura connessione");
                throw new RuntimeException(exc);
            }
        }
    }
}


