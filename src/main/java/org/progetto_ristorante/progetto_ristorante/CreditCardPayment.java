package org.progetto_ristorante.progetto_ristorante;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class CreditCardPayment implements PaymentStrategy {

    @FXML
    private TextField cardNumberField;

    @FXML
    private Label paymentConfirmationLabel;

    public CreditCardPayment(TextField cardNumberField, Label paymentConfirmationLabel) {
        this.cardNumberField = cardNumberField;
        this.paymentConfirmationLabel = paymentConfirmationLabel;
    }

    @Override
    public void processPayment() {
        paymentConfirmationLabel.setText(STR."Importo pagato correttamente con carta di credito. Numero carta: \{cardNumberField.getText()}");
        paymentConfirmationLabel.setStyle("-fx-text-fill: #32CD32; -fx-font-family: 'Helvetica'; -fx-font-size: 30px;");
        paymentConfirmationLabel.setVisible(true);
    }
}
