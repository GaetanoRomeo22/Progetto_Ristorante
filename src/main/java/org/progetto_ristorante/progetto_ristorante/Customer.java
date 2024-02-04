package org.progetto_ristorante.progetto_ristorante;

import javafx.application.Application;
import java.io.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Customer extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        stage.setTitle("Customer");
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LoginInterface.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setMaximized(true); // sets fullscreen
        stage.show(); // shows the interface
    }
}