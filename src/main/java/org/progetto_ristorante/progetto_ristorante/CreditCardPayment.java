package org.progetto_ristorante.progetto_ristorante;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.w3c.dom.Text;

import java.io.IOException;

public class CreditCardPayment implements PaymentStrategy{

    @FXML
    private TextField cardNumberField;
    @FXML
    private Label paymentConfirmationLabel;




    public CreditCardPayment( TextField cardNumberField, Label paymentConfirmationLabel){
        this.cardNumberField = cardNumberField;
        this.paymentConfirmationLabel = paymentConfirmationLabel;
    }

    @Override
    public void processPayment() {
        paymentConfirmationLabel.setText(STR."Importo pagato correttamente con carta di credito. Numero carta: \{cardNumberField.getText()}.");
        paymentConfirmationLabel.setVisible(true);
    }
}
