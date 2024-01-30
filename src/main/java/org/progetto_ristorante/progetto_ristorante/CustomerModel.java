package org.progetto_ristorante.progetto_ristorante;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;

public class CustomerModel {

    private static CustomerModel instance = null;

    // constructor with singleton pattern
    public static CustomerModel getInstance(){
        if(instance == null)
            instance = new CustomerModel();
        return instance;
    }

    // checks if the customer is registered
    public boolean loginUser(String username, String password) throws SQLException, NoSuchAlgorithmException {

        // encrypts the password
        String hashedPassword = hashPassword(password);

        // connection to the database
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {

            // query to check if the customer is registered
            String query = "SELECT * FROM UTENTI WHERE USERNAME = ? AND PASSWORD = ?";

            // substitutes "?" with username and password and performs the query
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);

                // returns user's data if finds him
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return resultSet.next();
                }
            }
        }
    }

    // manages the insertion of customer's data into the database once registered
    public boolean registerUser(String username, String password) throws SQLException, NoSuchAlgorithmException {

        // encrypts the password
        String hashedPassword = hashPassword(password);

        // connection to the database
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {
            if (!usernameAvailable(connection, username)) { // checks if the username is available
                return false;
            }

            // query to insert the customer into the database
            String query = "INSERT IGNORE INTO UTENTI (USERNAME, PASSWORD) VALUES (?, ?)";

            // substitutes "?" with username and password and performs the query
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);
                preparedStatement.executeUpdate();
                return true;
            }
        }
    }

    // checks if the username is available
    private boolean usernameAvailable(Connection connection, String username) throws SQLException {

        // query to check if the username is available
        String query = "SELECT COUNT(*) FROM UTENTI WHERE USERNAME = ?";

        // substitutes "?" with username and performs the query
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);

            // returns true if the username is available and false otherwise
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return !resultSet.next();
            }
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : hashedBytes) {
            stringBuilder.append(String.format("%02x", b));
        }
        return stringBuilder.toString();
    }

}
