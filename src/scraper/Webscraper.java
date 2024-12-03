package scraper;

import concurrency.ConcurrencyHandler;
import db.*;
import model.*;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static db.DBConnection.*;
import static db.UserManager.*;
import static model.Article.*;
import static model.Category.*;
import static model.User.*;

public class Webscraper {

    private static final ConcurrencyHandler concurrencyHandler = new ConcurrencyHandler(10); // Thread pool with 10 threads

    public static void scrapeArticles(int userId) {
        Map<String, Integer> userPreferences = UserManager.getUserPreferences(userId);

        try {
            // Connect to Al Jazeera News page and scrape titles and links
            Document doc = fetchDocument("https://www.aljazeera.com/");
            if (doc == null) {
                System.err.println("Failed to fetch the main page. Please check your network connection or the website's availability.");
                return;
            }

            Elements articles = doc.select(".article-trending__title-link");
            List<CompletableFuture<Void>> tasks = new ArrayList<>();

            for (Element article : articles) {
                String title = article.text();
                String url = "https://www.aljazeera.com" + article.attr("href");

                // Submit a task for each article
                tasks.add(CompletableFuture.runAsync(() -> processArticle(title, url), concurrencyHandler.getExecutorService()));
            }

            // Wait for all tasks to complete
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
            System.out.println("All articles processed.");

            displayTitles(userId);
        } catch (Exception e) {
            logError("An error occurred while scraping articles", e);
        }
    }

    private static void processArticle(String title, String url) {
        try {
            // Fetch the article page
            Document articlePage = fetchDocument(url);
            if (articlePage == null) {
                System.err.println("Failed to fetch article: " + title + ". Skipping...");
                return;
            }

            String content = articlePage.select(".wysiwyg").text();
            if (content.isEmpty()) {
                System.err.println("Content missing for article: " + title + ". Skipping...");
                return;
            }

            // Save the article to the database
            int articleId = getArticleIdByTitle(title);
            if (articleId == -1) {
                articleId = saveArticleToDB(title, content); // Insert article if not already present
            }

            // Categorize the article based on content
            Map<String, Integer> scores = categorizeArticle(articleId, content);

            // Save categorized scores to the database
            saveArticletoDB2(articleId, scores);
        } catch (Exception e) {
            logError("An error occurred while processing article: " + title, e);
        }
    }

    private static Document fetchDocument(String url) {
        int retries = 3;
        while (retries > 0) {
            try {
                return Jsoup.connect(url).timeout(5000).get();
            } catch (IOException e) {
                retries--;
                System.err.println("Failed to fetch URL: " + url + ". Retries left: " + retries);
                if (retries == 0) {
                    logError("Failed to fetch document after retries: " + url, e);
                }
            }
        }
        return null;
    }

    private static void logError(String message, Exception e) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("logs/error.log", true))) {
            writer.write(message + " - " + e.getMessage());
            writer.newLine();
            for (StackTraceElement element : e.getStackTrace()) {
                writer.write("\t" + element.toString());
                writer.newLine();
            }
        } catch (IOException ioException) {
            System.err.println("Failed to write to log file: " + ioException.getMessage());
        }
    }
}
