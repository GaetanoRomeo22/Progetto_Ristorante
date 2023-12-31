package org.progetto_ristorante.progetto_ristorante;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class ChefController implements Initializable {

    @FXML
    private TextField menuOrderField,
            orderPriceField;

    @FXML
    private TextArea chefMenuArea;

    @FXML
    private static Text orderStatus;

    @FXML
    private Button commitOrderButton,
                   stopWriteButton;

    // disables automatic focus on interface's elements
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        menuOrderField.setFocusTraversable(false);
        orderPriceField.setFocusTraversable(false);
        chefMenuArea.setFocusTraversable(false);
        orderStatus = new Text();
    }

    public static void updateOrderStatus(String status) {
        orderStatus.setText(status);
    }

    @FXML
    private void cook() {

        // hides the interface
        hideInterface();

        final int PORT = 1315; // used for communication with waiters

        // starts a thread
        Thread serverThread = new Thread(() -> {

            // creates a server socket to communicate with waiters
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {

                    // waits for a waiter request
                    Socket acceptedOrder = serverSocket.accept();

                    // creates a new thread to manage the request
                    Thread chef = new Thread(new ChefHandler(acceptedOrder));
                    chef.start();
                }
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        });

        serverThread.setDaemon(true); // Imposta il thread come daemon per terminarlo con l'applicazione
        serverThread.start();
    }

    // adds an order into the menu
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

                // shows written menu
                showMenu();
            }
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    // gets an order to prepare by a waiter
    public static String getOrder(Socket acceptedOrder) throws IOException {
        BufferedReader takeOrder = new BufferedReader(new InputStreamReader(acceptedOrder.getInputStream()));
        return takeOrder.readLine();
    }

    // simulates the preparation of an order by the chef
    public static void prepareOrder() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException exc) {
            throw new RuntimeException(exc);
        }
    }

    // sends a ready order to the waiter who has required to prepare it
    public static void giveOrder(Socket acceptedOrder, String order) throws IOException {
        PrintWriter sendOrder = new PrintWriter(acceptedOrder.getOutputStream(), true);
        sendOrder.println(order);
    }

    // chef thread code
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
                        prepareOrder();

                        String finalOrder = order;
                        Platform.runLater(() -> updateOrderStatus(finalOrder + " pronto"));

                        // gives back the order to the waiter
                        giveOrder(currentSocket, order);
                    } catch (IOException exc) {
                        throw new RuntimeException(exc);
                    }
                }
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    // shows the menu in real time
    private void showMenu() {

        // reads menu from file
        try (FileReader fileReader = new FileReader("menu.txt")) {
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String order;
            float price;

            // shows the menu
            while ((order = bufferedReader.readLine()) != null) {
                price = Float.parseFloat(bufferedReader.readLine());
                chefMenuArea.appendText("Piatto: " + order + System.lineSeparator());
                chefMenuArea.appendText("Prezzo: " + price + System.lineSeparator());
                chefMenuArea.appendText("\n");
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    // hides the interface once the chef has finished to write the menu
    private void hideInterface() {
        menuOrderField.setManaged(false);
        menuOrderField.setVisible(false);
        orderPriceField.setManaged(false);
        orderPriceField.setVisible(false);
        stopWriteButton.setManaged(false);
        stopWriteButton.setVisible(false);
        commitOrderButton.setManaged(false);
        commitOrderButton.setVisible(false);
    }
}


