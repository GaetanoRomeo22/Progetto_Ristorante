package org.progetto_ristorante.progetto_ristorante;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;

public class CustomerModell {

    private static  CustomerModell istance = null;

    private CustomerModell(){}

    public static CustomerModell getIstance(){
        if(istance == null)
            istance = new CustomerModell();
        return istance;
    }
    public boolean loginUser(String username, String password) throws SQLException, NoSuchAlgorithmException {
        String hashedPassword = hashPassword(password);
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {
            String query = "SELECT * FROM UTENTI WHERE USERNAME = ? AND PASSWORD = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return resultSet.next();
                }
            }
        }
    }

    public boolean registerUser(String username, String password, String confirmedPassword) throws SQLException, NoSuchAlgorithmException {
        if (username.isEmpty() || password.isEmpty() || confirmedPassword.isEmpty()) {
            return false;
        }
        if (!validPassword(password)) {
            return false;
        }
        if (!password.equals(confirmedPassword)) {
            return false;
        }
        String hashedPassword = hashPassword(password);
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/RISTORANTE", "root", "Gaetano22")) {
            if (!usernameAvailable(connection, username)) {
                return false;
            }
            String query = "INSERT IGNORE INTO UTENTI (USERNAME, PASSWORD) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);
                preparedStatement.executeUpdate();
                return true;
            }
        }
    }

    private boolean usernameAvailable(Connection connection, String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM UTENTI WHERE USERNAME = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return !resultSet.next();
            }
        }
    }

    private boolean validPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>?])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return password.matches(regex);
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
