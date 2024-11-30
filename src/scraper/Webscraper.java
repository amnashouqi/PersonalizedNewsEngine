package scraper;

import db.*;
import model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.Connection;

public class Webscraper {
    public static void scrapeArticles() {
        try {
            // Connect to Al Jazeera News page
            Document doc = Jsoup.connect("https://www.aljazeera.com/").get();

            // Scrape titles and links
            Elements articles = doc.select(".gc__title a"); // Adjust selector based on Al Jazeera's HTML

            for (Element article : articles) {
                String title = article.text();
                String url = article.attr("href");

                // Optionally scrape more content for each article
                Document articlePage = Jsoup.connect(url).get();
                String content = articlePage.select(".article-body").text(); // Adjust selector

                // Categorize the article (simple keyword matching)
                String category = categorizeArticle(title);

                // Save to the database
                saveArticleToDB(title, content, category);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String categorizeArticle(String title) {
        // Simple categorization based on keywords
        if (title.toLowerCase().contains("politics")) {
            return "Politics";
        } else if (title.toLowerCase().contains("economy")) {
            return "Economy";
        } else {
            return "General";
        }
    }

    private static void saveArticleToDB(String title, String content, String category) {
        // Insert the article into the Articles table (using DBConnection)
        try (Connection conn = DBConnection.getConnection()) {
            String query = "INSERT INTO Articles (title, content, category) VALUES (?, ?, ?)";
            var stmt = conn.prepareStatement(query);
            stmt.setString(1, title);
            stmt.setString(2, content);
            stmt.setString(3, category);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
