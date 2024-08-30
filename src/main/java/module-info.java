module org.progetto_ristorante.progetto_ristorante {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires java.sql;
    requires javafx.graphics;

    opens org.progetto_ristorante.progetto_ristorante to javafx.fxml;
    exports org.progetto_ristorante.progetto_ristorante;
}