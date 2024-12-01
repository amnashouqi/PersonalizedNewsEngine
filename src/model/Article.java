package model;

import db.DBConnection;
import db.UserManager;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static db.DBConnection.clearExistingNews;
import static db.UserManager.setUserPreferences;
import static model.User.*;
import static scraper.Webscraper.*;

public class Article {
    private int id;
    private String title;
    private String content;
    private String category;

    // Getters and setters

    public static void displayArticles(int articleId, int userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT title, content FROM Articles WHERE id = ?")) {
            pstmt.setInt(1, articleId); // Use PreparedStatement to prevent SQL injection
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String title = rs.getString("title");
                String content = rs.getString("content");
                System.out.println("\n--- Article Details ---");
                System.out.println("Title: " + title);
                System.out.println("Content: " + content);
                System.out.println();
                Scanner scan = new Scanner(System.in);
                System.out.println("Do you want to either 👍🏽/👎🏽 or Rate the article you just read? (Y/N): ");
                String likeOrNo = scan.next();
                System.out.println();
                if (likeOrNo.equalsIgnoreCase("Y")){
                    System.out.println("Do you want to 👍🏽 or 👎🏽 this article? (Like/Dislike): ");
                    String likeOrDislike = scan.next();
                    if (likeOrDislike.equalsIgnoreCase("Like")){
                        userLikes(userId, articleId);
                    }else if(likeOrDislike.equalsIgnoreCase("Dislike")){
                        userDislikes(userId, articleId);
                    }else{
                        System.out.println("Invalid preference entered. ");
                    }
                    System.out.println("Do you want to rate the article? (Y/N): ");
                    String RateOrNo = scan.next();
                    if (RateOrNo.equalsIgnoreCase("Y")){
                        userRates(userId, articleId);
                    }else if(RateOrNo.equalsIgnoreCase("N")){
                        userRates(userId, articleId);
                    }else{
                        System.out.println("Invalid preference entered. ");
                    }
                }
                else{
                    System.out.println("your like or no button isnt working");
                }
                Scanner scanner = new Scanner(System.in);
                System.out.println("Do you want to read more articles? (enter 1 for yes, 2 for no): ");
                int yesNo = scanner.nextInt();
                if (yesNo == 1) {
                    displayTitles(userId);
                }
                else{
                    clearExistingNews();
                    System.out.println("cleaning inside the loop");
                }
            } else {
                System.out.println("Article not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void displayTitles(int userId) {
        try (Connection conn = DBConnection.getConnection()) {
            // Check if user preferences exist and have a score > 0
            Map<String, Integer> userPreferences = UserManager.getUserPreferences(userId);
            boolean hasPreferences = userPreferences.values().stream().anyMatch(score -> score > 0);

            if (hasPreferences) {
                // Rank articles based on user preferences
                List<String> rankedArticles = rankArticlesForUser(userId);
                if (!rankedArticles.isEmpty()) {
                    System.out.println("Available Articles (Ranked):");
                    int index = 1;
                    for (String title : rankedArticles) {
                        System.out.println(index + ": " + title);
                        index++;
                    }
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("use preference included");
                    System.out.println("Select an article by number (or 0 to exit): ");
                    int choice = scanner.nextInt();
                    if (choice != 0 && choice <= rankedArticles.size()) {


                        // Fetch the article ID by title (could optimize if title-to-ID map exists)
                        String selectedTitle = rankedArticles.get(choice - 1);
                        int articleId = getArticleIdByTitle(selectedTitle);
                        // Get the scores for the selected article
                        Map<String, Integer> articleScores = getArticleScores(articleId);
                        // Update the user preferences using the article's scores
                        setUserPreferences(userId, articleScores);
                        if (articleId != -1) {
                            displayArticles(articleId,userId);
                            for (String title : rankedArticles) {
                                int idToCheck = getArticleIdByTitle(title);
                                if (idToCheck != articleId) {
                                    userSkips(userId, idToCheck);
                                }
                            }

                        }
                    }
                    return; // Exit early since ranked articles are displayed
                }

            }

            // Default behavior: Display all articles if no preferences exist or no ranked articles
            try (Statement stmt = conn.createStatement()) {
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
                    System.out.println("sample testingggggggg");
                    int articleId=articleMap.get(choice);
                    Map<String, Integer> articleScores = getArticleScores(articleId);
                    // Update the user preferences using the article's scores
                    setUserPreferences(userId, articleScores);
                    displayArticles(articleId,userId);
                    for (int idToCheck : articleMap.values()) {
                        if (idToCheck != articleId) {
                            userSkips(userId, idToCheck);
                        }
                    }

                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
