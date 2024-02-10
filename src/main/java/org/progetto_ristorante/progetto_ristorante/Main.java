package org.progetto_ristorante.progetto_ristorante;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            Thread receptionistThread = new Thread(() -> { // creates a receptionist thread
                ReceptionistModel receptionistModel = new ReceptionistModel(); // sets the Model
                ReceptionistController receptionistController = new ReceptionistController(receptionistModel); // sets the Controller
                try { // starts receptionist server thread to manage customer's request
                    receptionistController.startServer();
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            });
            receptionistThread.start(); // starts the receptionist thread

            // customer's interface
            FXMLLoader customerLoader = new FXMLLoader(getClass().getResource("LoginInterface.fxml")); // loads the fxml containing the interface
            Parent customerRoot = customerLoader.load();
            Scene customerScene = new Scene(customerRoot);
            primaryStage.setScene(customerScene);
            primaryStage.setMaximized(true); // sets fullscreen
            primaryStage.show(); // shows the interface

            // chef's interface
            FXMLLoader chefLoader = new FXMLLoader(getClass().getResource("ChefInterface.fxml")); // loads the fxml containing the interface
            Parent chefRoot = chefLoader.load();
            Stage chefStage = new Stage();
            Scene chefScene = new Scene(chefRoot);
            chefStage.setScene(chefScene);
            chefStage.setMaximized(true); // sets fullscreen
            chefStage.show(); // shows the interface

            Thread waiterThread = new Thread(() -> { // creates a waiter thread
                WaiterModel waiterModel = new WaiterModel(); // sets the Model
                WaiterController waiterController = new WaiterController(waiterModel); // sets the Controller
                try { // starts waiter server thread to manage customer's request
                    waiterController.startServer();
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            });
            waiterThread.start(); // starts waiter thread
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }
}