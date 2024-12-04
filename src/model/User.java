package model;

import db.DBConnection;
import db.UserManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Scanner;


import static model.RecommendationSystem.*;

// UserInterface: Interface defining methods for user actions (like, dislike, skip, rate)
interface UserInterface {
    int getId();  // Get user ID
    String getUsername();  // Get username
    String getPassword();  // Get password
    void setPassword(String password);  // Set password

    void userLikes(int articleId);  // Like an article
    void userDislikes(int articleId);  // Dislike an article
    void userSkips(int articleId);  // Skip an article
    void userRates(int articleId);  // Rate an article
}

// User class implementing the UserInterface, encapsulating user details and actions
public class User implements UserInterface {
    private int id;  // Unique identifier for the user
    private String username;  // User's username
    private String password;  // User's password

    // Setters for username and id (use of encapsulation for data protection)
    public void setUsername(String username) {
        this.username = username;
    }


    public void setId(int id) {
        this.id = id;
    }

    // Getters (Encapsulation: Access private data via getters)
    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }
    @Override
    public void setPassword(String password) {
        this.password = password;
    }
    @Override
    public String getPassword() {
        return password;
    }

    // Constructor with ID and password (Use of constructor for object initialization)
    public User(int id, String username, String password){
        this.id=id;
        this.username=username;
        this.password=password;
    }

    // Overloaded constructor (Polymorphism: Same method signature with different parameters)
    public User(String username, String password){
        //next_id=id;
        this.username=username;
        this.password=password;
    }

    // Method to handle user liking an article
    public void userLikes(int articleId) {
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
    public void userDislikes(int articleId) {
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
    public void userSkips(int articleId) {
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
    public void userRates(int articleId) {
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