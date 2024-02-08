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
    public void processPayment() {
        cashText.setText("Pagare l'importo presso la cassa");
        cashText.setVisible(true);
    }
}
