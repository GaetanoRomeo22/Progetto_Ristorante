package org.progetto_ristorante.progetto_ristorante;

public class Main {
    public static void main(String[] args) {
        try {
            // Esegui il processo Chef
            ProcessBuilder chefBuilder = new ProcessBuilder("java", "Chef");
            Process chefProcess = chefBuilder.start();

            // Esegui il processo Receptionist
            ProcessBuilder receptionistBuilder = new ProcessBuilder("java", "Receptionist");
            Process receptionistProcess = receptionistBuilder.start();

            // Esegui il processo Waiter
            ProcessBuilder waiterBuilder = new ProcessBuilder("java", "Waiter");
            Process waiterProcess = waiterBuilder.start();

            // Esegui il processo Customer
            ProcessBuilder customerBuilder = new ProcessBuilder("java", "Customer");
            Process customerProcess = customerBuilder.start();

            // Attendi che tutti i processi terminino
            int chefExitCode = chefProcess.waitFor();
            int receptionistExitCode = receptionistProcess.waitFor();
            int waiterExitCode = waiterProcess.waitFor();
            int customerExitCode = customerProcess.waitFor();

            System.out.println("Chef Exit Code: " + chefExitCode);
            System.out.println("Receptionist Exit Code: " + receptionistExitCode);
            System.out.println("Waiter Exit Code: " + waiterExitCode);
            System.out.println("Customer Exit Code: " + customerExitCode);

        } catch (Exception e) {
            throw new RuntimeException(e) ;
        }
    }
}


