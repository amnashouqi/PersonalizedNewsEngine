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

import static db.DBConnection.*;
import static db.UserManager.*;
import static model.Article.*;
import static model.Category.*;
import static model.User.*;

public class Webscraper {

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
                int articleId = getArticleIdByTitle(title);
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





}