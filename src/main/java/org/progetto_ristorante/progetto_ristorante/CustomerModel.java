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

    private CustomerModel() {} // constructor for singleton pattern (lazy implementation)

    public static CustomerModel getInstance() { // returns an instance or defines a new one if not exists already
        if(instance == null)
            instance = new CustomerModel();
        return instance;
    }

    public boolean loginUser(String username, String password) throws SQLException, NoSuchAlgorithmException { // checks if the customer is registered
        String hashedPassword = hashPassword(password); // encrypts the password
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano_22")) { // connection to the database
            String query = "SELECT * FROM UTENTI WHERE USERNAME = ? AND PASSWORD = ?"; // query to check if the customer is registered
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) { // substitutes "?" with username and password and performs the query
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);
                try (ResultSet resultSet = preparedStatement.executeQuery()) { // returns user's data if finds him
                    return resultSet.next();
                }
            }
        }
    }

    public boolean registerUser(String username, String password) throws SQLException, NoSuchAlgorithmException { // manages the insertion of customer's data into the database once registered
        String hashedPassword = hashPassword(password); // encrypts the password
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano_22")) { // connection to the database
            if (!usernameAvailable(connection, username)) { // checks if the username is available
                return false;
            }
            String query = "INSERT IGNORE INTO UTENTI (USERNAME, PASSWORD) VALUES (?, ?)"; // query to insert the customer into the database
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) { // substitutes "?" with username and password and performs the query
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);
                preparedStatement.executeUpdate();
                return true;
            }
        }
    }

    private boolean usernameAvailable(Connection connection, String username) throws SQLException { // checks if the username is available
        String query = "SELECT COUNT(*) FROM UTENTI WHERE USERNAME = ?"; // query to check if the username is available
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) { // substitutes "?" with username and performs the query
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) { // returns true if the username is available and false otherwise
                resultSet.next();
                int count = resultSet.getInt(1);
                return count == 0;
            }
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException { // encrypts passwords with a hash algorithm
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : hashedBytes) {
            stringBuilder.append(String.format("%02x", b));
        }
        return stringBuilder.toString();
    }
}
