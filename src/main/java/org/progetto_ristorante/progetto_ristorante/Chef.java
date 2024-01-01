package org.progetto_ristorante.progetto_ristorante;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import java.io.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Chef extends Application {

    public static void main(String[] args) {
        launch();
    }

    public void start(Stage stage) throws IOException {
        stage.setTitle("Chef");
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ChefInterface.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}