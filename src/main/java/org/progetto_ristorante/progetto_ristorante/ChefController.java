package org.progetto_ristorante.progetto_ristorante;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class ChefController implements Initializable {

    // FXML annotations for injecting UI elements
    @FXML
    private TextField menuOrderField,
            orderPriceField;

    @FXML
    private ListView<Order> menu;

    @FXML
    private Text invalidData;

    @FXML
    private VBox order,
                 menuArea;

    private final ChefModel chefModel = new ChefModel();
    private final OrderFactory orderFactory = new SimpleOrderFactory();
    private static  MenuObserverManager menuObserverManager; // instance to notify if the menu's been updated or not
    private boolean isMenuUpdated = false;
    private final MenuOriginator menuOriginator = new MenuOriginator(); // initial menu's state (before modifies)
    private final MenuMemento menuMemento = menuOriginator.saveMenuState(); // used to restore menu's previous state if chef undo modifies

    public static void setMenuObserverManager(MenuObserverManager manager) {
        ChefController.menuObserverManager = manager;
    }

    public void updateMenu() { // notifies if menu has been modified or not
        System.out.println(isMenuUpdated);
        if (isMenuUpdated) {
            System.out.println("Avviso menù aggiornato");
            menuObserverManager.notifyObserversMenuUpdate();
        } else {
            System.out.println("Avviso menù non aggiornato");
            menuObserverManager.notifyObserversMenuNotUpdated();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) { // shows current menu when the interface is loaded and sets the action to perform when the chef clicks on buttons
        showStoredMenu(); // shows the initial menu's state
        menu.setOnMouseClicked(event -> { // adds an event manager to get the order the chef wants to remove from the menu
            Order order = menu.getSelectionModel().getSelectedItem(); // gets chef's clicked order
            // shows a window to get chef confirm
            Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationDialog.setTitle("Conferma eliminazione ordine");
            confirmationDialog.setHeaderText(null);
            confirmationDialog.setGraphic(null);
            confirmationDialog.setContentText("Sei sicuro di voler eliminare " + order.name() + " dal menu?");
            confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL); // adds confirm and deny buttons
            confirmationDialog.showAndWait().ifPresent(response -> { // waits chef's response
                if (response == ButtonType.OK) { // if chef confirms
                    try {
                        deleteOrder(); // deletes the order from the menu
                    } catch (SQLException exc) {
                        throw new RuntimeException(exc);
                    }
                }
            });
        });
    }

    @FXML
    private void cook() {
        confirmMenu(); // stores menu's updates
        updateMenu(); // notifies if menu has been modified or not
        chefModel.startServer(); // starts chef's thread
    }

    @FXML
    private void addOrder() { // adds an order to current menu
        String orderName = menuOrderField.getText(); // gets order's name from the interface
        String inputPrice = orderPriceField.getText(); // gets order's price from the interface
        if (!orderName.isEmpty() && !inputPrice.isEmpty()) { // checks if the chef has entered not null values
            inputPrice = inputPrice.replace(',', '.'); // replaces "," with "."
            Order newOrder = orderFactory.createOrder(orderName, Float.parseFloat(inputPrice));
            if (menuOriginator.getMenu().stream().anyMatch(order -> order.name().equals(newOrder.name()))) { // checks if the order is already in the current menu
                invalidData.setText("Ordine già presente nel menu");
                invalidData.setVisible(true);
            } else {
                invalidData.setVisible(false);
                menuOriginator.getMenu().add(newOrder); // adds the order into current menu
                showCurrentMenu(); // shows current menu (with modifies)
                isMenuUpdated = true; // the menu is updated
                menuOrderField.setText("");
                orderPriceField.setText("");
            }
        } else {
            invalidData.setText("Ordine o prezzo mancante");
            invalidData.setVisible(true);
        }
    }

    private void deleteOrder() throws SQLException { // deletes an order from current menu
        Order selectedOrder = menu.getSelectionModel().getSelectedItem();
        menuOriginator.getMenu().removeIf(order -> order.name().equals(selectedOrder.name())); // checks if the order is into current menu
        isMenuUpdated = true; // the menu is updated
        showCurrentMenu(); // shows updated menu
    }

    @FXML
    private void showCurrentMenu() { // shows current menu in real time
        // makes the menu viewable as a list of Order elements (name-price)
        ObservableList<Order> menuItems = FXCollections.observableArrayList(menuOriginator.getMenu());
        menu.setCellFactory(new Callback<>() { // applies a border to each menu's order
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
                            HBox hbox = new HBox();
                            Label nameLabel = new Label(item.name());
                            Label priceLabel = new Label("€" + String.format("%.2f", item.price()));
                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);
                            hbox.getChildren().addAll(nameLabel, spacer, priceLabel);
                            setText(null);
                            setGraphic(hbox);
                            setStyle("-fx-border-color: #F5DEB3; -fx-padding: 5px;");
                        }
                    }
                };
            }
        });
        menu.setItems(menuItems);
    }

    @FXML
    private void showStoredMenu() { // shows the menu stored into the database (initial menu's state)
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) { // connection to the database
            String selectQuery = "SELECT * FROM ORDINI"; // query to get each menu's order
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) { // performs the select
                ResultSet resultSet = preparedStatement.executeQuery();
                // makes the menu viewable as list of Order elements (name-price)
                ObservableList<Order> menuItems = FXCollections.observableArrayList();
                while (resultSet.next()) { // each order is added into the menu
                    String name = resultSet.getString("NOME");
                    float price = resultSet.getFloat("PREZZO");
                    Order order = new Order(name, price);
                    menuItems.add(order);
                }
                menuOriginator.setMenu(menuItems); // sets menu's initial state
                menu.setCellFactory(new Callback<>() { // applies a border to each menu's order
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
                                    HBox hbox = new HBox();
                                    Label nameLabel = new Label(item.name());
                                    Label priceLabel = new Label("€" + String.format("%.2f", item.price()));
                                    Region spacer = new Region();
                                    HBox.setHgrow(spacer, Priority.ALWAYS);
                                    hbox.getChildren().addAll(nameLabel, spacer, priceLabel);
                                    setText(null);
                                    setGraphic(hbox);
                                    setStyle("-fx-border-color: #F5DEB3; -fx-padding: 5px;");
                                }
                            }
                        };
                    }
                });
                menu.setItems(menuItems);
            }
        } catch (SQLException exc) {
            throw new RuntimeException(exc);
        }
    }

    @FXML
    private void confirmMenu() { // hides the interface once the chef has finished to write the menu
        // shows a window to get chef confirm
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Conferma menu");
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setGraphic(null);
        confirmationDialog.setContentText("Sei sicuro di voler confermare il menu?");
        confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);  // adds confirm and deny buttons
        confirmationDialog.showAndWait().ifPresent(response -> { // waits chef's response
            if (response == ButtonType.OK) { // if chef confirms, confirms the menu and hides interface's elements
                saveConfirmedMenu(); // stores menu updates into the database
                menuArea.setVisible(false); // hides interface's elements
                order.setVisible(false);
                invalidData.setVisible(false);
            }
        });
    }

    @FXML
    public void restoreMenu() {
        // shows a window to get chef confirm
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Annullamento modifiche");
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setGraphic(null);
        confirmationDialog.setContentText("Sei sicuro di voler annullare le modifiche al menu?");
        confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);  // adds confirm and deny buttons
        confirmationDialog.showAndWait().ifPresent(response -> { // waits for chef's response
            if (response == ButtonType.OK) { // if chef confirms, confirms the menu and hides interface's elements
                menuMemento.restoreMenu(); // returns to previous state
                showStoredMenu();
            }
        });
    }

    private void saveConfirmedMenu() { // stores menu's updates
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) { // connection to the database
            String clearMenuQuery = "DELETE FROM ORDINI"; // clears existing menu in the database
            try (PreparedStatement clearMenuStatement = connection.prepareStatement(clearMenuQuery)) { // executes the query
                clearMenuStatement.executeUpdate();
            }
            String insertQuery = "INSERT INTO ORDINI (NOME, PREZZO) VALUES (?, ?)"; // inserts orders from currentMenu into the database
            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) { // executes the query
                for (Order order : menuOriginator.getMenu()) { // adds each current menu's order into the database
                    insertStatement.setString(1, order.name()); // substitutes "?" with order's name and order's price
                    insertStatement.setFloat(2, order.price());
                    insertStatement.executeUpdate(); // executes the query
                    menuOriginator.saveMenuState(); // saves menu's state
                }
            }
        } catch (SQLException exc) {
            throw new RuntimeException(exc);
        }
    }
}