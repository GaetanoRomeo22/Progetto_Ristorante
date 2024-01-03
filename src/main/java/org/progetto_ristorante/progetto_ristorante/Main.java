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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void compileProject(String progettoPath, String javafxPath) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("javac", "--module-path", javafxPath,
                "--add-modules", "javafx.controls,javafx.fxml", progettoPath + "/org/progetto_ristorante/progetto_ristorante/**/*.java");
        Process process = processBuilder.start();

        // Attendi la terminazione della compilazione
        try {
            int exitCode = process.waitFor();
            System.out.println("Compilazione completata con codice di uscita: " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runMainClass(String progettoPath) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "--module-path", progettoPath,
                "--add-modules", "javafx.controls,javafx.fxml", "org.progetto_ristorante.progetto_ristorante.Chef");
        Process process = processBuilder.start();

        // Attendi la terminazione dell'esecuzione
        try {
            int exitCode = process.waitFor();
            System.out.println("Esecuzione completata con codice di uscita: " + exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
