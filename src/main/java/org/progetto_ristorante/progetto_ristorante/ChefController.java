package org.progetto_ristorante.progetto_ristorante;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class ChefController implements Initializable {

    @FXML
    private TextField menuOrderField,
            orderPriceField;

    @FXML
    private ListView<Order> menuArea;

    @FXML
    private Text invalidData;

    @FXML
    private HBox order,
                 orderButton;

    // shows current menu when the interface is loaded
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // shows the menu
        showMenu();

        // adds an event manager to get customer's order by clicking onto the menu
        menuArea.setOnMouseClicked(event -> {

            // gets chef's clicked order
            Order order = menuArea.getSelectionModel().getSelectedItem();

            // shows a window to get customers confirm
            Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationDialog.setTitle("Conferma eliminazione ordine");
            confirmationDialog.setHeaderText(null);
            confirmationDialog.setGraphic(null);
            confirmationDialog.setContentText("Sei sicuro di voler eliminare " + order.getName() + " dal menu ?");

            // adds confirm and deny buttons
            confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

            // waits for customer's response
            confirmationDialog.showAndWait().ifPresent(response -> {

                // if the chef confirms, deletes the order from the menu and shows updated menu
                if (response == ButtonType.OK) {
                    try {
                        deleteOrder(order.getName());
                        showMenu();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });
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

        // shows current menu
        showMenu();

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

        // shows upgraded menu
        showMenu();
    }

    // deletes an order from the menu
    private void deleteOrder(String name) throws SQLException {

        // connection to the database
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

            // query to delete the order from the database
            String deleteQuery = "DELETE FROM ORDINI WHERE NOME = ?";
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {

                // substitutes ? with order's name
                deleteStatement.setString(1, name);

                // executes the query
                deleteStatement.executeUpdate();
            }
        }
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

                // makes the menu viewable
                ObservableList<Order> menuItems = FXCollections.observableArrayList();
                while (resultSet.next()) {
                    String name = resultSet.getString("NOME");
                    float price = resultSet.getFloat("PREZZO");
                    Order order = new Order(name, price);
                    menuItems.add(order);
                }
                menuArea.setItems(menuItems);
            }
        } catch (SQLException exc) {
            throw new RuntimeException(exc);
        }
    }

    // hides the interface once the chef has finished to write the menu
    private void hideInterface() {
        order.setVisible(false);
        menuArea.setVisible(false);
        orderButton.setVisible(false);
        invalidData.setVisible(false);
    }
}


