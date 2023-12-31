package org.progetto_ristorante.progetto_ristorante;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
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

public class CustomerController {

    // FXML annotations for injecting UI elements
    @FXML
    private TextArea totalOrderedArea,
            menu;

    @FXML
    private Text billText,
            tableNumber,
            waitingMessage,
            loginError,
            registerError,
            unavailableReceptionist,
            unavailableWaiter;

    @FXML
    private TextField loginUsername,
            registerUsername,
            requiredSeatsField;

    @FXML
    private Button stopButton;

    @FXML
    private VBox seatsBox,
            waitingBox;

    // variables to manage communication with the receptionist
    private BufferedReader checkSeats2;
    private int waitingTime; // time the customer has to wait to enter

    @FXML
    private PasswordField loginPassword,
            registerPassword;

    // customer's bill
    protected float bill = 0.0f;

    // customer's table's number
    protected int table;

    // allows a customer to login himself by entering a username and a password
    @FXML
    private void login() throws SQLException, IOException, NoSuchAlgorithmException {

        // gets username and password from the interface
        String username = loginUsername.getText(),
               password = loginPassword.getText();

        // checks if the user has entered valid username and password
        if (username.isEmpty() || password.isEmpty()) {
            loginError.setText("Username o password mancante");
            loginError.setVisible(true);
        } else {

            // encrypts the password with a hash algorithm
            String hashedPassword = hashPassword(password);

            // connection to the database
            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

                // query to check if the user is registered
                String query = "SELECT * FROM UTENTI WHERE USERNAME = ? AND PASSWORD = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                    // substitutes ? with username and password
                    preparedStatement.setString(1, username);
                    preparedStatement.setString(2, hashedPassword);

                    // checks if the login works
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            showSeatsInterface();
                        } else {
                            loginError.setText("Credenziali errate, riprovare");
                            loginError.setVisible(true);
                        }
                    }
                }
            }
        }
    }

    // allows a customer to register himself by entering a username and a password
    @FXML
    private void register() throws SQLException, IOException, NoSuchAlgorithmException {

        // gets username and password from the interface
        String username = registerUsername.getText();
        String password = registerPassword.getText();

        // checks if the user has entered valid username and password
        if (username.isEmpty() || password.isEmpty()) {
            registerError.setText("Username o password mancante");
            registerError.setVisible(true);
        } else {

            // encrypts the password with a hash algorithm
            String hashedPassword = hashPassword(password);

            // connection to the database
            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

                // check if the username already exists
                if (usernameAvailable(connection, username)) {

                    // query to insert a new uses into the database
                    String query = "INSERT IGNORE INTO UTENTI (USERNAME, PASSWORD) VALUES (?, ?)";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                        // substitutes ? with username and password
                        preparedStatement.setString(1, username);
                        preparedStatement.setString(2, hashedPassword);

                        // performs the insert
                        preparedStatement.executeUpdate();
                        showLoginInterface();
                    }
                }
            }
        }
    }

    // checks if the username is available (is not used by another customer)
    private boolean usernameAvailable (Connection connection, String username) throws SQLException {

        // query to count how many users have the same username
        String query = "SELECT COUNT(*) FROM UTENTI WHERE USERNAME = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // substitutes ? with the username
            preparedStatement.setString(1, username);

            // checks if there is at least a user that has the same username
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {

                    // if the username is already used, shows the error message, otherwise sends the user to the login
                    int count = resultSet.getInt(1);
                    if (count == 0) {
                        return true;
                    }
                }
            }
        }
        registerError.setText("Username gia' utilizzato");
        registerError.setVisible(true);
        return false;
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

                // closes connection with the receptionist
                receptionSocket.close();

                // shows second interface's elements
                showOrderInterface();

                // shows the menu
                getMenu();
            } else {
                // otherwise, he waits
                try (Socket receptionSocket2 = new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT)) {
                    // used to read through the socket
                    checkSeats2 = new BufferedReader(new InputStreamReader(receptionSocket2.getInputStream()));

                    // decides if waiting or not
                    waitingTime = Integer.parseInt(checkSeats2.readLine());
                    seatsBox.setVisible(false);
                    waitingBox.setVisible(true);

                    // Set the waiting time message
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
            checkSeats2.close();
        }
    }

    // Method to handle the completion of the waiting time
    private void onWaitComplete() {
        Platform.runLater(() -> {

            // After a certain period of time, hide the waiting components and reload the first interface
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

    // method to handle the action when the customer clicks the "Leave" button
    @FXML
    private void leaveButton() {

        // closes the interface
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

    // simulates a customer's order
    @FXML
    private void getOrder(String order, float price) {
        final int WAITER_PORT = 1316; // used to communicate with the waiter

        try {
            // creates a socket to communicate with the waiter
            Socket waiterSocket = new Socket(InetAddress.getLocalHost(), WAITER_PORT);

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

            // shows orders and total bill
            totalOrderedArea.appendText(totalOrdered + "\n");
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

        menu = (TextArea) scene.lookup("#menu");
        totalOrderedArea = (TextArea) scene.lookup("#totalOrderedArea");
        tableNumber = (Text) scene.lookup("#tableNumber");
        tableNumber.setText(String.valueOf(table));
        billText = (Text) scene.lookup("#billText");
        billText.setText("0€");
        unavailableWaiter = (Text) scene.lookup("#unavailableWaiter");

        // Aggiungi un gestore di eventi per catturare i clic sulla TextArea del menu
        menu.setOnMouseClicked(event -> {
            String order = menu.getSelectedText();
            if (order != null && !order.isEmpty()) {

                // connection to the database
                try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

                    // query to get order's price
                    String selectQuery = "SELECT PREZZO FROM ORDINI WHERE NOME = ?";
                    try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

                        // substitutes ? with order's name
                        selectStatement.setString(1, order);

                        // gets order's price and calls getOrder to allow the customer to order
                        try (ResultSet resultSet = selectStatement.executeQuery()) {
                            if (resultSet.next()) {
                                float price = resultSet.getFloat("PREZZO");
                                getOrder(order, price);
                            }
                        }
                    }
                } catch (SQLException exc) {
                    throw new RuntimeException(exc);
                }
            }
        });
        stage.show();
    }

    // Method to close the customer's interface once they are done
    @FXML
    private void closeInterface() {
        Stage stage = (Stage) stopButton.getScene().getWindow();
        stage.close();
    }

    // encrypts the password using a hash algorithm
    private String hashPassword(String password) throws NoSuchAlgorithmException {

        // gets an instance of Message Digest (Java package that establishes hash functionalities)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // calculates the array of byte that contains the hashed password
        byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));

        // converts the array of byte into hexadecimal
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : hashedBytes) {
            stringBuilder.append(String.format("%02x", b));
        }

        // returns it as string
        return stringBuilder.toString();
    }
}