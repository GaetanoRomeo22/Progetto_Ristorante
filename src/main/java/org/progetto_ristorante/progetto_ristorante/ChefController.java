package org.progetto_ristorante.progetto_ristorante;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class ChefController implements Initializable {
    @FXML
    private TextField menuOrderField,
            orderPriceField;

    @FXML
    private ComboBox<String> orderCategory;

    @FXML
    private ListView<ConcreteOrder> menu;

    @FXML
    private Text invalidData;

    @FXML
    private VBox order,
                 menuArea;

    @FXML
    private Button  confirmOrderButton,
                    cancelButton,
                    confirmMenuButton;

    private final ChefModel chefModel = new ChefModel(); // reference to Model
    private final OrderFactory orderFactory = new SimpleOrderFactory(); // used to create an order with name and price
    private final MenuOriginator menuOriginator = new MenuOriginator(); // initial menu's state (before modifies)
    private final MenuMemento menuMemento = menuOriginator.saveMenuState(); // used to restore menu's previous state if chef undo modifies

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) { // shows current menu when the interface is loaded and sets the action to perform when the chef clicks on buttons
        if (menuOrderField != null) { // if is loaded the first interface
            orderCategory.setValue("Antipasti");  // sets the default order category
            setButtonShadow(); // sets buttons' shadow when chef hovers them with mouse
            showStoredMenu(); // shows the initial menu's state
            menuOrderField.setOnMouseEntered(_ -> menuOrderField.setEffect(new DropShadow()));
            menuOrderField.setOnMouseExited(_ -> menuOrderField.setEffect(null));
            orderPriceField.setOnMouseEntered(_ -> orderPriceField.setEffect(new DropShadow()));
            orderPriceField.setOnMouseExited(_ -> orderPriceField.setEffect(null));
        }
    }

    @FXML
    private void cook() { // starts chef's thread to starts cooking customers' orders
        confirmMenu(); // stores menu's updates
        chefModel.startServer(); // starts chef's thread
    }

    @FXML
    private void addOrderToMenu() { // adds an order to current menu
        invalidData.setVisible(false); // hides error message
        String orderName = menuOrderField.getText().trim(); // gets order's name from the interface
        String inputPrice = orderPriceField.getText().trim(); // gets order's price from the interface
        String category = orderCategory.getValue().trim();  // gets order's category from the interface
        float price;
        if (!orderName.isEmpty() && !inputPrice.isEmpty()) { // checks if the chef has entered not null values
            try {
                price = Float.parseFloat(inputPrice.replace(',', '.')); // replaces "," with "."
                if (price <= 0) { // checks if orderPrice is non-negative
                    invalidData.setText("Prezzo non valido");
                    invalidData.setVisible(true);
                    return;
                }
            } catch (NumberFormatException exc) { // checks if chef has entered a correct price
                invalidData.setText("Prezzo non valido");
                invalidData.setVisible(true);
                return;
            }
            if (!orderName.matches("[a-zA-Z ]+")) { // checks if order's name contains only letters
                invalidData.setText("Il nome dell'ordine non può contenere numeri o caratteri speciali");
                invalidData.setVisible(true);
                return;
            }
            ConcreteOrder newOrder = orderFactory.createOrder(orderName, price, category); // creates an order with entered name and price
            if (menuOriginator.getMenu().stream().anyMatch(order -> order.name().equals(newOrder.name()))) { // checks if the order is already in the current menu
                invalidData.setText("Ordine già presente nel menu");
                invalidData.setVisible(true);
            } else {
                Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION); // shows a confirmation dialog to check if chef confirms to add the order into the menu
                confirmationDialog.setTitle("Conferma aggiunta ordine");
                confirmationDialog.setHeaderText(null);
                confirmationDialog.setGraphic(null);
                confirmationDialog.setContentText(STR."Sei sicuro di voler aggiungere \{orderName} al menu?");
                confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
                confirmationDialog.showAndWait().ifPresent(result -> { // checks chef's answer
                    if (result == ButtonType.OK) { // if chef confirms
                        menuOriginator.getMenu().add(newOrder); // adds the order into current menu
                        showCurrentMenu(); // shows current menu (with modifies)
                        menuOrderField.clear(); // clears previous text
                        orderPriceField.clear();
                        orderCategory.setValue("Antipasti");
                    }
                });
            }
        } else {
            invalidData.setText("Ordine o prezzo mancante");
            invalidData.setVisible(true);
        }
    }

    @FXML
    private void modifyMenuOrder(ConcreteOrder order) { // allows the chef to modify an order
        menuOrderField.setText(order.name()); // sets the previous order's info
        orderPriceField.setText(String.format("%.2f", order.price()));
        orderCategory.setValue(order.category());
        confirmOrderButton.setOnAction(_ -> updateOrder(order));
    }

    private void updateOrder(ConcreteOrder order) {
        String newName = menuOrderField.getText().trim(); // new order's info
        String newPriceText = orderPriceField.getText().trim();
        String newCategory = orderCategory.getValue().trim();
        float newPrice;
        try {
            newPrice = Float.parseFloat(newPriceText.replace(',', '.'));
            if (newPrice <= 0 || newName.isEmpty()) {
                invalidData.setText("Dati non validi");
                invalidData.setVisible(true);
            }
            menuOriginator.getMenu().remove(order); // remove the old order
            ConcreteOrder updatedOrder = new ConcreteOrder(newName, newPrice, newCategory);
            menuOriginator.getMenu().add(updatedOrder); // adds the new order
            showCurrentMenu(); // show the menu post modify
            confirmOrderButton.setOnAction(_ -> addOrderToMenu()); // resets button's on action
            menuOrderField.clear(); // clears previous input
            orderPriceField.clear();
        } catch (NumberFormatException exc) {
            invalidData.setText("Prezzo non valido");
            invalidData.setVisible(true);
        }
    }

    @FXML
    private void showCurrentMenu() { // shows current menu in real time
        ObservableList<ConcreteOrder> menuItems = FXCollections.observableArrayList(); // list of orders
        menuOriginator.getMenu().stream().filter(order -> order.category().equals("Antipasti")).forEach(menuItems::add); // filters each category's order
        menuOriginator.getMenu().stream().filter(order -> order.category().equals("Primi")).forEach(menuItems::add);
        menuOriginator.getMenu().stream().filter(order -> order.category().equals("Secondi")).forEach(menuItems::add);
        menuOriginator.getMenu().stream().filter(order -> order.category().equals("Dolci")).forEach(menuItems::add);
        applyMenuStyle(); // applies a border to the menu
        menu.setItems(menuItems); // makes the menu viewable as a list of Order elements (name-price)
    }

    @FXML
    private void showStoredMenu() { // shows the menu stored into the database (initial menu's state)
        invalidData.setVisible(false); // hides error message
        menuOrderField.clear(); // clears previous text
        orderPriceField.clear();
        orderCategory.setValue("Antipasti");
        ObservableList<ConcreteOrder> menuItems = FXCollections.observableArrayList(); // list of orders
        ConcreteOrder antipastiPlaceholder = new ConcreteOrder("Antipasti", 0.0f, "Antipasti"); // placeholders for order's categories
        ConcreteOrder primiPlaceholder = new ConcreteOrder("Primi", 0.0f, "Primi");
        ConcreteOrder secondiPlaceholder = new ConcreteOrder("Secondi", 0.0f, "Secondi");
        ConcreteOrder dolciPlaceholder = new ConcreteOrder("Dolci", 0.0f, "Dolci");
        menuItems.add(antipastiPlaceholder); // adds placeholders' visualization
        menuItems.add(primiPlaceholder);
        menuItems.add(secondiPlaceholder);
        menuItems.add(dolciPlaceholder);
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano_22")) { // connection to the database
            String selectQuery = "SELECT * FROM ORDINI ORDER BY CATEGORIA"; // query to get each menu's order
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) { // performs the select
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) { // each order is added into the menu
                    String name = resultSet.getString("NOME"); // gets order's name
                    float price = resultSet.getFloat("PREZZO"); // gets order's price
                    String category = resultSet.getString("CATEGORIA"); // gets order's category
                    ConcreteOrder order = new ConcreteOrder(name, price, category); // calls the constructor to create an Order object
                    switch (category) { // adds the order under the correct category placeholder
                        case "Antipasti" -> menuItems.add(menuItems.indexOf(antipastiPlaceholder) + 1, order);
                        case "Primi" -> menuItems.add(menuItems.indexOf(primiPlaceholder) + 1, order);
                        case "Secondi" -> menuItems.add(menuItems.indexOf(secondiPlaceholder) + 1, order);
                        case "Dolci" -> menuItems.add(menuItems.indexOf(dolciPlaceholder) + 1, order);
                    }
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
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION); // shows a confirmation dialog to check if chef confirms the menu
        confirmationDialog.setTitle("Conferma menu");
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setGraphic(null);
        confirmationDialog.setContentText("Sei sicuro di voler confermare il menu?");
        confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
        confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);  // adds confirm and deny buttons
        confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
        confirmationDialog.showAndWait().ifPresent(response -> { // waits chef's response
            if (response == ButtonType.OK) { // if chef confirms, confirms the menu and hides interface's elements
                try {
                    saveConfirmedMenu(); // stores menu updates into the database
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                menuArea.setVisible(false); // hides interface's elements
                order.setVisible(false);
                invalidData.setVisible(false);
            }
        });
    }

    private void saveConfirmedMenu() throws IOException { // stores menu's updates
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano_22")) { // connection to the database
            String clearMenuQuery = "DELETE FROM ORDINI"; // clears existing menu in the database
            try (PreparedStatement clearMenuStatement = connection.prepareStatement(clearMenuQuery)) { // executes the query
                clearMenuStatement.executeUpdate();
            }
            String insertQuery = "INSERT INTO ORDINI (NOME, PREZZO, CATEGORIA) VALUES (?, ?, ?)"; // inserts orders from currentMenu into the database
            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) { // executes the query
                for (ConcreteOrder order : menuOriginator.getMenu()) { // adds each current menu's order into the database
                    if (order.price() != 0.0f) {
                        insertStatement.setString(1, order.name()); // substitutes "?" with order's name and order's price
                        insertStatement.setFloat(2, order.price());
                        insertStatement.setString(3, order.category());
                        insertStatement.executeUpdate(); // executes the query
                    }
                    menuOriginator.saveMenuState(); // saves menu's state
                }
            }
        } catch (SQLException exc) {
            throw new RuntimeException(exc);
        }
    }

    @FXML
    private void restoreMenu() { // undo menu's updates
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION); // shows a confirmation dialog to check if chef confirms to undo menu's modifies
        confirmationDialog.setTitle("Annulla modifiche");
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setGraphic(null);
        confirmationDialog.setContentText("Sei sicuro di voler annullare le modifiche al menù?");
        confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
        confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL); // adds confirm and deny buttons
        confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
        confirmationDialog.showAndWait().ifPresent(response -> { // waits for chef's response
            if (response == ButtonType.OK) { // if chef confirms, confirms the menu and hides interface's elements
                menuMemento.restoreMenu(); // returns to previous state
                showStoredMenu(); // shows initial's menu state
            }
        });
    }

    private void deleteOrderFromMenu(ConcreteOrder order) { // deletes an order from current menu
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Conferma Rimozione");
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setGraphic(null);
        confirmationDialog.setContentText("Sei sicuro di voler rimuovere " + order.name() + " dal menù?");
        confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
        confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL); // adds confirm and deny buttons
        confirmationDialog.initOwner(menuOrderField.getScene().getWindow());
        confirmationDialog.showAndWait().ifPresent(response -> { // waits for chef's response
            if (response == ButtonType.OK) { // if chef confirms, confirms the menu and hides interface's elements
                menuOriginator.getMenu().remove(order);
                showCurrentMenu(); // shows initial's menu state
            }
        });
    }

    private void setButtonShadow() { // sets a shadow effect when chef hovers buttons with mouse
        cancelButton.setOnMouseEntered(_ -> cancelButton.setEffect(new DropShadow()));
        cancelButton.setOnMouseExited(_ -> cancelButton.setEffect(null));
        confirmMenuButton.setOnMouseEntered(_ -> confirmMenuButton.setEffect(new DropShadow()));
        confirmMenuButton.setOnMouseExited(_ -> confirmMenuButton.setEffect(null));
        confirmOrderButton.setOnMouseEntered(_ -> confirmOrderButton.setEffect(new DropShadow()));
        confirmOrderButton.setOnMouseExited(_ -> confirmOrderButton.setEffect(null));
    }

    private void applyMenuStyle() { // applies menu's style
        menu.setCellFactory(new Callback<>() {
            @Override
            public ListCell<ConcreteOrder> call(ListView<ConcreteOrder> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(ConcreteOrder item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle(null);
                            setGraphic(null);
                            setOnMouseClicked(null);
                        } else if (item.price() == 0.0f) {
                            setText(item.name());
                            setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 10px; -fx-background-color: #D3D3D3;");
                            setGraphic(null);
                            setDisable(true);
                        } else {
                            HBox hbox = new HBox(10);
                            hbox.setAlignment(Pos.CENTER_LEFT);
                            Label nameLabel = new Label(item.name());
                            Label priceLabel = new Label(String.format("€%.2f", item.price()));
                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);
                            Label pencilIcon = new Label("✎");
                            pencilIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #000000; -fx-padding: 0 10px;");
                            pencilIcon.setCursor(Cursor.HAND);
                            pencilIcon.setOnMouseClicked(_ -> modifyMenuOrder(item));
                            pencilIcon.setOnMouseEntered(_ -> pencilIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #0000FF; -fx-padding: 0 10px;"));
                            pencilIcon.setOnMouseExited(_ -> pencilIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #000000; -fx-padding: 0 10px;"));
                            Label removeIcon = new Label("❌");
                            removeIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #FF0000; -fx-padding: 0 10px;");
                            removeIcon.setCursor(Cursor.HAND);
                            removeIcon.setOnMouseClicked(_ -> deleteOrderFromMenu(item));
                            removeIcon.setOnMouseEntered(_ -> removeIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #FF0000; -fx-padding: 0 10px; -fx-background-color: #FFE4E1"));
                            removeIcon.setOnMouseExited(_ -> removeIcon.setStyle("-fx-font-size: 22px; -fx-text-fill: #FF0000; -fx-padding: 0 10px;"));
                            hbox.getChildren().addAll(nameLabel, spacer, priceLabel, pencilIcon, removeIcon);
                            setText(null);
                            setGraphic(hbox);
                            setOnMouseClicked(null);
                            setStyle("-fx-border-color: #F5DEB3; -fx-padding: 10px; -fx-margin: 10px");
                            setOnMouseEntered(_ -> setStyle("-fx-border-color: #F5DEB3; -fx-padding: 10px; -fx-margin: 10px; -fx-background-color: #F0E3A2"));
                            setOnMouseExited(_ -> setStyle("-fx-border-color: #F5DEB3; -fx-padding: 10px; -fx-margin: 10px"));
                        }
                    }
                };
            }
        });
        menu.setSelectionModel(null);
    }
}