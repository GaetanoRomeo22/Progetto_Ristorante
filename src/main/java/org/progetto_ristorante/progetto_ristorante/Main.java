package org.progetto_ristorante.progetto_ristorante;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            // Percorso del tuo progetto
            String progettoPath = "C:/Users/figli/Documents/università/Terzo_anno/Programmazione_3/Progetto_Ristorante";

            // Percorso delle librerie di JavaFX (aggiorna con il tuo percorso)
            String javafxPath = "C:/Users/figli/javafx-sdk-21.0.1/lib";

            // Compila il progetto
            compileProject(progettoPath, javafxPath);

            // Esegui il main
            runMainClass(progettoPath);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private static void compileProject(String progettoPath, String javafxPath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("javac", "--module-path", javafxPath,
                "--add-modules", "javafx.controls,javafx.fxml", progettoPath + "/org/progetto_ristorante/progetto_ristorante/**/*.java");
        Process process = processBuilder.start();
        process.waitFor();
    }

    private static void runMainClass(String progettoPath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "--module-path", progettoPath,
                "--add-modules", "javafx.controls,javafx.fxml", "org.progetto_ristorante.progetto_ristorante.Chef");
        Process process = processBuilder.start();
        process.waitFor();
    }
}
