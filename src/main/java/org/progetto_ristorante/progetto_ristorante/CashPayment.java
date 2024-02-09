package org.progetto_ristorante.progetto_ristorante;

import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class CashPayment implements PaymentStrategy {

    @FXML
    Text cashText;

    public CashPayment(Text cashText) {
        this.cashText = cashText;
    }

    @Override
    public void processPayment() { // executes the payment with cash
        cashText.setText("Pagare l'importo presso la cassa, indicando il numero del tavolo");
        cashText.setVisible(true);
    }
}
