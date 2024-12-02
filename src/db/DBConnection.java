package db;

import java.sql.*;
import java.util.Map;

import static model.Article.*;
import static scraper.Webscraper.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/news_scraper";
    private static final String USER = "root";  // Replace with your MySQL username
    private static final String PASSWORD = "amna";  // Replace with your MySQL password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public synchronized static void clearExistingNews() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            String query = "DELETE FROM Articles";
            stmt.executeUpdate(query);  // Use executeUpdate for data modification queries
            //System.out.println("Deleted old article data");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int saveArticleToDB(String title, String content) {
        // SQL query to insert article into the Articles table
        String articleSql = "INSERT INTO Articles (title, content) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(articleSql, Statement.RETURN_GENERATED_KEYS)) {
            // Set the article title and content
            pstmt.setString(1, title);
            pstmt.setString(2, content);

            // Execute the update
            int affectedRows = pstmt.executeUpdate();

            // Check if insertion was successful
            if (affectedRows > 0) {
                // Retrieve the generated key (article ID)
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); // Return the generated article ID
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if something went wrong
    }

    public static void saveArticletoDB2(int articleId, Map<String, Integer> scores) {
        // Insert or update the article classification for each category in the scores map
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String category = entry.getKey();
            int keywordCount = entry.getValue();
            // Insert or update the classification record in the article_classification table
            saveArticleClassification(articleId, category, keywordCount);
        }
    }
}
