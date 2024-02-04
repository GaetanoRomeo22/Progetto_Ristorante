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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

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

    private final CustomerModel model;
    private BufferedReader getWaitingTime; // used to get how much time has the customer to wait if there aren't available seats
    private int waitingTime;               // time the customer has to wait if there aren't available seats
    private float bill = 0.0f;             // customer's total bill
    private int table;                     // customer's table's number

    public CustomerController() { // constructor
        model = CustomerModel.getInstance();
    }

    @FXML
    private void login() throws SQLException, NoSuchAlgorithmException, IOException { // manages customer's login
        String username = loginUsername.getText(); // gets username from the interface
        String password = loginPassword.getText(); // gets password from the interface
        if (username.isEmpty() || password.isEmpty()) { // checks if the customer has entered null values
            loginError.setText("Credenziali incomplete");
            loginError.setVisible(true);
        } else if (model.loginUser(username, password)) { // if the login works, sends the customer to next interface
            showSeatsInterface();
        } else { // if the login doesn't work, shows an error message
            loginError.setText("Credenziali errate, riprovare");
            loginError.setVisible(true);
        }
    }

    @FXML
    private void register() throws SQLException, IOException, NoSuchAlgorithmException { // manages customer's registration
        String username = registerUsername.getText(); // gets username from the interface
        String password = registerPassword.getText(); // gets password from the interface
        String confirmedPassword = confirmPassword.getText(); // gets confirmed password from the interface
        if (username.isEmpty() || password.isEmpty() || confirmedPassword.isEmpty()) { // checks if the customer has entered null values
            registerError.setText("Credenziali incomplete");
            registerError.setVisible(true);
        } else if (!validPassword(password)) { // checks if the password doesn't respect the standard
            registerError.setText("La password deve contenere almeno 8 caratteri, una lettera maiuscola, un carattere speciale e un numero");
            registerError.setVisible(true);
        } else if (!confirmedPassword.equals(password)) { // checks if the password isn't correctly confirmed
            registerError.setText("Conferma password errata");
            registerError.setVisible(true);
        } else if (model.registerUser(username, password)) { // if the register works, sends the user to the login
            showLoginInterface();
        } else { // checks if the username is available
            registerError.setText("Username non disponibile");
            registerError.setVisible(true);
        }
    }

    private boolean validPassword(String password) { // checks if the password respects the standard
        String regex = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>?])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return password.matches(regex);
    }

    @FXML
    private void getRequiredSeats() { // manages the request of seats by a customer
        final int RECEPTIONIST_PORT = 1313; // port to communicate with the receptionist
        try (Socket receptionSocket = new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT)){ // creates a socket to communicate with the receptionist
            unavailableReceptionist.setVisible(false);
            table = getTable(receptionSocket); // says how many seats he needs to the receptionist and gets a table
            if (table > 0) { // if there are available seats, the customer takes them
                showOrderInterface(); // shows second interface's elements
            } else { // otherwise, opens a second socket
                try (Socket receptionSocket2 = new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT)) {
                    seatsBox.setVisible(false); // hides the interface's element to get required seats
                    waitingBox.setVisible(true); // shows the interface's element to get customer's answer about waiting or not
                    getWaitingTime = new BufferedReader(new InputStreamReader(receptionSocket2.getInputStream())); // used to read the time to wait communicated by the receptionist
                    waitingTime = Integer.parseInt(getWaitingTime.readLine()); // read the time to wait from the socket and parses it to integer
                    waitingMessage.setText("Non ci sono abbastanza posti disponibili, vuoi attendere " + waitingTime + " minuti ?"); // shows the waiting time message
                    waitingMessage.setVisible(true);
                }
            }
        } catch (IOException exc) { // if receptionist is unreachable, shows an error message
            unavailableReceptionist.setText("Receptionist non disponibile al momento");
            unavailableReceptionist.setVisible(true);
        }
    }

    @FXML
    private void waitButton() throws IOException { // manages the action when the customer clicks the "Wait" button
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // creates a scheduler to plan the periodic execution of tasks
        ScheduledFuture<?> waitTask = scheduler.schedule(this::onWaitComplete, waitingTime, TimeUnit.SECONDS);  // plans which task execute after a waiting time, and specifies the time unit
        try { // waits for the task to complete (the estimated wait time)
            waitTask.get();
        } catch (InterruptedException | ExecutionException exc) {
            throw new RuntimeException(exc);
        } finally { // deallocates used resources
            scheduler.shutdown();
            getWaitingTime.close();
        }
    }

    private void onWaitComplete() { // manages the completion of the waiting time
        Platform.runLater(() -> {
            Timeline timeline = new Timeline( // after a certain period of time, hides the waiting components and sends the customer to the interface to take orders
                new KeyFrame(Duration.seconds(1), event -> {
                    try { showOrderInterface(); // shows the interface to get orders
                    } catch (IOException exc) {
                        throw new RuntimeException(exc);
                    }
                })
            );
            timeline.play();
        });
    }

    @FXML
    private void leave() { // closes the interface when the customer clicks the "Leave" button
        Stage stage = (Stage) requiredSeatsField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private int getTable(Socket receptionSocket) throws IOException { // allows the customer to specify how many seats they need and to get a table if available
        BufferedReader checkSeats = new BufferedReader(new InputStreamReader(receptionSocket.getInputStream())); // used to get customer's required seats
        PrintWriter sendSeats = new PrintWriter(receptionSocket.getOutputStream(), true); // used to send the number of required seats to the receptionist
        String input = requiredSeatsField.getText(); // gets customer's required seats from the interface
        if (!input.matches("\\d+")) { // checks if the user's entered a number
            unavailableReceptionist.setText("Numero di posti non valido");
            unavailableReceptionist.setVisible(true);
        }
        requiredSeatsField.setText(""); // clears previous input
        int requiredSeats = Integer.parseInt(input); // parses to Integer
        sendSeats.println(requiredSeats); // says how many seats he requires to the receptionist
        int tableNumber = Integer.parseInt(checkSeats.readLine());  // gets the table number from the receptionist if it's possible

        // closes used resources and connection
        checkSeats.close();
        sendSeats.close();
        receptionSocket.close();
        return tableNumber;
    }

    @FXML
    private void showMenu() { // shows the menu in real time
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) { // connection to the database
            String selectQuery = "SELECT * FROM ORDINI"; // query to get each menu's orders
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) { // performs the query
                ResultSet resultSet = preparedStatement.executeQuery();

                // makes the menu viewable
                ObservableList<Order> menuItems = FXCollections.observableArrayList();
                while (resultSet.next()) { // gets each order's name and price
                    String name = resultSet.getString("NOME");
                    float price = resultSet.getFloat("PREZZO");
                    Order order = new Order(name, price); // calls the constructor to build an Order object
                    menuItems.add(order); // adds the order to the menu
                }

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
                                    Label priceLabel = new Label("€" + String.format("%.2f", item.price()) + "€");
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
    private void getOrder(String order, float price) { // allows a customer to get an order
        final int WAITER_PORT = 1316;  // used to communicate with the waiter
        try (Socket waiterSocket = new Socket(InetAddress.getLocalHost(), WAITER_PORT)) { // creates a socket to communicate with the waiter
            unavailableWaiter.setVisible(false);
            BufferedReader eatOrder = new BufferedReader(new InputStreamReader(waiterSocket.getInputStream())); // used to get a customer's order
            PrintWriter takeOrder = new PrintWriter(waiterSocket.getOutputStream(), true); // used to send an order to a waiter
            StringBuilder totalOrdered = new StringBuilder(); // contains each customer's order
            takeOrder.println(order); // sends the order to the waiter
            order = eatOrder.readLine(); // waits for the order and eats it
            totalOrdered.append(order).append("\n"); // adds the order to the customer's list and its price to the bill
            totalOrderedArea.setCellFactory(new Callback<>() { // applies a border to each customer's order
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
        } catch (IOException exc) { // if waiter is unreachable
            unavailableWaiter.setText("Nessun cameriere disponibile al momento");
            unavailableWaiter.setVisible(true);
        }
    }

    @FXML
    private void showLoginInterface() throws IOException { // switches the interface to the login
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) registerUsername.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true); // sets fullscreen
        stage.show(); // shows the interface
    }

    @FXML
    private void showRegisterInterface() throws IOException { // switches the interface to the registration
        FXMLLoader loader = new FXMLLoader(getClass().getResource("RegisterInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) loginUsername.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true); // sets fullscreen
        stage.show(); // shows the interface
    }

    @FXML
    private void showSeatsInterface() throws IOException { // switches the interface to require seats
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GetSeatsInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) loginUsername.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true); // sets fullscreen
        stage.show(); // shows the interface
    }

    @FXML
    private void showOrderInterface() throws IOException { // switches the interface to get orders
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GetOrderInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) requiredSeatsField.getScene().getWindow();
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

        menu.setOnMouseClicked(event -> { // adds an event manager to get customer's order by clicking onto the menu
            Order order = menu.getSelectionModel().getSelectedItem(); // gets customer's clicked order

            // shows a window to get customers confirm
            Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationDialog.setTitle("Conferma ordine");
            confirmationDialog.setHeaderText(null);
            confirmationDialog.setGraphic(null);
            confirmationDialog.setContentText("Sei sicuro di voler ordinare " + order.name() + " ?");

            confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL); // adds confirm and deny buttons
            confirmationDialog.showAndWait().ifPresent(response -> { // waits for customer's response
                if (response == ButtonType.OK) { // if the customer confirms, sends the order to the waiter
                    getOrder(order.name(), order.price());
                }
            });
        });
        stage.show(); // shows the interface
        showMenu(); // shows the menu
    }

    @FXML
    private void askBill() { // method to close customers' interface once they've done

        // shows a window to get customers confirm
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Conferma richiesta conto");
        confirmationDialog.setGraphic(null);
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setContentText("Sei sicuro di voler chiedere il conto?");

        confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL); // adds confirm and deny buttons
        confirmationDialog.showAndWait().ifPresent(response -> { // waits for customer's response
            if (response == ButtonType.OK) { // if customer confirms, closes the interface
                Stage stage = (Stage) stopButton.getScene().getWindow();
                stage.close();
            }
        });
    }
}