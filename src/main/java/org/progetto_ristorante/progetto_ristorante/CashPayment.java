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
        cashText.setText("Pagare l'importo presso la cassa, indicando il numero del tavolo");
        cashText.setStyle("-fx-text-fill: #32CD32; -fx-font-family: 'Helvetica'; -fx-font-size: 30px;");
        cashText.setVisible(true);
    }
}
