package model;

import db.DBConnection;
import db.UserManager;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static db.DBConnection.*;
import static db.UserManager.*;
import static model.User.*;
import static scraper.Webscraper.*;

public class Article {
    private int id;
    private String title;
    private String content;
    private String category;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);


    // Getters and setters

    public static void displayArticles(int articleId, int userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT title, content FROM Articles WHERE id = ?")) {
            pstmt.setInt(1, articleId); // Use PreparedStatement to prevent SQL injection
            ResultSet rs = pstmt.executeQuery();
            updateInteractionScore(userId, articleId, 1.0f); //1 for viewing
            if (rs.next()) {
                String title = rs.getString("title");
                String content = rs.getString("content");
                System.out.println("\n--- Article Details ---");
                System.out.println("Title: " + title);
                System.out.println("Content: ");
                System.out.println(wrapText(content, 110));
                System.out.println();
                handleUserFeedback(userId, articleId);
            } else {
                System.out.println("Article not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleUserFeedback(int userId, int articleId) {
        Scanner scan = new Scanner(System.in);
        System.out.println("Do you want to either 👍🏽/👎🏽 or Rate the article you just read? (Y/N): ");
        String likeOrNo = scan.next();
        System.out.println();

        if (likeOrNo.equalsIgnoreCase("Y")) {
            System.out.println("Do you want to 👍🏽 or 👎🏽 this article? (Like/Dislike): ");
            String likeOrDislike = scan.next();
            System.out.println();
            if (likeOrDislike.equalsIgnoreCase("Like")) {
                executorService.submit(() -> userLikes(userId, articleId));
            } else if (likeOrDislike.equalsIgnoreCase("Dislike")) {
                executorService.submit(() -> userDislikes(userId, articleId));
            } else {
                System.out.println("Invalid preference entered. ");
            }

            System.out.println("Do you want to rate the article? (Y/N): ");
            String rateOrNo = scan.next();
            System.out.println();
            if (rateOrNo.equalsIgnoreCase("Y")) {
                executorService.submit(() -> userRates(userId, articleId));
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
            displayTitles(userId);
        } else {
            clearExistingNews();
        }
    }

    public static String wrapText(String text, int lineWidth) {
        String[] words = text.split(" ");
        StringBuilder wrappedText = new StringBuilder();
        int lineLength = 0;

        for (String word : words) {
            if (lineLength + word.length() > lineWidth) {
                wrappedText.append("\n");
                lineLength = 0;
            }
            wrappedText.append(word).append(" ");
            lineLength += word.length() + 1;
        }

        return wrappedText.toString().trim();
    }

    public static void displayTitles(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            Map<String, Integer> userPreferences = UserManager.getUserPreferences(userId);
            boolean hasPreferences = userPreferences.values().stream().anyMatch(score -> score > 0);

            if (hasPreferences) {
                List<String> rankedArticles = hybridRecommendations(userId);
                if (!rankedArticles.isEmpty()) {
                    displayRankedArticles(rankedArticles, userId);
                    return;
                }
            }

            displayAllArticles(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void displayRankedArticles(List<String> rankedArticles, int userId) {
        System.out.println("Available Articles (Ranked):");
        int index = 1;
        for (String title : rankedArticles) {
            System.out.println(index + ": " + title);
            index++;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("Select an article by number (or 0 to exit): ");
        int choice = scanner.nextInt();
        System.out.println();

        if (choice != 0 && choice <= rankedArticles.size()) {
            String selectedTitle = rankedArticles.get(choice - 1);
            int articleId = getArticleIdByTitle(selectedTitle);
            Map<String, Integer> articleScores = getArticleScores(articleId);
            setUserPreferences(userId, articleScores);
            if (articleId != -1) {
                displayArticles(articleId, userId);
                executorService.submit(() -> markSkippedArticles(userId, rankedArticles, articleId));
            }
        }
    }

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
                articleMap.put(index, id);
                System.out.println(index + ": " + title);
                index++;
            }

            Scanner scanner = new Scanner(System.in);
            System.out.println("Select an article by number (or 0 to exit): ");
            int choice = scanner.nextInt();
            if (choice != 0 && articleMap.containsKey(choice)) {
                int articleId = articleMap.get(choice);
                Map<String, Integer> articleScores = getArticleScores(articleId);
                setUserPreferences(userId, articleScores);
                displayArticles(articleId, userId);
                executorService.submit(() -> markSkippedArticles(userId, articleMap.values(), articleId));
            }
        }
    }

    private static void markSkippedArticles(int userId, Collection<?> articleIds, int viewedArticleId) {
        for (Object id : articleIds) {
            if (id instanceof Integer && (Integer) id != viewedArticleId) {
                userSkips(userId, (Integer) id);
            }
        }
    }
    public static int getArticleIdByTitle(String title) {
        int articleId = -1;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM Articles WHERE title = ?")) {
            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                articleId = rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articleId;
    }

    public static String getArticleTitleById(int articleId) {
        String query = "SELECT title FROM articles WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, articleId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("title");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

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
                    scores.put(category, keywordCount);
                }
                System.out.println("Scores for article ID " + articleId + ": " + scores);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return scores;
    }
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
