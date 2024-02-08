package org.progetto_ristorante.progetto_ristorante;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class Main extends Application {
    protected MenuObserverManager menuObserverManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        menuObserverManager = new MenuObserverManager(new ArrayList<>());
        try {
            Thread receptionistThread = new Thread(() -> { // executes the receptionist
                ReceptionistModel receptionistModel = new ReceptionistModel();
                ReceptionistController receptionistController = new ReceptionistController(receptionistModel);
                try {
                    receptionistController.startServer();
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            });
            receptionistThread.start();

            // customer's interface
            FXMLLoader customerLoader = new FXMLLoader(getClass().getResource("LoginInterface.fxml"));
            Parent customerRoot = customerLoader.load();
            CustomerController customerController = customerLoader.getController();
            customerController.setMenuObserverManager(menuObserverManager);
            Scene customerScene = new Scene(customerRoot);
            primaryStage.setScene(customerScene);
            primaryStage.setMaximized(true);
            primaryStage.show();

            // chef's interface
            FXMLLoader chefLoader = new FXMLLoader(getClass().getResource("ChefMenuInterface.fxml"));
            Parent chefRoot = chefLoader.load();
            ChefController chefController = chefLoader.getController();
            chefController.setMenuObserverManager(this.menuObserverManager);
            Stage chefStage = new Stage();
            Scene chefScene = new Scene(chefRoot);
            chefStage.setScene(chefScene);
            chefStage.setMaximized(true);
            chefStage.show();

            Thread waiterThread = new Thread(() -> { // executes the waiter
                WaiterModel waiterModel = new WaiterModel();
                WaiterController waiterController = new WaiterController(waiterModel);
                try {
                    waiterController.startServer();
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            });
            waiterThread.start();
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }
}