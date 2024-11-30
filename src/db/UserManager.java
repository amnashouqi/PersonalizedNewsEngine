package db;

import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserManager {
    public static boolean registerUser(User user) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "INSERT INTO Users (username, password) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());  // Consider hashing passwords
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean loginUser(String username, String password) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT * FROM Users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);  // Passwords should be hashed
            ResultSet rs = stmt.executeQuery();
            return rs.next();  // If user exists, return true
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
