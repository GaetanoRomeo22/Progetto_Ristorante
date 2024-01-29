package org.progetto_ristorante.progetto_ristorante;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
public class ChefModel {
    private final int PORT = 1315;

    public void startServer() {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    Socket acceptedOrder = serverSocket.accept();
                    Thread chef = new Thread(new ChefHandler(acceptedOrder));
                    chef.start();
                }
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        });
        serverThread.start();
    }

    public static String getOrder(Socket acceptedOrder) throws IOException {
        BufferedReader takeOrder = new BufferedReader(new InputStreamReader(acceptedOrder.getInputStream()));
        return takeOrder.readLine();
    }

    public static void giveOrder(Socket acceptedOrder, String order) throws IOException {
        PrintWriter sendOrder = new PrintWriter(acceptedOrder.getOutputStream(), true);
        sendOrder.println(order);
    }

    public static class ChefHandler implements Runnable {

        protected final Socket customerSocket;

        public ChefHandler(Socket accepted) {
            this.customerSocket = accepted;
        }

        public void run() {
            String order;
            while (true) {
                try {
                    order = getOrder(customerSocket);
                    if (order == null) {
                        break;
                    }
                    giveOrder(customerSocket, order);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
