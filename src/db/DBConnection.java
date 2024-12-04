package db;

import java.sql.*;
import java.util.Map;

import static model.Article.*;
import static scraper.Webscraper.*;

public class DBConnection {
    // Constants for database connection details - Encapsulation: hiding the internal implementation of the connection
    private static final String URL = "jdbc:mysql://localhost:3306/news_scraper";
    private static final String USER = "root";
    private static final String PASSWORD = "amna";

    /**
     * Establishes and returns a database connection.
     * Encapsulation: Abstracts the connection logic from the rest of the program.
     *
     * @return Connection object for interacting with the database
     * @throws SQLException if the connection fails
     */
    public static Connection getConnection() throws SQLException {
        // Using DriverManager to establish a connection to the database
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Clears all existing articles from the Articles table.
     * Synchronization ensures that this method can only be executed by one thread at a time.
     * Thread safety is achieved using synchronized keyword.
     */
    public synchronized static void clearExistingNews() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            // SQL query to delete all articles
            String query = "DELETE FROM Articles";
            stmt.executeUpdate(query);  // Use executeUpdate for data modification queries
            //System.out.println("Deleted old article data");
        } catch (Exception e) {
            e.printStackTrace();// Exception handling using try-catch block
        }
    }

    /**
     * Saves an article's title and content to the database.
     * This method demonstrates encapsulation by interacting with the database via SQL queries.
     *
     * @param title   The title of the article
     * @param content The content of the article
     * @return the article ID if saved successfully, or -1 if something went wrong
     */
    public static int saveArticleToDB(String title, String content) {
        // SQL query to insert article into the Articles table
        String articleSql = "INSERT INTO Articles (title, content) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(articleSql, Statement.RETURN_GENERATED_KEYS)) {
            // Set the article title and content using parameterized queries (prevents SQL injection)
            pstmt.setString(1, title);
            pstmt.setString(2, content);

            // Execute the update
            int affectedRows = pstmt.executeUpdate();

            // Check if insertion was successful
            if (affectedRows > 0) {
                // Retrieve the generated key (article ID) after successful insertion
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); // Return the generated article ID
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Exception handling to catch SQL errors
        }
        return -1; // Return -1 if something went wrong, indicating failure
    }

    /**
     * Saves article classifications for the provided article ID.
     * It uses a Map to store categories and their associated keyword counts.
     * This method applies the use of OOP principles by utilizing a data structure (Map) to group related data. This could be seen as overloading when you compare how the database is updated versus the article information saving process.
     *
     * @param articleId The ID of the article
     * @param scores    A Map containing categories and keyword counts for the article classification
     */
    public static void saveArticletoDB2(int articleId, Map<String, Integer> scores) {
        // Loop through the map containing category names and their keyword counts
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String category = entry.getKey();// Key representing the category name
            int keywordCount = entry.getValue();// Value representing the keyword count for the category

            // Insert or update the classification record in the article_classification table
            saveArticleClassification(articleId, category, keywordCount);
        }
    }
}
