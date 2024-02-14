package org.progetto_ristorante.progetto_ristorante;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
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

    @FXML
    private Button  confirmOrderButton,
                    cancelButton,
                    confirmMenuButton;

    private final ChefModel chefModel = new ChefModel();
    private final OrderFactory orderFactory = new SimpleOrderFactory();
    private final MenuOriginator menuOriginator = new MenuOriginator(); // initial menu's state (before modifies)
    private final MenuMemento menuMemento = menuOriginator.saveMenuState(); // used to restore menu's previous state if chef undo modifies



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) { // shows current menu when the interface is loaded and sets the action to perform when the chef clicks on buttons
        setButtonShadow(); // sets buttons' shadow when chef hovers them with mouse
        showStoredMenu(); // shows the initial menu's state
        setMouseClickHandler(); // sets an event handler that catches chef's clicks on the interface
    }

    @FXML
    private void cook() {
        confirmMenu(); // stores menu's updates
        chefModel.startServer(); // starts chef's thread
    }

    @FXML
    private void addOrder() { // adds an order to current menu
        String orderName = menuOrderField.getText(); // gets order's name from the interface
        String inputPrice = orderPriceField.getText(); // gets order's price from the interface
        if (!orderName.isEmpty() && !inputPrice.isEmpty()) { // checks if the chef has entered not null values
            inputPrice = inputPrice.replace(',', '.'); // replaces "," with "."
            Product newOrder = orderFactory.createOrder(orderName, Float.parseFloat(inputPrice));
            if (menuOriginator.getMenu().stream().anyMatch(order -> order.name().equals(((Order) newOrder).name()))) { // checks if the order is already in the current menu
                invalidData.setText("Ordine già presente nel menu");
                invalidData.setVisible(true);
            } else {
                Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmationDialog.setTitle("Conferma aggiunta ordine");
                confirmationDialog.setHeaderText(null);
                confirmationDialog.setGraphic(null);
                confirmationDialog.setContentText(STR."Sei sicuro di voler aggiungere \{orderName} al menu?");
                confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
                confirmationDialog.showAndWait().ifPresent(result -> { // checks chef's answer
                    if (result == ButtonType.OK) { // if chef confirms
                        menuOriginator.getMenu().add((Order) newOrder); // adds the order into current menu
                        showCurrentMenu(); // shows current menu (with modifies)
                        menuOrderField.setText(""); // clears previous text
                        orderPriceField.setText("");
                    }
                });
            }
        } else {
            invalidData.setText("Ordine o prezzo mancante");
            invalidData.setVisible(true);
        }
    }

    private void deleteOrder() throws SQLException { // deletes an order from current menu
        Order selectedOrder = menu.getSelectionModel().getSelectedItem(); // gets the order clicked by chef into the interface
        menuOriginator.getMenu().removeIf(order -> order.name().equals(selectedOrder.name())); // checks if the order is into current menu
        showCurrentMenu(); // shows updated menu
    }

    @FXML
    private void showCurrentMenu() { // shows current menu in real time
        ObservableList<Order> menuItems = FXCollections.observableArrayList(menuOriginator.getMenu()); // list of orders
        applyMenuStyle(); // applies a border to the menu
        menu.setItems(menuItems); // makes the menu viewable as a list of Order elements (name-price)
    }

    @FXML
    private void showStoredMenu() { // shows the menu stored into the database (initial menu's state)
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) { // connection to the database
            String selectQuery = "SELECT * FROM ORDINI"; // query to get each menu's order
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) { // performs the select
                ResultSet resultSet = preparedStatement.executeQuery();
                ObservableList<Order> menuItems = FXCollections.observableArrayList(); // list of orders
                while (resultSet.next()) { // each order is added into the menu
                    String name = resultSet.getString("NOME");
                    float price = resultSet.getFloat("PREZZO");
                    Order order = new Order(name, price); // calls the constructor to create an Order object
                    menuItems.add(order);
                }
                menuOriginator.setMenu(menuItems); // sets menu's initial state
                applyMenuStyle(); // applies a border to the menu
                menu.setItems(menuItems); // makes the menu viewable as list of Order elements (name-price)
            }
        } catch (SQLException exc) {
            throw new RuntimeException(exc);
        }
    }

    @FXML
    private void confirmMenu() { // hides the interface once the chef has finished to write the menu
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION); // shows a window to get chef confirm
        confirmationDialog.setTitle("Conferma menu");
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setGraphic(null);
        confirmationDialog.setContentText("Sei sicuro di voler confermare il menu?");
        confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
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
    public void restoreMenu() { // undo menu's updates
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION); // shows a window to get chef confirm
        confirmationDialog.setTitle("Annulla modifiche");
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setGraphic(null);
        confirmationDialog.setContentText("Sei sicuro di voler annullare le modifiche al menu?");
        confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
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

    public void setButtonShadow() { // sets a shadow when chef hovers buttons with mouse
        cancelButton.setOnMouseEntered(_ -> cancelButton.setEffect(new DropShadow()));
        cancelButton.setOnMouseExited(_ -> cancelButton.setEffect(null));
        confirmMenuButton.setOnMouseEntered(_ -> confirmMenuButton.setEffect(new DropShadow()));
        confirmMenuButton.setOnMouseExited(_ -> confirmMenuButton.setEffect(null));
        confirmOrderButton.setOnMouseEntered(_ -> confirmOrderButton.setEffect(new DropShadow()));
        confirmOrderButton.setOnMouseExited(_ -> confirmOrderButton.setEffect(null));
    }

    public void setMouseClickHandler () { // sets an event handler that catches chef's clicks on the interface
        menu.setOnMouseClicked(_ -> { // adds an event manager to get the order the chef wants to remove from the menu
            Order order = menu.getSelectionModel().getSelectedItem(); // gets chef's clicked order
            Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION); // shows a window to get chef confirm
            confirmationDialog.setTitle("Conferma eliminazione ordine");
            confirmationDialog.setHeaderText(null);
            confirmationDialog.setGraphic(null);
            confirmationDialog.setContentText(STR."Sei sicuro di voler eliminare \{order.name()} dal menu?");
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

    public void applyMenuStyle() { // applies a border to each menu's order
        menu.setCellFactory(new Callback<>() {
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
                            Label priceLabel = new Label(STR."€\{String.format("%.2f", item.price())}");
                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);
                            hbox.getChildren().addAll(nameLabel, spacer, priceLabel);
                            setText(null);
                            setGraphic(hbox);
                            setStyle("-fx-border-color: #F5DEB3; -fx-padding: 10px; -fx-margin: 10px");
                        }
                    }
                };
            }
        });
    }
}