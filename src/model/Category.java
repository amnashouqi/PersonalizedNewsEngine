package model;
import db.*;
import scraper.Webscraper.*;
import model.*;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.sql.*;
import java.sql.Connection;
import java.util.*;
//
import static db.UserManager.*;
import static model.Article.*;
import static model.User.*;
import static scraper.Webscraper.*;

public class Category {

    public static Map<String, String[]> categories = new HashMap<>();
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

    public static Map<String, Integer> categorizeArticle(int articleId, String content) {
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

    public static String getTopCategory(Map<String, Integer> scores) {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getKey();
    }
}
