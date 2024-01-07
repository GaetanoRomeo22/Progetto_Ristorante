package org.progetto_ristorante.progetto_ristorante;

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChefController implements Initializable {

    @FXML
    private TextField menuOrderField,
            orderPriceField;

    @FXML
    private TextArea menu;

    @FXML
    private static Text invalidData;

    @FXML
    private Button commitOrderButton,
                   stopWriteButton;

    // disables automatic focus on interface's elements
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // shows written menu
        showMenu();
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
    private void addOrder() throws SQLException {

        // reads order's name and price
        String order = menuOrderField.getText(),
                inputPrice = orderPriceField.getText();

        // checks if the chef has entered an order and a price
        if (!order.isEmpty() && !inputPrice.isEmpty()) {

            inputPrice = inputPrice.replace(',', '.');

            // connection to the database
            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

                // query to check if the user is registered
                String selectQuery = "SELECT * FROM ORDINI WHERE NOME = ?";
                try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

                    // substitutes ? with order's name
                    selectStatement.setString(1, order);

                    // if the order is already in the menu, shows an error message, otherwise inserts it into the menu
                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        if (resultSet.next()) {
                            invalidData.setText("Ordine gia presente nel menu");
                            invalidData.setVisible(true);
                        } else {

                            // query to insert the order into the menu
                            String insertQuery = "INSERT INTO ORDINI (NOME, PREZZO) VALUES (?, ?)";
                            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {

                                // substitutes ? with username and password
                                insertStatement.setString(1, order);
                                insertStatement.setString(2, inputPrice);

                                // performs the insert
                                insertStatement.executeUpdate();

                                // clears previous text
                                menuOrderField.setText("");
                                orderPriceField.setText("");
                            }
                        }
                    }
                }
            }
        } else {
            invalidData.setText("Ordine o prezzo mancante");
            invalidData.setVisible(true);
        }

        // shows written menu
        showMenu();
    }

    // gets an order to prepare by a waiter
    public static String getOrder(Socket acceptedOrder) throws IOException {
        BufferedReader takeOrder = new BufferedReader(new InputStreamReader(acceptedOrder.getInputStream()));
        return takeOrder.readLine();
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

        // connection to the database
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

            // query to get each menu's orders
            String selectQuery = "SELECT * FROM ORDINI";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {

                // performs the select
                ResultSet resultSet = preparedStatement.executeQuery();

                menu.clear();

                // shows the menu
                while (resultSet.next()) {
                    String order = resultSet.getString("NOME");
                    float price = resultSet.getFloat("PREZZO");

                    menu.appendText(order + System.lineSeparator());
                    menu.appendText(price + System.lineSeparator());
                    menu.appendText("\n");
                }
            }
        } catch (SQLException exc) {
            throw new RuntimeException(exc);
        }
    }

    // hides the interface once the chef has finished to write the menu
    private void hideInterface() {
        menuOrderField.setVisible(false);
        menu.setVisible(false);
        orderPriceField.setVisible(false);
        stopWriteButton.setVisible(false);
        commitOrderButton.setVisible(false);
    }
}