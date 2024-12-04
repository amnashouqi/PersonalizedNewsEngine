package db;
import model.User;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

import static model.Article.getArticleTitleById;

public class UserManager {

    // Static variable to hold the current logged-in user. An approach for managing a single user session at a time.
    private static User currentUser = null;

    // ReentrantLock for thread-safety during user-related operations
    private static final ReentrantLock lock = new ReentrantLock();

    // Using ConcurrentHashMap to store locks for each user, ensuring thread-safety when interacting with user preferences
    private static final Map<Integer, ReentrantLock> userLocks = new ConcurrentHashMap<>();


    // Method to get the current logged-in user
    public static User getCurrentUser() {
        return currentUser;
    }

    // Encapsulation | Managing user registration with database access in a method.
    public static boolean registerUser(User user) {
        lock.lock();  // To ensure thread-safety during the registration process.
        try (Connection conn = DBConnection.getConnection()) {
            String query = "INSERT INTO Users (username, password) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());  // Password should be hashed in a production system.
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }finally {
            lock.unlock();  // Always unlock in the finally block to prevent deadlock.
        }
    }

    // Method to login a user by validating credentials. Returns user ID on success, -1 if login fails.
    public static int loginUser(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id"); // Return user_id if login is successful
            } else {
                return -1; // Return -1 if login fails
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Abstraction | Hiding the complex details of SQL operations while exposing a simple interface to retrieve user preferences.
    public static Map<String, Integer> getUserPreferences(int userId) {
        Map<String, Integer> userPreferences = new HashMap<>();

        String query = "SELECT category, score FROM user_preferences WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId); // Bind userId to prevent SQL injection
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category");
                int preferenceWeight = rs.getInt("score");
                userPreferences.put(category, preferenceWeight);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userPreferences;  // Returning the user preferences (encapsulated data)
    }

    // Method to update user preferences. This ensures thread safety using a ReentrantLock.
    public static void setUserPreferences(int userId, Map<String, Integer> scores) {
        userLocks.putIfAbsent(userId, new ReentrantLock());  //initialization of user-specific locks
        ReentrantLock userLock = userLocks.get(userId);
        userLock.lock();  // Ensure thread-safety when updating user preferences.

        String upsertQuery = "INSERT INTO user_preferences (user_id, category, score) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE score = score + VALUES(score)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertQuery)) {
            // Loop through the category scores and update user preferences
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                String category = entry.getKey();
                int score = entry.getValue();

                // Set the parameters for the upsert query
                pstmt.setInt(1, userId); // User ID
                pstmt.setString(2, category); // Category
                pstmt.setInt(3, score); // Score

                pstmt.addBatch(); // Add to batch for efficient execution
            }

            // Execute the batch update
            pstmt.executeBatch();
            System.out.println("User preferences updated successfully for user ID: " + userId);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            userLock.unlock();  // Always unlock in the finally block
        }
    }



}

