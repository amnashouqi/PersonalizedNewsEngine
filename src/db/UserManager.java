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
import static model.RecommendationSystem.updateInteractionScore;

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


            // Check if the username already exists
            String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, user.getUsername());

            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);

            if (count > 0) {
                System.out.println("Username already exists. Registration failed.");
                System.out.println();
                return false; // User already exists
            }

            // Insert new user
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

    public static void userLikes(int articleId) {
        // Get current user instance using UserManager (encapsulation of global user)
        User user = UserManager.getCurrentUser();

        // SQL queries for fetching categories and updating preferences
        String fetchCategoriesQuery = "SELECT category, keyword_count FROM article_classification WHERE article_id = ?";
        String updatePreferencesQuery = """
        INSERT INTO user_preferences (user_id, category, score)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE score = score + ?
    """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement fetchStmt = conn.prepareStatement(fetchCategoriesQuery);
             PreparedStatement updateStmt = conn.prepareStatement(updatePreferencesQuery)) {

            // Fetch the categories and weights associated with the article
            fetchStmt.setInt(1, articleId);
            ResultSet rs = fetchStmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category");
                int keywordCount = rs.getInt("keyword_count");

                // Calculate dynamic impact of a like
                int impactOfALike = 2 * keywordCount; // Dynamic weight based on keyword_count

                // Update the user's preferences for each category (use of batching for efficiency)
                updateStmt.setInt(1, user.getId());
                updateStmt.setString(2, category);
                updateStmt.setInt(3, impactOfALike); // Insert score
                updateStmt.setInt(4, impactOfALike); // Increment score if exists
                updateStmt.addBatch(); // Add to batch (improves performance)
            }

            // Update interaction score for this user
            updateInteractionScore(user.getId(), articleId, 5.0f); // 5.0 for like

            // Execute the batch update (use of batch processing for better performance)
            updateStmt.executeBatch();
            System.out.println("Thumbs up! You officially liked it. We knew you had good taste.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to handle user disliking an article (similar to the 'userLikes' method)
    public static void userDislikes(int articleId) {
        User user = UserManager.getCurrentUser();

        // SQL queries for fetching categories and updating preferences
        String fetchCategoriesQuery = "SELECT category, keyword_count FROM article_classification WHERE article_id = ?";
        String updatePreferencesQuery = """
        INSERT INTO user_preferences (user_id, category, score)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE score = score + ?
    """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement fetchStmt = conn.prepareStatement(fetchCategoriesQuery);
             PreparedStatement updateStmt = conn.prepareStatement(updatePreferencesQuery)) {

            // Fetch the categories and weights associated with the article
            fetchStmt.setInt(1, articleId);
            ResultSet rs = fetchStmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category");
                int keywordCount = rs.getInt("keyword_count");

                // Calculate dynamic impact of a dislike
                int impactOfADislike = -2 * keywordCount; // Dynamic weight based on keyword_count

                // Update the user's preferences for each category
                updateStmt.setInt(1, user.getId());
                updateStmt.setString(2, category);
                updateStmt.setInt(3, impactOfADislike); // Insert score
                updateStmt.setInt(4, impactOfADislike); // Decrement score if exists
                updateStmt.addBatch(); // Add to batch
            }

            // Update interaction score for this user (negative score for dislike)
            updateInteractionScore(user.getId(), articleId, -5.0f); // -5.0 for dislike

            // Execute the batch update
            updateStmt.executeBatch();
            System.out.println("A thumbs down? Don’t worry, we’ll find something better!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Method to handle user skipping an article (same idea as liking/disliking)
    public static void userSkips(int articleId) {
        User user = UserManager.getCurrentUser();

        // Define the weightage of a skip
        int impactOfASkip = -1; // Reduce score for skipping

        // SQL queries for fetching categories and updating preferences
        String fetchCategoriesQuery = "SELECT category FROM article_classification WHERE article_id = ?";
        String updatePreferencesQuery = """
        INSERT INTO user_preferences (user_id, category, score)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE score = GREATEST(score + ?, 0)
    """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement fetchStmt = conn.prepareStatement(fetchCategoriesQuery);
             PreparedStatement updateStmt = conn.prepareStatement(updatePreferencesQuery)) {

            // Fetch the categories associated with the article
            fetchStmt.setInt(1, articleId);
            ResultSet rs = fetchStmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category");

                // Update the user's preferences for each category
                updateStmt.setInt(1, user.getId());
                updateStmt.setString(2, category);
                updateStmt.setInt(3, impactOfASkip); // Insert score
                updateStmt.setInt(4, impactOfASkip); // Decrement score if exists
                updateStmt.addBatch(); // Add to batch
            }

            // Update interaction score for this user (skip has minimal impact)
            updateInteractionScore(user.getId(), articleId, -1.0f);

            // Execute the batch update
            updateStmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Method to handle user rating an article (allows users to rate articles from 1 to 10)
    public static void userRates(int articleId) {
        User user = UserManager.getCurrentUser();

        // SQL queries for fetching categories and updating preferences
        String fetchCategoriesQuery = "SELECT category, keyword_count FROM article_classification WHERE article_id = ?";
        String updatePreferencesQuery = """
        INSERT INTO user_preferences (user_id, category, score)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE score = ?
    """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement fetchCategoriesStmt = conn.prepareStatement(fetchCategoriesQuery);
             PreparedStatement updateStmt = conn.prepareStatement(updatePreferencesQuery)) {

            // Fetch the categories and scores associated with the article
            fetchCategoriesStmt.setInt(1, articleId);
            ResultSet rs = fetchCategoriesStmt.executeQuery();

            // Prompt user for a rating
            Scanner scanner = new Scanner(System.in);
            System.out.println("Rate it like you’re reviewing a Netflix series from 1 to 10: ");
            int rating = scanner.nextInt();

            // Validate rating input (Ensures input validation)
            if (rating < 1 || rating > 10) {
                System.out.println("Uh-oh, you’ve gone off the charts. A rating between 1 and 10 is all we need!");
                return;
            }

            // Calculate rating multiplier based on rating value (polymorphism in handling different user inputs)
            double multiplier;
            if (rating == 6) {
                multiplier = 1; // No impact for a neutral rating
                updateInteractionScore(user.getId(), articleId, 0.0f); // 0.0 for neutral
            } else if (rating > 6) {
                multiplier = 1 + (rating - 6) * 0.5; // Increment by 0.5 per step above 6
                System.out.println("Top contender right here! This article just leveled up.");
                updateInteractionScore(user.getId(), articleId, (rating - 3));
            } else {
                multiplier = 1 - (6 - rating) * 0.2; // Decrease multiplier for negative ratings (below 6)
                updateInteractionScore(user.getId(), articleId, rating - 5); // 0.0 for dislike
                System.out.println("Don’t worry, we are working hard behind the scenes to find your favorites!");
            }

            // Iterate over categories and update scores based on the multiplier
            while (rs.next()) {
                String category = rs.getString("category");
                int currentScore = rs.getInt("keyword_count"); // Fetch the score directly from article_classification

                // Apply multiplier and calculate new score
                int roundedScore = (int) Math.round(currentScore * multiplier);
                if (roundedScore < 0) roundedScore = 0; // Ensure score doesn't go negative

                // Update the score in the database
                updateStmt.setInt(1, user.getId());
                updateStmt.setString(2, category);
                updateStmt.setInt(3, roundedScore); // Insert score
                updateStmt.setInt(4, roundedScore); // Update score
                updateStmt.addBatch(); // Add to batch
            }

            // Execute batch update
            updateStmt.executeBatch();
            System.out.println("User rating updated successfully for user ID: " + user.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}

