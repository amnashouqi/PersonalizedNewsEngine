package scraper;

import db.*;
import model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
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
        categories.put("Environment", new String[]{"environment", "climate change", "pollution", "global warming", "renewable", "sustainability", "biodiversity", "deforestation", "carbon", "green energy"});
    }

    public static void scrapeArticles() {
        try {
            // Connect to Al Jazeera News page
            Document doc = Jsoup.connect("https://www.aljazeera.com/").get();

            // Scrape titles and links
            Elements articles = doc.select(".article-trending__title-link");

            for (Element article : articles) {
                String title = article.text();
                String url = "https://www.aljazeera.com" + article.attr("href"); // Complete the URL if it's relative

                // Optionally scrape more content for each article
                Document articlePage = Jsoup.connect(url).get();
                String content = articlePage.select(".wysiwyg").text(); // Adjust selector if necessary

                // Categorize the article (simple keyword matching)
                Map<String, Integer> scores = categorizeArticle(content);

                // Save article to DB
                saveArticleToDB(title, content, scores);
            }
            System.out.println();
            displayTitles();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void displayTitles() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            String query = "SELECT id, title FROM Articles";
            ResultSet rs = stmt.executeQuery(query);

            System.out.println("Available Articles:");
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                System.out.println(id + ". " + title);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Integer> categorizeArticle(String content) {
        content = content.toLowerCase(); // Convert to lowercase for easier matching
        Map<String, Integer> scores = new HashMap<>();

        // Calculate scores for each category
        for (Map.Entry<String, String[]> entry : categories.entrySet()) {
            String category = entry.getKey();
            String[] keywords = entry.getValue();

            int score = 0;
            for (String keyword : keywords) {
                if (content.contains(keyword)) {
                    score++;
                }
            }

            scores.put(category, score);
        }

        return scores;
    }

    private static String getTopCategory(Map<String, Integer> scores) {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getKey();
    }

    private static void saveArticleToDB(String title, String content, Map<String, Integer> scores) {
        try (Connection conn = DBConnection.getConnection()) {
            // Get the top category
            String topCategory = getTopCategory(scores);

            // Save the article with its top category
            String query = "INSERT INTO Articles (title, content, category) VALUES (?, ?, ?)";
            var stmt = conn.prepareStatement(query);
            stmt.setString(1, title);
            stmt.setString(2, content);
            stmt.setString(3, topCategory); // Save only the top category
            stmt.executeUpdate();

            System.out.println("Saved article with top category: " + topCategory);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}