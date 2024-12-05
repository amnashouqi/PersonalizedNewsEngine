package model;

import db.DBConnection;
import db.UserManager;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static db.DBConnection.*;
import static db.UserManager.*;
import static model.RecommendationSystem.*;
import static model.User.*;
import static scraper.Webscraper.*;

// Article class contains Category (Composition), and it demonstrates key OOP concepts.
public class Article {
    private int id; // Encapsulation: Stores article's ID, not directly accessible.
    private String title;   // Encapsulation: Stores the article's title.
    private String content; // Encapsulation: Stores the article's content.
    private String topcategory;  // Composition | Article "has-a" top Category

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);    // Thread pooling for concurrent tasks

    // Getters and setters for private attributes to allow controlled access to properties (Encapsulation)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return topcategory;
    }

    public void setCategory(String topcategory) {
        this.topcategory = topcategory;
    }

    // Displays the article's title and content, then handles user feedback. (Abstraction)
    public static void displayArticles(int articleId, int userId) {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT title, content FROM Articles WHERE id = ?")) {
            pstmt.setInt(1, articleId); // Prevents SQL injection (Security principle)
            ResultSet rs = pstmt.executeQuery();
            updateInteractionScore(userId, articleId, 1.0f); // Updates interaction score when the article is viewed (Behavior related to interaction)

            if (rs.next()) {
                String title = rs.getString("title");
                String content = rs.getString("content");
                System.out.println("\n--- Article Details ---");
                System.out.println("Title: " + title);
                System.out.println("Content: ");
                System.out.println(wrapText(content, 110)); // Text wrapping logic for content display (Encapsulation)
                System.out.println();
                handleUserFeedback(userId, articleId);  // Encapsulation: Feedback handling is abstracted into a method.

            } else {
                System.out.println("Article not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handles user feedback after article is displayed. (Polymorphism | different actions based on user input)
    private static void handleUserFeedback(int userId, int articleId) {
        User user = UserManager.getCurrentUser();   // Composition | Article depends on the UserManager class for managing user data.
        Scanner scan = new Scanner(System.in);
        System.out.println("Do you want to either 👍🏽/👎🏽 or Rate the article you just read? (Y/N): ");
        String likeOrNo = scan.next();
        System.out.println();

        if (likeOrNo.equalsIgnoreCase("Y")) {
            System.out.println("Do you want to 👍🏽 or 👎🏽 this article? (Like/Dislike): ");
            String likeOrDislike = scan.next();
            System.out.println();

            if (likeOrDislike.equalsIgnoreCase("Like")) {
                //System.out.println("user.userlikes called b4 ex service");
                UserManager.userLikes(userId, articleId);
                //executorService.submit(() -> user.userLikes(userId, articleId));    // Delegating action to the User class (Abstraction, Polymorphism)
                //System.out.println("user.userlikes called after ex service");
            } else if (likeOrDislike.equalsIgnoreCase("Dislike")) {
                UserManager.userDislikes(userId, articleId);
                //executorService.submit(() -> user.userDislikes(articleId));     // Polymorphism | Executes different methods based on user input
            } else {
                System.out.println("Invalid preference entered. ");
            }

            System.out.println("Do you want to rate the article? (Y/N): ");
            String rateOrNo = scan.next();
            System.out.println();

            if (rateOrNo.equalsIgnoreCase("Y")) {
                UserManager.userRates(userId, articleId);
                //executorService.submit(() -> user.userRates(articleId));    // Asynchronous task submission
                System.out.println();
            } else if (rateOrNo.equalsIgnoreCase("N")) {
                System.out.println("Algorithm is sad without your rating 🥲");
                System.out.println();
            } else {
                System.out.println("Invalid preference entered. ");
            }
        }

        System.out.println("Do you want to read more articles? (enter 1 for yes, 2 for no): ");
        int yesNo = scan.nextInt();
        System.out.println();

        if (yesNo == 1) {
            displayTitles(userId);  // Invokes method to display article titles
        } else {
            clearExistingNews();    // Another abstraction to clear news articles
        }
    }

    // Wraps long text into lines based on the specified line width. (Encapsulation | Handles content formatting)
    private static String wrapText(String text, int lineWidth) {
        String[] words = text.split(" ");
        StringBuilder wrappedText = new StringBuilder();
        int lineLength = 0;

        for (String word : words) {
            if (lineLength + word.length() > lineWidth) {
                wrappedText.append("\n");    // Adds a new line if text exceeds the line width
                lineLength = 0;
            }
            wrappedText.append(word).append(" ");
            lineLength += word.length() + 1;
        }

        return wrappedText.toString().trim();
    }

    // Displays the list of articles for the user. (Uses OOP principles -> abstraction and composition)
    public static void displayTitles(int userId) {

        try (Connection conn = DBConnection.getConnection()) {
            Map<String, Integer> userPreferences = UserManager.getUserPreferences(userId);  // Encapsulation: Accessing user preferences
            boolean hasPreferences = userPreferences.values().stream().anyMatch(score -> score > 0);

            if (hasPreferences) {
                List<String> rankedArticles = hybridRecommendations(userId);    // Uses a hybrid recommendation method (Abstraction, Polymorphism)
                if (!rankedArticles.isEmpty()) {
                    displayRankedArticles(rankedArticles, userId);  // Polymorphism | Displaying ranked articles
                    return;
                }
            }

            displayAllArticles(userId); // If no preferences, shows all articles (Abstraction)
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Displays a ranked list of articles, allowing user to choose one. (Polymorphism | Different behaviors depending on user choice)
    private static void displayRankedArticles(List<String> rankedArticles, int userId) {
        System.out.println("Available Articles (Ranked):");
        int index = 1;

        for (String title : rankedArticles) {
            System.out.println(index + ": " + title);    // Displays ranked article list
            index++;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("Select an article by number (or 0 to exit): ");
        int choice = scanner.nextInt();
        System.out.println();

        if (choice != 0 && choice <= rankedArticles.size()) {
            String selectedTitle = rankedArticles.get(choice - 1);   // Gets selected article
            int articleId = getArticleIdByTitle(selectedTitle);      // Encapsulation | Fetches article ID using title
            Map<String, Integer> articleScores = getArticleScores(articleId);   // Fetches article scores based on classification
            setUserPreferences(userId, articleScores);  // Updates user preferences

            if (articleId != -1) {
                displayArticles(articleId, userId); // Displays the selected article
                executorService.submit(() -> markSkippedArticles(userId, rankedArticles, articleId));    // Marks skipped articles asynchronously
            }
        }
    }

    // Displays a list of all available articles for the user to choose from. (Encapsulation)
    private static void displayAllArticles(int userId) throws SQLException {

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            String query = "SELECT id, title FROM Articles";
            ResultSet rs = stmt.executeQuery(query);
            System.out.println("Available Articles:");
            Map<Integer, Integer> articleMap = new HashMap<>();
            int index = 1;

            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                // Storing article titles with corresponding IDs
                articleMap.put(index, id);
                System.out.println(index + ": " + title);
                index++;
            }

            Scanner scanner = new Scanner(System.in);
            System.out.println("Select an article by number (or 0 to exit): ");
            int choice = scanner.nextInt();

            if (choice != 0 && articleMap.containsKey(choice)) {
                int articleId = articleMap.get(choice); // Gets article ID from selection
                Map<String, Integer> articleScores = getArticleScores(articleId);  // Fetches article scores
                setUserPreferences(userId, articleScores);  // Updates user preferences based on the article
                displayArticles(articleId, userId);  // Displays the selected article
                executorService.submit(() -> markSkippedArticles(userId, articleMap.values(), articleId));  // Marks skipped articles asynchronously
            }
        }
    }

    // Marks articles as skipped for a user. (Abstraction and encapsulation)
    private static void markSkippedArticles(int userId, Collection<?> articleIds, int viewedArticleId) {
        User user = UserManager.getCurrentUser();

        for (Object id : articleIds) {
            if (id instanceof Integer && (Integer) id != viewedArticleId) {
                user.userSkips(userId, (Integer) id); // Skipping articles that were not selected
            }
        }
    }

    // Retrieves the article ID from its title. (Encapsulation)
    public static int getArticleIdByTitle(String title) {
        int articleId = -1;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM Articles WHERE title = ?")) {
            pstmt.setString(1, title);  // Prepared statement to prevent SQL injection
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                articleId = rs.getInt("id");  // Extracts article ID from query result
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articleId;
    }

    // Retrieves the title of an article using its ID. (Encapsulation)
    public static String getArticleTitleById(int articleId) {
        String query = "SELECT title FROM articles WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, articleId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("title");  // Retrieves title from query result
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Retrieves article scores based on its classification (e.g., category, keyword count). (Abstraction)
    public static Map<String, Integer> getArticleScores(int articleId) {
        Map<String, Integer> scores = new HashMap<>();
        String query = "SELECT category, keyword_count FROM article_classification WHERE article_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, articleId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String category = rs.getString("category");
                    int keywordCount = rs.getInt("keyword_count");
                    scores.put(category, keywordCount); // Maps category to keyword count
                }
                System.out.println("Scores for article ID " + articleId + ": " + scores);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return scores;
    }

    // Saves or updates article classification. (Abstraction)
    public static void saveArticleClassification(int articleId, String category, int keywordCount) {

        // SQL query to insert or update the article classification
        String sql = "INSERT INTO article_classification (article_id, category, keyword_count) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE keyword_count = VALUES(keyword_count)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set the parameters for the article classification
            pstmt.setInt(1, articleId);  // Set the article ID
            pstmt.setString(2, category); // Set the category
            pstmt.setInt(3, keywordCount); // Set the keyword count
            pstmt.executeUpdate(); // Execute the query
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
