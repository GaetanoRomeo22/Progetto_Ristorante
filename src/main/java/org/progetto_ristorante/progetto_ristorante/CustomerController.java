package org.progetto_ristorante.progetto_ristorante;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.ResourceBundle;

import static java.lang.StringTemplate.STR;

public class CustomerController implements Initializable {
    @FXML
    private Text billText,
            tableNumber,
            waitingMessage,
            loginError,
            registerError,
            unavailableReceptionist,
            loginRegisterButton,
            registerLoginButton,
            unavailableWaiter,
            cashText;

    @FXML
    private ListView<String> totalOrderedArea;

    @FXML
    private ListView<ConcreteOrder> menu;

    @FXML
    private TextField loginUsername,
            registerUsername,
            requiredSeatsField,
            cardNumberField,
            cardNameField,
            expiryDateField,
            cvvField;

    @FXML
    private PasswordField loginPassword,
            registerPassword,
            confirmPassword;

    @FXML
    private Button loginButton,
                   registerButton,
                   confirmSeatsButton,
                   stopButton,
                   waitingButton,
                   leavingButton,
                   payButton;

    @FXML
    private HBox tableBillBox;

    @FXML
    private VBox seatsBox,
            waitingBox,
            menuBox,
            orderBox,
            cardBox;

    @FXML
    private Label paymentConfirmationLabel;

    private final CustomerModel model;                      // reference to Model
    private int waitingTime;                                // time the customer has to wait if there aren't available seats
    private float bill = 0.0f;                              // customer's total bill
    private int table;                                      // customer's table's number
    protected MenuContext menuContext = new MenuContext();  // used to show discounted or not discounted menu
    protected PaymentStrategy paymentStrategy;              // used to manage payment method

    public CustomerController() { // constructor
        model = CustomerModel.getInstance();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) { // called everytime the interface is loaded
        if (loginButton != null && loginRegisterButton != null) { // sets hover effect in login interface
            loginButton.setOnMouseEntered(_ -> loginButton.setEffect(new DropShadow()));
            loginButton.setOnMouseExited(_ -> loginButton.setEffect(null));
            loginRegisterButton.setOnMouseEntered(_ -> loginRegisterButton.setUnderline(true));
            loginRegisterButton.setOnMouseExited(_ -> loginRegisterButton.setUnderline(false));
        } else if (waitingButton != null && leavingButton != null) { // sets hover effect in get seats interface
            waitingButton.setOnMouseEntered(_ -> waitingButton.setEffect(new DropShadow()));
            waitingButton.setOnMouseExited(_ -> waitingButton.setEffect(null));
            leavingButton.setOnMouseEntered(_ -> leavingButton.setEffect(new DropShadow()));
            leavingButton.setOnMouseExited(_ -> leavingButton.setEffect(null));
        }
    }

    @FXML
    private void login() throws SQLException, NoSuchAlgorithmException, IOException { // manages customer's login
        String username = loginUsername.getText().trim(); // gets username from the interface
        String password = loginPassword.getText().trim(); // gets password from the interface
        if (username.isEmpty() || password.isEmpty()) { // checks if the customer has entered null values
            loginError.setText("Credenziali incomplete");
            loginError.setVisible(true);
        } else if (model.loginUser(username, password)) { // if the login works, shows get seats interface
            showSeatsInterface();
        } else { // if the login doesn't work, shows an error message
            loginError.setText("Credenziali errate, riprovare");
            loginError.setVisible(true);
        }
    }

    @FXML
    private void register() throws SQLException, IOException, NoSuchAlgorithmException { // manages customer's registration
        String username = registerUsername.getText().trim(); // gets username from the interface
        String password = registerPassword.getText().trim(); // gets password from the interface
        String confirmedPassword = confirmPassword.getText().trim(); // gets confirmed password from the interface
        if (username.isEmpty() || password.isEmpty() || confirmedPassword.isEmpty()) { // checks if the customer has entered null values
            registerError.setText("Credenziali incomplete");
            registerError.setVisible(true);
        } else if (!validPassword(password)) { // checks if the password respects the standard
            registerError.setText("La password deve contenere almeno 8 caratteri, una lettera maiuscola, un carattere speciale e un numero");
            registerError.setVisible(true);
        } else if (!confirmedPassword.equals(password)) { // checks if the password isn't correctly confirmed
            registerError.setText("Conferma password errata");
            registerError.setVisible(true);
        } else if (model.registerUser(username, password)) { // if the register works, sends the user to login interface
            showLoginInterface();
        } else if (username.contains(" ")) { // checks that the username not contains spaces
            loginError.setText("L'username non può contenere spazi");
            loginError.setVisible(true);
        } else { // checks if the username is available
            registerError.setText("Username non disponibile");
            registerError.setVisible(true);
        }
    }

    @FXML
    private void getRequiredSeats() { // manages the request of seats by a customer
        final int RECEPTIONIST_PORT = 1313; // port to communicate with the receptionist
        try (SocketHandler receptionSocket = new SocketProxy(new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT))) { // creates a socket to communicate with the receptionist
            unavailableReceptionist.setVisible(false);
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION); // shows a confirmation dialog
            confirmationAlert.setTitle("Conferma posti");
            confirmationAlert.setGraphic(null);
            confirmationAlert.setHeaderText(null);
            confirmationAlert.initOwner(requiredSeatsField.getScene().getWindow());
            confirmationAlert.setContentText(STR."Vuoi prenotare \{requiredSeatsField.getText().trim()} posti?");
            confirmationAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        table = getTable(receptionSocket); // says how many seats he needs to the receptionist and gets a table
                        if (table > 0) { // if there are available seats, the customer takes them
                            showOrderInterface(); // shows second interface's elements
                        } else { // otherwise, opens a second socket
                            try (SocketHandler receptionSocket2 = new SocketProxy(new Socket(InetAddress.getLocalHost(), RECEPTIONIST_PORT))) {
                                seatsBox.setVisible(false);
                                waitingBox.setVisible(true);
                                BufferedReader getWaitingTime = receptionSocket2.getReader(); // used to read the time to wait communicated by the receptionist
                                waitingTime = Integer.parseInt(getWaitingTime.readLine()); // reads waiting time
                                waitingMessage.setText(STR."Non ci sono abbastanza posti disponibili, vuoi attendere \{waitingTime} minuti?"); // shows waiting message
                                waitingMessage.setVisible(true);
                            }
                        }
                    } catch (IOException exc) { // shows an error message if receptionist isn't available
                        unavailableReceptionist.setText("Receptionist non disponibile al momento");
                        unavailableReceptionist.setVisible(true);
                    }
                }
            });
        } catch (IOException exc) { // se il receptionist non è raggiungibile, mostra un messaggio di errore
            unavailableReceptionist.setText("Receptionist non disponibile al momento");
            unavailableReceptionist.setVisible(true);
        }
    }

    @FXML
    private int getTable(SocketHandler receptionSocket) throws IOException { // allows the customer to specify how many seats they need and to get a table if available
        BufferedReader checkSeats = receptionSocket.getReader(); // used to gets a table from the receptionist
        PrintWriter sendSeats = receptionSocket.getWriter(); // used to say to the receptionist how much seats does him require
        String input = requiredSeatsField.getText().trim(); // gets customer's required seats from the interface
        if (!input.matches("\\d+")) { // checks if the user's entered a number
            unavailableReceptionist.setText("Numero di posti non valido");
            unavailableReceptionist.setVisible(true);
        }
        requiredSeatsField.clear(); // clears previous input
        int requiredSeats = Integer.parseInt(input); // parses to Integer
        sendSeats.println(requiredSeats); // says how many seats he requires to the receptionist
        int tableNumber = Integer.parseInt(checkSeats.readLine());  // gets the table number from the receptionist if it's possible
        checkSeats.close(); // closes used resources and connection
        sendSeats.close();
        receptionSocket.close();
        return tableNumber;
    }

    @FXML
    private void waitButton() {
        Service<Void> waitService = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        Thread.sleep(waitingTime * 1000L);
                        return null;
                    }
                };
            }
        };

        waitService.setOnSucceeded(_ -> {
            try {
                showOrderInterface();
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        });

        waitService.start();
    }

    @FXML
    private void showMenu() { // shows the menu in real time
        LocalDate today = LocalDate.now(); // local's date
        DayOfWeek day = today.getDayOfWeek(); // local's day
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) { // shows discounted menu during the weekend
            menuContext.setMenuState(new DiscountMenu());
        } else { // shows full price menu during the week
            menuContext.setMenuState(new NotDiscountMenu());
        }
        applyMenuStyle(); // applies a style to the menu
    }

    @FXML
    private void getOrder(String order, float price) { // allows a customer to get an order
        final int WAITER_PORT = 1316;  // used to communicate with the waiter
        try (SocketHandler waiterSocket = new SocketProxy(new Socket(InetAddress.getLocalHost(), WAITER_PORT))) { // creates a socket to communicate with the waiter
            unavailableWaiter.setVisible(false);
            BufferedReader eatOrder = waiterSocket.getReader(); // used to get the order from the receptionist
            PrintWriter takeOrder = waiterSocket.getWriter(); // used to send an order to the receptionist
            StringBuilder totalOrdered = new StringBuilder(); // contains each customer's order
            takeOrder.println(order); // sends the order to the waiter
            order = eatOrder.readLine(); // waits for the order and eats it
            totalOrdered.append(order).append("\n"); // adds the order to the customer's list and its price to the bill
            applyTotalOrderedStyle(); // applies a style to total ordered
            bill += price; // updates customer's bill
            billText.setText(STR."€\{String.format("%.2f", bill)}");
            totalOrderedArea.getItems().add(totalOrdered.toString()); // shows orders and total bill
        } catch (IOException exc) { // if waiter is unreachable
            unavailableWaiter.setText("Nessun cameriere disponibile al momento");
            unavailableWaiter.setVisible(true);
        }
    }

    @FXML
    private void askBill() { // closes customer's interface once he has done
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION); // shows a confirmation dialog to check if the customer confirms to ask the bill
        confirmationDialog.setTitle("Richiesta conto");
        confirmationDialog.setGraphic(null);
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setContentText("Sei sicuro di voler chiedere il conto?");
        confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL); // adds confirm and deny buttons
        confirmationDialog.initOwner(menu.getScene().getWindow());
        confirmationDialog.showAndWait().ifPresent(response -> { // waits for customer's response
            if (response == ButtonType.OK) { // payment method window
                ChoiceDialog<String> paymentChoiceDialog = new ChoiceDialog<>("Contanti", "Contanti", "Carta di Credito"); // shows to the customer payment methods
                paymentChoiceDialog.setTitle("Metodo di pagamento");
                paymentChoiceDialog.setHeaderText(null);
                paymentChoiceDialog.setGraphic(null);
                paymentChoiceDialog.setContentText("Scegli il metodo di pagamento:");
                paymentChoiceDialog.initOwner(menuBox.getScene().getWindow());
                paymentChoiceDialog.showAndWait().ifPresent(paymentMethod -> { // waits for customer's selection
                    if (paymentMethod.equals("Contanti")) { // if customer chooses cash payment
                        paymentStrategy = new CashPayment(cashText);
                        paymentStrategy.processPayment();
                        tableBillBox.setVisible(false);
                        menuBox.setVisible(false);
                        orderBox.setVisible(false);
                    } else if (paymentMethod.equals("Carta di Credito")) { // if customer chooses credit card payment
                        try { // shows the interface to get credit card data
                            ShowCreditCardInterface();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });
    }

    public void pay() { // allows the user to pay
        String cardNumber = cardNumberField.getText().trim(); // gets card's data from the interface
        String cardName = cardNameField.getText().trim();
        String expiryDate = expiryDateField.getText().trim();
        String cvv = cvvField.getText().trim();
        if (cardNumber.length() != 16) { // checks if card's number's length is valid
            paymentConfirmationLabel.setText("Numero di carta non valido");
            paymentConfirmationLabel.setVisible(true);
            return;
        }
        if (cardName.isEmpty() || !cardName.matches("^[a-zA-Z]+\\s[a-zA-Z]+$")) { // checks if card's name isn't null
            paymentConfirmationLabel.setText("Intestatario mancante");
            paymentConfirmationLabel.setVisible(true);
            return;
        }
        if (cvv.length() != 3) { // checks if the CVV is valid
            paymentConfirmationLabel.setText("CVV non valido");
            paymentConfirmationLabel.setVisible(true);
            return;
        }
        if (!expiryDate.matches("\\d{2}/\\d{2}")) { // checks card's date's format
            paymentConfirmationLabel.setText("Data di scadenza non valida. Utilizzare il formato MM/AA");
            paymentConfirmationLabel.setVisible(true);
            return;
        }
        int currentYear = Calendar.getInstance().get(Calendar.YEAR); // gets current year
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1; // gets current month (+1 because starts with 0)
        String[] expiryParts = expiryDate.split("/"); // splits entered data into year and month
        if (expiryParts.length != 2) { // checks the user has entered month and year
            paymentConfirmationLabel.setText("Data di scadenza non valida. Utilizzare il formato MM/AA");
            paymentConfirmationLabel.setVisible(true);
            return;
        }
        int expiryYear = Integer.parseInt(expiryParts[1]); // gets expiry year from date
        int expiryMonth = Integer.parseInt(expiryParts[0]); // gets expiry month from date
        if (expiryMonth < 1 || expiryMonth > 12) { // checks if the month is valid
            paymentConfirmationLabel.setText("Mese di scadenza non valido");
            paymentConfirmationLabel.setVisible(true);
            return;
        }
        if (expiryYear + 2000 < currentYear || (expiryYear == currentYear && expiryMonth < currentMonth)) { // checks if the card is expired
            paymentConfirmationLabel.setText("La carta è scaduta");
            paymentConfirmationLabel.setVisible(true);
            return;
        }
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION); // shows a confirmation dialog to check if customer confirms credit card's data
        confirmationDialog.setTitle("Conferma pagamento");
        confirmationDialog.setHeaderText(null);
        confirmationDialog.setGraphic(null);
        confirmationDialog.setContentText("Sei sicuro di voler procedere al pagamento?");
        confirmationDialog.initOwner(cardBox.getScene().getWindow());
        confirmationDialog.showAndWait().ifPresent(result -> { // checks customer's answer
            if (result == ButtonType.OK) { // if chef confirms
                // performs the payment
                paymentStrategy = new CreditCardPayment(cardNumberField, paymentConfirmationLabel);
                paymentStrategy.processPayment();
                cardBox.setVisible(false);
            }
        });
    }

    @FXML
    private void showLoginInterface() throws IOException { // switches the interface to the login
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginInterface.fxml"));
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) registerUsername.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true); // sets fullscreen
        initializeLoginInterfaceElements(scene); // initializes interface's elements
        stage.show(); // shows the interface
    }

    @FXML
    private void showRegisterInterface() throws IOException { // switches the interface to the registration
        FXMLLoader loader = new FXMLLoader(getClass().getResource("RegisterInterface.fxml")); // loads the fxml containing the interface
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) loginUsername.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true); // sets fullscreen
        initializeRegisterInterfaceElements(scene); // initializes interface's elements
        stage.show(); // shows the interface
    }

    @FXML
    private void showSeatsInterface() throws IOException { // switches the interface to require seats
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GetSeatsInterface.fxml")); // loads the fxml containing the interface
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) loginUsername.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true); // sets fullscreen
        initializeSeatsOrderInterface(scene); // initializes interface's elements
        stage.show(); // shows the interface
    }

    private void showOrderInterface() throws IOException { // switches the interface to get orders
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GetOrderInterface.fxml")); // loads the fxml containing the interface
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) seatsBox.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true);
        initializeOrderInterfaceElements(scene); // initializes interface's elements
        setMouseClickHandler(); // sets an event handler that catches customer's clicks on the interface
        stage.show(); // shows the interface
        showMenu(); // shows the menu
    }

    @FXML
    private void ShowCreditCardInterface() throws IOException { // switches the interface to the payment method
        FXMLLoader loader = new FXMLLoader(getClass().getResource("CreditCardPaymentInterface.fxml")); // loads the fxml containing the interface
        Parent parent = loader.load();
        Scene scene = new Scene(parent);
        Stage stage = (Stage) menuBox.getScene().getWindow();
        stage.setScene(scene);
        stage.setMaximized(true); // sets fullscreen
        initializeCreditCardInterfaceElements(scene); // initializes interface's elements
        stage.show(); // shows the interface
    }

    private void initializeLoginInterfaceElements(Scene scene) { // initializes LoginInterface's elements
        loginButton = (Button) scene.lookup("#loginButton");
        loginRegisterButton = (Text) scene.lookup("#loginRegisterButton");
        loginButton.setOnMouseEntered(_ -> loginButton.setEffect(new DropShadow()));
        loginButton.setOnMouseExited(_ -> loginButton.setEffect(null));
        loginRegisterButton.setOnMouseEntered(_ -> loginRegisterButton.setUnderline(true));
        loginRegisterButton.setOnMouseExited(_ -> loginRegisterButton.setUnderline(false));
    }

    private void initializeRegisterInterfaceElements(Scene scene) { // initializes RegisterInterface's elements
        registerButton = (Button) scene.lookup("#registerButton");
        registerLoginButton = (Text) scene.lookup("#registerLoginButton");
        registerButton.setOnMouseEntered(_ -> registerButton.setEffect(new DropShadow()));
        registerButton.setOnMouseExited(_ -> registerButton.setEffect(null));
        registerLoginButton.setOnMouseEntered(_ -> registerLoginButton.setUnderline(true));
        registerLoginButton.setOnMouseExited(_ -> registerLoginButton.setUnderline(false));
    }

    private void initializeSeatsOrderInterface(Scene scene) { // initializes GetSeatsInterface's elements
        confirmSeatsButton = (Button) scene.lookup("#confirmSeatsButton");
        confirmSeatsButton.setOnMouseEntered(_ -> confirmSeatsButton.setEffect(new DropShadow()));
        confirmSeatsButton.setOnMouseExited(_ -> confirmSeatsButton.setEffect(null));
    }

    private void initializeOrderInterfaceElements(Scene scene) { // initializes GetOrderInterface's elements
        menu = (ListView<ConcreteOrder>) scene.lookup("#menu");
        totalOrderedArea = (ListView<String>) scene.lookup("#totalOrderedArea");
        tableNumber = (Text) scene.lookup("#tableNumber");
        tableNumber.setText(String.valueOf(table));
        billText = (Text) scene.lookup("#billText");
        billText.setText("€0");
        unavailableWaiter = (Text) scene.lookup("#unavailableWaiter");
        stopButton = (Button) scene.lookup("#stopButton");
        stopButton.setOnMouseEntered(_ -> stopButton.setEffect(new DropShadow()));
        stopButton.setOnMouseExited(_ -> stopButton.setEffect(null));
    }

    private void initializeCreditCardInterfaceElements(Scene scene) { // initializes CreditCardPaymentInterface's elements
        cardNameField = (TextField) scene.lookup("#cardNameField");
        cardNumberField = (TextField) scene.lookup("#cardNumberField");
        cvvField = (TextField) scene.lookup("#cvvField");
        expiryDateField = (TextField) scene.lookup("#expiryDateField");
        paymentConfirmationLabel = (Label) scene.lookup("#paymentConfirmationLabel");
        payButton = (Button) scene.lookup("#payButton");
        payButton.setOnMouseEntered(_ -> payButton.setEffect(new DropShadow()));
        payButton.setOnMouseExited(_ -> payButton.setEffect(null));
    }

    private void setMouseClickHandler () { // sets an event handler that catches customer's clicks on the interface
        menu.setOnMouseClicked(_ -> { // adds an event manager to get customer's order by clicking onto the menu
            ConcreteOrder order = menu.getSelectionModel().getSelectedItem(); // gets customer's clicked order
            if (order != null) {
                Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION); // shows a window to get customers confirm
                confirmationDialog.setTitle("Conferma ordine");
                confirmationDialog.setHeaderText(null);
                confirmationDialog.setGraphic(null);
                confirmationDialog.setContentText(STR."Sei sicuro di voler ordinare \{order.name()} ?");
                confirmationDialog.initOwner(menu.getScene().getWindow());
                confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL); // adds confirm and deny buttons
                confirmationDialog.showAndWait().ifPresent(response -> { // waits for customer's response
                    if (response == ButtonType.OK) { // if the customer confirms, sends the order to the waiter
                        getOrder(order.name(), order.price());
                    }
                });
                menu.getSelectionModel().clearSelection(); // removes order's selection on the interface
            }
        });
    }

    @FXML
    private void leave() { // closes the interface when the customer clicks the "Leave" button
        Stage stage = (Stage) requiredSeatsField.getScene().getWindow();
        stage.close();
    }

    private boolean validPassword(String password) { // checks if the password respects the standard
        String regex = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>?])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return password.matches(regex);
    }

    private void applyMenuStyle() { // applies a style to the menu
        menu.setCellFactory(new Callback<>() { // applies a border to each menu's order
            @Override
            public ListCell<ConcreteOrder> call(ListView<ConcreteOrder> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(ConcreteOrder item, boolean empty) {
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
                            setOnMouseEntered(_ -> setStyle("-fx-border-color: #F5DEB3; -fx-padding: 10px; -fx-margin: 10px; -fx-background-color: #ECD797"));
                            setOnMouseExited(_ -> setStyle("-fx-border-color: #F5DEB3; -fx-padding: 10px; -fx-margin: 10px"));
                        }
                    }
                };
            }
        });
        menu.setItems(menuContext.getMenuState().getMenu()); // makes the menu viewable as a list of Order elements (name-price)
    }

    private void applyTotalOrderedStyle() { // applies a style to the list of orders
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
                            setStyle("-fx-border-color: #D2B48C; -fx-border-width: 1");
                        }
                    }
                };
            }
        });
    }
}