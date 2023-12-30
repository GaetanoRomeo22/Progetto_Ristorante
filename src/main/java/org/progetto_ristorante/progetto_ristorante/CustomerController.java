package org.progetto_ristorante.progetto_ristorante;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.*;

public class CustomerController {

    @FXML
    private TextArea totalOrderedArea,
                     menuArea;

    @FXML
    private Text billText,
                 loginError,
                 unavailableOrder;

    @FXML
    private TextField usernameField,
                      orderField,
                      requiredSeatsField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button stopButton;

    @FXML
    private void login() {

        // gets username and password from the interface
        String username = usernameField.getText(),
               password = passwordField.getText();

        // connection to the database
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

            // query to check if the user is registered
            String query = "SELECT * FROM UTENTI WHERE USERNAME = ? AND PASSWORD = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, password);


                // checks if the login works
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        showSeatsInterface();
                    } else {
                        loginError.setVisible(true);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (SQLException exc) {
            exc.printStackTrace();
        }
    }

    @FXML
    private void getRequiredSeats() {

        try {
            final int RECEPTIONIST_PORT = 1313;     // used to communicate with the receptionist

            // creates a socket to communicate with the receptionist
            Socket receptionSocket = new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT);
            int waitingTime;                   // time the customer has to wait to enter
            String answerWaitingTime;          // used to check if the user wants waiting

            // says how many seats he needs to the receptionist and gets a table
            int tableNumber = getTable(receptionSocket);

            // if there are available seats, the customer takes them
            if (tableNumber >= 0) {

                // gets the menù
                System.out.println("(Cliente) Prendo posto al tavolo " + tableNumber + " e scannerizzo il menù");

                // closes connection with receptionist
                receptionSocket.close();

                // shows second interface's elements
                showOrderInterface();

                // shows the menu
                getMenu();

                // orders, waits for the order and eats it
                getOrder();
            }

            // otherwise, he waits
            /*
            else {
                try (Socket receptionSocket2 = new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT)) {

                    // used to read through the socket
                    BufferedReader checkSeats2 = new BufferedReader(new InputStreamReader(receptionSocket2.getInputStream()));

                    // used to read through the standard input
                    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

                    // decides if waiting or not
                    waitingTime = Integer.parseInt(checkSeats2.readLine());

                    System.out.println("(Reception) Vuoi attendere " + waitingTime + " minuti ?");
                    answerWaitingTime = stdin.readLine();

                    if (answerWaitingTime.equalsIgnoreCase("si")) {

                        // creates a scheduler to plan the periodic execution of tasks
                        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

                        // plans which task execute after a waiting time, and specifies the time unity
                        ScheduledFuture<?> waitTask = scheduler.schedule(this::onWaitComplete, waitingTime, TimeUnit.SECONDS);

                        // waits for the task to complete (the estimated wait time)
                        try {
                            waitTask.get();
                        } catch (InterruptedException | ExecutionException exc) {
                            System.out.println("(Cliente) Errore utilizzo scheduler");
                            throw new RuntimeException(exc);
                        } finally {

                            // deallocates used resources
                            scheduler.shutdown();
                            checkSeats2.close();
                            stdin.close();
                            stdin.close();
                        }
                    } else {
                        System.out.println("(Cliente) Me ne vado!");
                    }
                }
            } */
        } catch (IOException exc) {
            System.out.println("(Cliente) Impossibile comunicare con il receptionist");
            throw new RuntimeException(exc);
        }
    }

    // allows customer to say how many seats he needs and to get a table if there are one available and there are enough seats
    private int getTable(Socket receptionSocket) throws IOException {

        // used to gets customer's required seats and to say it to the receptionist
        BufferedReader checkSeats = new BufferedReader(new InputStreamReader(receptionSocket.getInputStream()));
        PrintWriter sendSeats = new PrintWriter(receptionSocket.getOutputStream(), true);

        String input = requiredSeatsField.getText();
        int requiredSeats = Integer.parseInt(input);

        // says how many seats he requires to the receptionist
        sendSeats.println(requiredSeats);

        // gets the table number by the receptionist if it's possible
        int tableNumber = Integer.parseInt(checkSeats.readLine());

        // closes used resources
        checkSeats.close();
        sendSeats.close();
        receptionSocket.close();

        System.out.println(requiredSeats);
        return tableNumber;
    }

    // simulates menu's scanning by the customer and shows it
    private void getMenu() {

        // opens the files that contains the menu in read mode
        try (FileReader fileReader = new FileReader("menu.txt")) {

            // used to get each order and its price
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            // menu contains each order and its price
            StringBuilder menu = new StringBuilder();
            String order;
            float price;

            // shows each menu order and its price on the screen
            while ((order = bufferedReader.readLine()) != null) {
                price = Float.parseFloat(bufferedReader.readLine());
                menu.append("Ordine: ").append(order).append("\n");
                menu.append("Prezzo: ").append(price).append("\n");
            }

            // shows the menu into the interface's text area
            menuArea.setText(menu.toString());

            // closes the connection to the file
            bufferedReader.close();
        } catch (Exception exc) {
            System.out.println("(Cliente) Errore scannerizzazione menù");
            throw new RuntimeException(exc);
        }
    }

    // checks if customer's requested order is in the menù and returns true if the order is available and false otherwise
    private float checkOrder(String order) {

        // opens the file that contains the menu in read mode
        try (FileReader fileReader = new FileReader("menu.txt")) {

            // used to read an order from the file
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String menuOrder;
            float price;

            // reads each order stored into the menu while it finds the customer's requested one or until it realizes that it isn't available
            while ((menuOrder = bufferedReader.readLine()) != null){
                price = Float.parseFloat(bufferedReader.readLine());
                if (menuOrder.equals(order)) {
                    return price;
                }
            }

            // closes the connection to the file
            bufferedReader.close();
            return -1;
        } catch (Exception exc) {
            System.out.println("(Cliente) Errore apertura menù");
            throw new RuntimeException(exc);
        }
    }

    // simulates a customer order
    @FXML
    private void getOrder() {
        final int WAITER_PORT = 1316;           // used to communicate with the waiter

        try {
            // creates a socket to communicate with the waiter
            Socket waiterSocket = new Socket(InetAddress.getLocalHost(), WAITER_PORT);

            // used to get a customer's order and to send it to a waiter
            BufferedReader eatOrder = new BufferedReader(new InputStreamReader(waiterSocket.getInputStream()));
            PrintWriter takeOrder = new PrintWriter(waiterSocket.getOutputStream(), true);

            StringBuilder totalOrdered = new StringBuilder();
            String order;
            float bill = 0.0f;

            // gets customer's order
            order = orderField.getText();

            // if the customer stops eating
            if (order.equalsIgnoreCase("fine")) {
                takeOrder.println("fine");

                // closes the interface and stop the execution
                Stage stage = (Stage) orderField.getScene().getWindow();
                stage.close();
            } else if (!order.isEmpty()) { // Check if the order is not empty before checking its availability

                // if the requested order isn't in the menù, shows an error message
                if (checkOrder(order) < 0.50f) {
                    unavailableOrder.setVisible(true);
                } else {

                    // sends the order to the waiter
                    unavailableOrder.setVisible(false);
                    System.out.println("(Cliente) Attendo che " + order + " sia pronto");
                    takeOrder.println(order);

                    // waits for the order and eats it
                    order = eatOrder.readLine();

                    // adds the order to the customer's list and its price to the bill
                    totalOrdered.append(order).append("\n");
                    bill += checkOrder(order);

                    // eats the order
                    System.out.println("(Cliente) Mangio " + order);
                }
            }

            // shows orders and total bill
            totalOrderedArea.appendText(totalOrdered + "\n");
            billText.setText("CONTO: " + String.format("%.2f", bill) + "€");
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    private void showSeatsInterface() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GetSeatsInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000));
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
        stage.show();
    }

    private void showOrderInterface() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GetOrderInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) requiredSeatsField.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000));
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
        stage.show();
    }

    // closes customer's interface once he has done
    @FXML
    private void closeInterface() {
        Stage stage = (Stage) stopButton.getScene().getWindow();
        stage.close();
    }
}