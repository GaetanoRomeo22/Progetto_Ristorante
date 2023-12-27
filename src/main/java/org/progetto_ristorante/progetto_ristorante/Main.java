package org.progetto_ristorante.progetto_ristorante;

public class Main {
    public static void main(String[] args) {
        try {

            // executes chef process
            ProcessBuilder chefBuilder = new ProcessBuilder("java", "org.progetto_ristorante.progetto_ristorante.Chef");
            Process chefProcess = chefBuilder.start();

            // executes receptionist process
            ProcessBuilder receptionistBuilder = new ProcessBuilder("java", "Receptionist");
            Process receptionistProcess = receptionistBuilder.start();

            // executes waiter process
            ProcessBuilder waiterBuilder = new ProcessBuilder("java", "Waiter");
            Process waiterProcess = waiterBuilder.start();

            // executes customer process
            ProcessBuilder customerBuilder = new ProcessBuilder("java", "Customer");
            Process customerProcess = customerBuilder.start();

            // waits for their termination
            int chefExitCode = chefProcess.waitFor();
            int receptionistExitCode = receptionistProcess.waitFor();
            int waiterExitCode = waiterProcess.waitFor();
            int customerExitCode = customerProcess.waitFor();

            System.out.println("Chef Exit Code: " + chefExitCode);
            System.out.println("Receptionist Exit Code: " + receptionistExitCode);
            System.out.println("Waiter Exit Code: " + waiterExitCode);
            System.out.println("Customer Exit Code: " + customerExitCode);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
}


