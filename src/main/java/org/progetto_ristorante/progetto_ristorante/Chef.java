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
        FXMLLoader fxmlLoader = new FXMLLoader(Chef.class.getResource("ChefInterface.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setMaximized(true);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000));
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
        stage.show();
    }
}