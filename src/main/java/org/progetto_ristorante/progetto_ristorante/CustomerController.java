package org.progetto_ristorante.progetto_ristorante;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomerController {

    @FXML
    private Text billText,
            tableNumber,
            waitingMessage,
            loginError,
            registerError,
            unavailableReceptionist,
            unavailableWaiter;

    @FXML
    private ListView<String> totalOrderedArea;

    @FXML
    private ListView<Order> menu;

    @FXML
    private TextField loginUsername,
            registerUsername,
            requiredSeatsField;

    @FXML
    private PasswordField loginPassword,
            registerPassword,
            confirmPassword;

    @FXML
    private Button stopButton;

    @FXML
    private VBox seatsBox,
            waitingBox;

    private CustomerModell model;
    private BufferedReader getWaitingTime;
    private int waitingTime;
    private float bill = 0.0f;
    private int table;

    public CustomerController() {
        model = CustomerModell.getIstance();
    }

    @FXML
    private void login() throws SQLException, IOException, NoSuchAlgorithmException {
        String username = loginUsername.getText();
        String password = loginPassword.getText();
        if (model.loginUser(username, password)) {
            showSeatsInterface();
        } else {
            loginError.setText("Credenziali errate, riprovare");
            loginError.setVisible(true);
        }
    }

    @FXML
    private void register() throws SQLException, IOException, NoSuchAlgorithmException {
        String username = registerUsername.getText();
        String password = registerPassword.getText();
        String confirmedPassword = confirmPassword.getText();
        if (model.registerUser(username, password, confirmedPassword)) {
            showLoginInterface();
        }else{
            registerError.setText("Dati errati, controlla se la password contiente almeno 8 caratteri (lettera maiuscola, carattere speciale e numero) oppure l'username non è disponibile");
        }
    }



    // Method to handle the action when the customer requests seats
    @FXML
    private void getRequiredSeats() {
        try {
            final int RECEPTIONIST_PORT = 1313; // used to communicate with the receptionist

            // creates a socket to communicate with the receptionist
            Socket receptionSocket = new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT);
            unavailableReceptionist.setVisible(false);

            // says how many seats he needs to the receptionist and gets a table
            table = getTable(receptionSocket);

            // if there are available seats, the customer takes them
            if (table >= 0) {

                // shows second interface's elements
                showOrderInterface();
            } else {

                // otherwise, opens a second socket
                try (Socket receptionSocket2 = new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT)) {

                    // used to read the time to wait communicated by the receptionist
                    getWaitingTime = new BufferedReader(new InputStreamReader(receptionSocket2.getInputStream()));

                    // read the time to wait from the socket and parses it to integer
                    waitingTime = Integer.parseInt(getWaitingTime.readLine());
                    seatsBox.setVisible(false);
                    waitingBox.setVisible(true);

                    // sets the waiting time message
                    waitingMessage.setText("Non ci sono abbastanza posti disponibili, vuoi attendere " + waitingTime + " minuti ?");
                    waitingMessage.setVisible(true);
                }
            }
        } catch (IOException exc) {
            unavailableReceptionist.setText("Receptionist non disponibile al momento");
            unavailableReceptionist.setVisible(true);
            throw new RuntimeException(exc);
        }
    }

    // Method to handle the action when the customer clicks the "Wait" button
    @FXML
    private void waitButton() throws IOException {

        // creates a scheduler to plan the periodic execution of tasks
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // plans which task execute after a waiting time, and specifies the time unit
        ScheduledFuture<?> waitTask = scheduler.schedule(this::onWaitComplete, waitingTime, TimeUnit.SECONDS);

        // waits for the task to complete (the estimated wait time)
        try {
            waitTask.get();
        } catch (InterruptedException | ExecutionException exc) {
            throw new RuntimeException(exc);
        } finally {

            // deallocates used resources
            scheduler.shutdown();
            getWaitingTime.close();
        }
    }

    // Method to handle the completion of the waiting time
    private void onWaitComplete() {
        Platform.runLater(() -> {

            // after a certain period of time, hides the waiting components and sends the customer to the interface to take orders
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(1), event -> {
                        try {
                            showOrderInterface();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
            );
            timeline.play();
        });
    }

    // closes the interface when the customer clicks the "Leave" button
    @FXML
    private void leave() {
        Stage stage = (Stage) requiredSeatsField.getScene().getWindow();
        stage.close();
    }

    // Method to allow the customer to specify how many seats they need and get a table if available
    private int getTable(Socket receptionSocket) throws IOException {

        // used to get customer's required seats and to send it to the receptionist
        BufferedReader checkSeats = new BufferedReader(new InputStreamReader(receptionSocket.getInputStream()));
        PrintWriter sendSeats = new PrintWriter(receptionSocket.getOutputStream(), true);

        String input = requiredSeatsField.getText();
        requiredSeatsField.setText("");
        int requiredSeats = Integer.parseInt(input);

        // says how many seats he requires to the receptionist
        sendSeats.println(requiredSeats);

        // gets the table number from the receptionist if it's possible
        int tableNumber = Integer.parseInt(checkSeats.readLine());

        // closes used resources
        checkSeats.close();
        sendSeats.close();
        receptionSocket.close();
        return tableNumber;
    }

    // shows the menu in real time
    private void getMenu() {

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

                // applies a border to each menu's order
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
                                    setText(item.getName() + " - €" + item.getPrice());
                                    setStyle("-fx-border-color: #D2B48C; -fx-border-width: 1;");
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

    // simulates a customer's order
    @FXML
    private void getOrder(String order, float price) {
        final int WAITER_PORT = 1316;  // used to communicate with the waiter

        // creates a socket to communicate with the waiter
        try (Socket waiterSocket = new Socket(InetAddress.getLocalHost(), WAITER_PORT)) {

            // used to get a customer's order and to send it to a waiter
            BufferedReader eatOrder = new BufferedReader(new InputStreamReader(waiterSocket.getInputStream()));
            PrintWriter takeOrder = new PrintWriter(waiterSocket.getOutputStream(), true);

            // contains each customer's order
            StringBuilder totalOrdered = new StringBuilder();

            unavailableWaiter.setVisible(false);

            // sends the order to the waiter
            takeOrder.println(order);

            // waits for the order and eats it
            order = eatOrder.readLine();

            // adds the order to the customer's list and its price to the bill
            totalOrdered.append(order).append("\n");

            // applies a border to each customer's order
            totalOrderedArea.setCellFactory(new Callback<>() {
                @Override
                public ListCell<String> call(ListView<String> param) {
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setStyle(null);
                            } else {
                                setText(item);
                                setStyle("-fx-border-color: #D2B48C; -fx-border-width: 1;");
                            }
                        }
                    };
                }
            });

            // shows orders and total bill
            totalOrderedArea.getItems().add(totalOrdered.toString());
            bill += price;
            billText.setText(String.format("%.2f", bill) + "€");
        } catch (IOException exc) {
            unavailableWaiter.setText("Nessun cameriere disponibile al momento");
            unavailableWaiter.setVisible(true);
            throw new RuntimeException(exc);
        }
    }

    // switches the interface to the login
    @FXML
    private void showLoginInterface() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) registerUsername.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    // switches the interface to the register form
    @FXML
    private void showRegisterInterface() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("RegisterInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) loginUsername.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private void showSeatsInterface() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GetSeatsInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) loginUsername.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    // method to show the interface that allows users to get orders
    private void showOrderInterface() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GetOrderInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) seatsBox.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);

        // initializes interface's elements
        menu = (ListView<Order>) scene.lookup("#menu");
        totalOrderedArea = (ListView<String>) scene.lookup("#totalOrderedArea");
        tableNumber = (Text) scene.lookup("#tableNumber");
        tableNumber.setText(String.valueOf(table));
        billText = (Text) scene.lookup("#billText");
        billText.setText("0€");
        unavailableWaiter = (Text) scene.lookup("#unavailableWaiter");

        // shows the menu
        getMenu();

        // adds an event manager to get customer's order by clicking onto the menu
        menu.setOnMouseClicked(event -> {

            // gets customer's clicked order
            Order order = menu.getSelectionModel().getSelectedItem();

            // shows a window to get customers confirm
            Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationDialog.setTitle("Conferma ordine");
            confirmationDialog.setHeaderText(null);
            confirmationDialog.setGraphic(null);
            confirmationDialog.setContentText("Sei sicuro di voler ordinare " + order.getName() + " ?");

            // adds confirm and deny buttons
            confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

            // waits for customer's response
            confirmationDialog.showAndWait().ifPresent(response -> {

                // if the customer confirms, sends the order to the waiter
                if (response == ButtonType.OK) {
                    getOrder(order.getName(), order.getPrice());
                }
            });
        });
        stage.show();
    }

    // method to close customers' interface once they've done
    @FXML
    private void askBill() {

        // shows a window to get customers confirm
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Conferma richiesta conto");
        confirmationDialog.setGraphic(null);
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setContentText("Sei sicuro di voler chiedere il conto?");

        // adds confirm and deny buttons
        confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        // waits for customer's response
        confirmationDialog.showAndWait().ifPresent(response -> {

            // if customer confirms, closes the interface
            if (response == ButtonType.OK) {
                Stage stage = (Stage) stopButton.getScene().getWindow();
                stage.close();
            }
        });
    }

}