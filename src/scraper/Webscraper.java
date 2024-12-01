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

                // Save the article to the database
                int articleId = getArticleId(title);
                if (articleId == -1) {
                    // Insert article if not already present and get the ID
                    articleId = saveArticleToDB(title, content);
                }
                //System.out.println("Article ID: " + articleId);

                // Categorize the article for simple keyword matching
                Map<String, Integer> scores = categorizeArticle(articleId, content);
                //int score = calculateArticleScore(content, userPreferences);
                //System.out.println(title + " - Score: " + score);
                //System.out.println(url);
                saveArticletoDB2(articleId, scores);

            }
            System.out.println();
            displayTitles(userId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //to decide on the order of displaying articles for a returning user
    public static List<String> rankArticlesForUser(int userId) {
        List<String> rankedArticles = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            // Retrieve user preferences
            Map<String, Integer> userPreferences = UserManager.getUserPreferences(userId);
            // Build SQL query to rank articles by keyword count for preferred categories
            String query = """
            SELECT a.title, SUM(ac.keyword_count) AS total_score
            FROM Articles a
            JOIN article_classification ac ON a.id = ac.article_id
            WHERE ac.category IN (%s)
            GROUP BY a.id
            ORDER BY total_score DESC
        """;
            // Prepare category placeholders dynamically
            String categories = String.join(",", Collections.nCopies(userPreferences.size(), "?"));
            query = String.format(query, categories);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                // Set user-preferred categories in the query
                int index = 1;
                for (String category : userPreferences.keySet()) {
                    pstmt.setString(index++, category);
                }
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    rankedArticles.add(rs.getString("title")); // Add titles to the result list
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rankedArticles;
    }
    public static int getArticleId(String title) {
        int articleId = -1;
        try (Connection conn = DBConnection.getConnection()) {
            // Check if the article already exists
            String selectQuery = "SELECT id FROM Articles WHERE title = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(selectQuery)) {
                pstmt.setString(1, title);
                //pstmt.setString(2, url);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    articleId = rs.getInt("id");
                    return articleId;  // Article already exists, return its ID
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articleId;
    }
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
    public static void clearExistingNews() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            String query = "DELETE FROM Articles";
            stmt.executeUpdate(query);  // Use executeUpdate for data modification queries
            //System.out.println("Deleted old article data");
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
                        if (articleId != -1) {
                            displayArticles(articleId,userId);
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
                    displayArticles(articleMap.get(choice),userId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper method to get an article ID by its title
    private static int getArticleIdByTitle(String title) {
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
    private static int saveArticleToDB(String title, String content) {
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

    private static void saveArticletoDB2(int articleId, Map<String, Integer> scores) {
        // Insert or update the article classification for each category in the scores map
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String category = entry.getKey();
            int keywordCount = entry.getValue();
            // Insert or update the classification record in the article_classification table
            saveArticleClassification(articleId, category, keywordCount);
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