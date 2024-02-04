package org.progetto_ristorante.progetto_ristorante;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class Main extends Application {
    protected MenuObserverManager menuObserverManager;

    @Override
    public void start(Stage primaryStage) {
        menuObserverManager = new MenuObserverManager(new ArrayList<>());
        try {

            FXMLLoader customerLoader = new FXMLLoader(getClass().getResource("LoginInterface.fxml"));
            Parent customerRoot = customerLoader.load();
            CustomerController customerController = customerLoader.getController();
            customerController.setMenuObserverManager(menuObserverManager);

            FXMLLoader chefLoader = new FXMLLoader(getClass().getResource("ChefMenuInterface.fxml"));
            Parent chefRoot = chefLoader.load();
            ChefController chefController = chefLoader.getController();
            chefController.setMenuObserverManager(menuObserverManager);

            Scene customerScene = new Scene(customerRoot);
            primaryStage.setScene(customerScene);
            primaryStage.setMaximized(true); // sets fullscreen
            primaryStage.show();

            Stage chefStage = new Stage();
            Scene chefScene = new Scene(chefRoot);
            chefStage.setScene(chefScene);
            chefStage.setMaximized(true); // sets fullscreen
            chefStage.show();
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}