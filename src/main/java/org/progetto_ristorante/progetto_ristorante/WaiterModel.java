package org.progetto_ristorante.progetto_ristorante;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
public class WaiterModel {
    public static class WaiterHandler implements Runnable {
        protected final Socket customerSocket;
        protected final int PORT_TO_CHEF;
        BufferedReader readOrder, readReadyOrder;
        PrintWriter sendOrder, sendReadyOrder;

        public WaiterHandler(Socket accepted, int PORT_TO_CHEF) {
            this.customerSocket = accepted;
            this.PORT_TO_CHEF = PORT_TO_CHEF;
        }

        public void run() {
            try (Socket chefSocket = new Socket(InetAddress.getLocalHost(), PORT_TO_CHEF)) {
                readOrder = new BufferedReader(new InputStreamReader(customerSocket.getInputStream()));
                sendOrder = new PrintWriter(chefSocket.getOutputStream(), true);
                readReadyOrder = new BufferedReader(new InputStreamReader(chefSocket.getInputStream()));
                sendReadyOrder = new PrintWriter(customerSocket.getOutputStream(), true);

                String order;
                do {
                    order = readOrder.readLine();
                    if (order == null) {
                        break;
                    }
                    sendOrder.println(order);
                    order = readReadyOrder.readLine();
                    sendReadyOrder.println(order);
                } while (true);
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            } finally {
                closeConnections();
            }
        }

        private void closeConnections() {
            try {
                customerSocket.close();
                readOrder.close();
                sendReadyOrder.close();
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        }
    }
}
