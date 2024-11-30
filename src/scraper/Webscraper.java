package scraper;
import db.*;
import model.*;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
import java.util.*;
public class Webscraper {
    private static Map<String, String[]> categories = new HashMap<>();
    static {
        categories.put("Politics", new String[]{"politics", "government", "election", "parliament", "policy", "democracy", "legislation", "president", "congress", "prime minister"});
        categories.put("Conflict", new String[]{"war", "conflict", "military", "terror", "attack", "rebel", "troops", "soldier", "weapon", "bombing", "insurgent", "israel", "gaza", "ukraine", "russia", "arms", "killed"});
        categories.put("Economy", new String[]{"economy", "finance", "stock", "trade", "business", "investment", "inflation", "recession", "currency", "market", "banking"});
        categories.put("Technology", new String[]{"technology", "tech", "innovation", "ai", "cyber", "blockchain", "software", "gadget", "robotics", "machine learning", "startup"});
        categories.put("Sports", new String[]{"sports", "football", "cricket", "tennis", "olympics", "athlete", "tournament", "basketball", "fifa", "nba", "score"});
        categories.put("Health", new String[]{"health", "medicine", "covid", "disease", "mental health", "vaccine", "hospital", "treatment", "wellness", "public health", "outbreak"});
        categories.put("Science", new String[]{"science", "space", "climate", "environment", "research", "discovery", "energy", "nature", "wildlife", "sustainability", "experiment"});
        categories.put("Culture", new String[]{"culture", "art", "music", "film", "literature", "theater", "fashion", "museum", "heritage", "festival", "tradition"});
        categories.put("Crime", new String[]{"crime", "court", "justice", "police", "fraud", "trial", "arrest", "lawsuit", "corruption", "homicide", "investigation"});
        categories.put("World", new String[]{"world", "international", "global", "foreign", "diplomacy", "united nations", "embassy", "alliance", "treaty", "migration", "refugee"});
        categories.put("Society", new String[]{"society", "education", "community", "rights", "human rights", "inequality", "gender", "poverty", "justice", "social movement", "activism"});
        categories.put("Business", new String[]{"business", "startup", "entrepreneur", "revenue", "profits", "industry", "market trends", "acquisition", "merger", "shareholders", "branding"});
        categories.put("Environment", new String[]{"environment", "climate change", "pollution", "global warming", "renewable", "sustainability", "biodiversity", "deforestation", "carbon", "green energy"});}
    public static int calculateArticleScore(String content, Map<String, Integer> userPreferences) {
        int totalScore = 0;
        String normalizedContent = content.toLowerCase();
        // Iterate through each category in user preferences
        for (Map.Entry<String, Integer> entry : userPreferences.entrySet()) {
            String category = entry.getKey();
            int preferenceWeight = entry.getValue();
            // Get the keywords for this category
            String[] keywords = categories.get(category);
            if (keywords != null) {
                int categoryScore = 0;
                // Calculate keyword frequency in the article content
                for (String keyword : keywords) {
                    int index = 0;
                    while ((index = normalizedContent.indexOf(keyword, index)) != -1) {
                        categoryScore++;
                        index += keyword.length(); // Move index forward to avoid infinite loop
                    }
                }
                // Apply user preference weight to category score
                totalScore += categoryScore * preferenceWeight;
            }
        }
        return totalScore;
    }
    public static void scrapeArticles(int userId) {
        Map<String, Integer> userPreferences = UserManager.getUserPreferences(userId);
        try {
            // Connect to Al Jazeera News page and Scrape titles and links
            Document doc = Jsoup.connect("https://www.aljazeera.com/").get();
            Elements articles = doc.select(".article-trending__title-link");
            for (Element article : articles) {
                String title = article.text();
                String url = "https://www.aljazeera.com" + article.attr("href");
                Document articlePage = Jsoup.connect(url).get();
                String content = articlePage.select(".wysiwyg").text();
                int articleId = getArticleId(title, url);
                // Categorize the article for simple keyword matching
                Map<String, Integer> scores = categorizeArticle(articleId, content);
                int score = calculateArticleScore(content, userPreferences);
                System.out.println(title + " - Score: " + score);
                System.out.println(url);
                // Save article to DB
                saveArticleToDB(title, content, scores);
            }
            System.out.println();
            displayTitles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static int getArticleId(String title, String url) {
        int articleId = -1;
        try (Connection conn = DBConnection.getConnection()) {
            // Check if the article already exists
            String selectQuery = "SELECT id FROM Articles WHERE title = ? AND url = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(selectQuery)) {
                pstmt.setString(1, title);
                pstmt.setString(2, url);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    articleId = rs.getInt("id");
                    return articleId;  // Article already exists, return its ID
                }
            }
            // If the article doesn't exist, insert it
            String insertQuery = "INSERT INTO Articles (title, url) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, title);
                pstmt.setString(2, url);
                pstmt.executeUpdate();
                // Get the generated ID
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        articleId = generatedKeys.getInt(1);  // Get the generated ID
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articleId;
    }
    public static void displayArticles(int articleId) {
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
                Scanner scanner = new Scanner(System.in);
                System.out.println("Do you want to read more articles? (enter 1 for yes, 2 for no): ");
                int yesNo = scanner.nextInt();
                if (yesNo == 1) {
                    displayTitles();
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
    public static void clearExistingNews() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            String query = "DELETE FROM Articles";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void displayTitles() {
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
                displayArticles(articleMap.get(choice));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static Map<String, Integer> categorizeArticle(int articleId, String content) {
        content = content.toLowerCase(); // Convert to lowercase for easier matching
        Map<String, Integer> scores = new HashMap<>();
        // Calculate scores for each category
        for (Map.Entry<String, String[]> entry : categories.entrySet()) {
            String category = entry.getKey();
            String[] keywords = entry.getValue();
            int score = 0;
            for (String keyword : keywords) {
                if (content.contains(keyword.toLowerCase())) {
                    score++;
                }
            }
            scores.put(category, score);
            String topCategory = getTopCategory(scores);
            // Save the keyword count for this category into the database
            saveArticleClassification(articleId, topCategory, score);
        }
        return scores;
    }
    private static void saveArticleToDB(String title, String content, Map<String, Integer> scores) {
        // SQL query to insert article into the Articles table
        String articleSql = "INSERT INTO Articles (title, content) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(articleSql, Statement.RETURN_GENERATED_KEYS)) {
            // Set the article title and content
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.executeUpdate();
            // Get the generated article ID
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int articleId = rs.getInt(1); // Get the generated article ID
                // Insert or update the article classification for each category in the scores map
                for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                    String category = entry.getKey();
                    int keywordCount = entry.getValue();
                    // Insert or update the classification record in the article_classification table
                    saveArticleClassification(articleId, category, keywordCount);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void saveArticleClassification(int articleId, String category, int keywordCount) {
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
    private static String getTopCategory(Map<String, Integer> scores) {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getKey();
    }
}