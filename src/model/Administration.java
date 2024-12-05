package model;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static model.Category.categories;

public class Administration {
    // Admin actions and options
    public static void Administration() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("Welcome Admin! Please select an option:");
            System.out.println("1. Add Article");
            System.out.println("2. Delete Article");
            System.out.println("3. Update Keywords for Categorization");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();  // Consume the newline

            switch (choice) {
                case 1: // Add Article
                    System.out.print("Enter article title: ");
                    String title = scanner.nextLine();
                    System.out.print("Enter article content: ");
                    String content = scanner.nextLine();
                    addArticle(title, content);
                    break;

                case 2: // Delete Article
                    System.out.println("Here are the current articles:");
                    displayArticles(); // Method to display articles for selection
                    System.out.print("Enter the article number to delete: ");
                    int articleId = scanner.nextInt();
                    deleteArticle(articleId);
                    break;

                case 3: // Update Keywords for Categorization
                    System.out.println("Updating Keywords for Categorization.");
                    updateKeywords(); // Method to update keywords
                    break;

                case 4: // Exit
                    exit = true;
                    System.out.println("Exiting admin interface...");
                    break;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // Method to add a new article to the database
    private static void addArticle(String title, String content) {
        String sql = "INSERT INTO articles (title, content) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.executeUpdate();
            System.out.println("Article added successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Method to display all articles (for deletion purposes)
    private static void displayArticles() {
        String sql = "SELECT id, title FROM articles";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                System.out.println(rs.getInt("id") + ". " + rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Method to delete an article by its ID
    private static void deleteArticle(int articleId) {
        String sql = "DELETE FROM articles WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, articleId);
            pstmt.executeUpdate();
            System.out.println("Article deleted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void updateKeywords() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the category to update (or enter a new category name): ");
        String category = scanner.nextLine();

        // Check if the category exists; if not, create a new one
        if (categories.containsKey(category)) {
            System.out.println("Category found. Enter the new keyword to add:");
        } else {
            System.out.println("Category not found. A new category will be created.");
            System.out.println("Enter the new keyword to add to the new category:");
        }

        String newKeyword = scanner.nextLine().toLowerCase();  // Convert keyword to lowercase for consistency

        // Add the new keyword to the existing category or create a new category
        if (categories.containsKey(category)) {
            // Get the existing keywords for the category
            List<String> keywordList = new ArrayList<>(Arrays.asList(categories.get(category)));

            // Avoid adding duplicate keywords
            if (!keywordList.contains(newKeyword)) {
                keywordList.add(newKeyword);  // Add the new keyword
                categories.put(category, keywordList.toArray(new String[0]));  // Update the category with the new list of keywords
                System.out.println("Keyword added to existing category: " + category);
            } else {
                System.out.println("Keyword already exists in the category.");
            }
        } else {
            // Create a new category with the new keyword
            categories.put(category, new String[]{newKeyword});
            System.out.println("New category created with the keyword: " + newKeyword);
        }
    }








}
