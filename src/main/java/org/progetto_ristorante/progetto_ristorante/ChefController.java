package org.progetto_ristorante.progetto_ristorante;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class ChefController implements Initializable {

    // FXML annotations for injecting UI elements
    @FXML
    private TextField menuOrderField,
            orderPriceField;

    @FXML
    private ListView<Order> menuArea;

    @FXML
    private Text chefTitle,
                 invalidData;

    @FXML
    private HBox order,
                 orderButton;

    private final ChefModel chefModel = new ChefModel();

    // shows current menu when the interface is loaded and sets the action to perform when the customer clicks on buttons
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // shows the menu
        showMenu();

        // adds an event manager to get the order the chef wants to remove from the menu
        menuArea.setOnMouseClicked(event -> {

            // gets chef's clicked order
            Order order = menuArea.getSelectionModel().getSelectedItem();

            // shows a window to get chef confirm
            Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationDialog.setTitle("Conferma eliminazione ordine");
            confirmationDialog.setHeaderText(null);
            confirmationDialog.setGraphic(null);
            confirmationDialog.setContentText("Sei sicuro di voler eliminare " + order.getName() + " dal menu ?");

            // adds confirm and deny buttons
            confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

            // waits chef's response
            confirmationDialog.showAndWait().ifPresent(response -> {

                // if chef confirms, deletes the order from the menu and shows the updated menu
                if (response == ButtonType.OK) {
                    try {
                        deleteOrder(order.getName());
                        showMenu();
                    } catch (SQLException exc) {
                        throw new RuntimeException(exc);
                    }
                }
            });
        });
    }

    @FXML
    private void cook() {
        hideInterface();
        chefModel.startServer();
    }

    // adds an order into the menu
    @FXML
    private void addOrder() throws SQLException {

        // shows current menu
        showMenu();

        // reads order's name and price from the interface
        String order = menuOrderField.getText(),
                inputPrice = orderPriceField.getText();

        // checks if the chef has entered a valid order and a valid price
        if (!order.isEmpty() && !inputPrice.isEmpty()) {

            // replaces ',' with '.'
            inputPrice = inputPrice.replace(',', '.');

            // connection to the database
            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

                // query to check if the order is already into the menu
                String selectQuery = "SELECT * FROM ORDINI WHERE NOME = ?";
                try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

                    // substitutes ? with order's name
                    selectStatement.setString(1, order);

                    // performs the query
                    try (ResultSet resultSet = selectStatement.executeQuery()) {

                        // if the order is already into the menu, shows an error message
                        if (resultSet.next()) {
                            invalidData.setText("Ordine gia presente nel menu");
                            invalidData.setVisible(true);
                        } else {
                            invalidData.setVisible(false);

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

        // shows updated menu
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


    // shows the menu in real time
    private void showMenu() {

        // connection to the database
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

            // query to get each menu's order
            String selectQuery = "SELECT * FROM ORDINI";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {

                // performs the select
                ResultSet resultSet = preparedStatement.executeQuery();

                // makes the menu viewable as list of Order elements (name-price)
                ObservableList<Order> menuItems = FXCollections.observableArrayList();
                while (resultSet.next()) {
                    String name = resultSet.getString("NOME");
                    float price = resultSet.getFloat("PREZZO");
                    Order order = new Order(name, price);
                    menuItems.add(order);
                }

                // applies a border to each menu's order
                menuArea.setCellFactory(new Callback<>() {
                    @Override
                    public ListCell<Order> call(ListView<Order> param) {
                        return new ListCell<>() {
                            @Override
                            protected void updateItem(Order item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                    setText(null);
                                    setStyle(null);
                                } else {
                                    setText(item.getName() + " - €" + item.getPrice());
                                    setStyle("-fx-border-color: #D2B48C; -fx-border-width: 1;");
                                }
                            }
                        };
                    }
                });
                menuArea.setItems(menuItems);
            }
        } catch (SQLException exc) {
            throw new RuntimeException(exc);
        }
    }

    // hides the interface once the chef has finished to write the menu
    private void hideInterface() {
        chefTitle.setVisible(false);
        order.setVisible(false);
        menuArea.setVisible(false);
        orderButton.setVisible(false);
        invalidData.setVisible(false);
    }
}


