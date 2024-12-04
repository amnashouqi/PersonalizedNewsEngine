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

import static db.UserManager.*;
import static model.Article.*;
import static model.User.*;
import static scraper.Webscraper.*;

public class Category {

    // Map to store categories and their related keywords (Composition: the Category class "has-a" Map for keyword categorization)
    public static Map<String, String[]> categories = new HashMap<>();

    // Static block to initialize the categories map with predefined keywords for each category (Initialization, Static Block)
    static {

        categories.put("Politics", new String[]{
                "politics", "government", "election", "parliament", "policy", "democracy", "legislation", "president", "congress", "prime minister",
                "senate", "vote", "campaign", "party", "coalition", "laws", "politician", "constitutional", "political", "bureaucracy", "suffrage",
                "governance", "diplomacy", "autocracy", "dictatorship", "voting rights", "referendum"
        });

        categories.put("Conflict", new String[]{
                "war", "conflict", "military", "terror", "attack", "rebel", "troops", "soldier", "weapon", "bombing", "insurgent", "israel", "gaza",
                "ukraine", "russia", "arms", "killed", "victims", "militants", "peace talks", "tensions", "ceasefire", "clash", "hostilities", "violence",
                "occupation", "peace agreement", "intervention", "genocide", "refugees", "humanitarian"
        });

        categories.put("Economy", new String[]{
                "economy", "finance", "stock", "trade", "business", "investment", "inflation", "recession", "currency", "market", "banking",
                "GDP", "economic growth", "unemployment", "interest rates", "fiscal policy", "deflation", "debt", "global economy", "consumer",
                "trade deficit", "corporations", "startups", "entrepreneurship", "commerce", "global trade", "economic disparity", "currency exchange"
        });

        categories.put("Technology", new String[]{
                "technology", "tech", "innovation", "ai", "cyber", "blockchain", "software", "gadget", "robotics", "machine learning", "startup",
                "smartphones", "internet of things", "5G", "cloud computing", "virtual reality", "augmented reality", "big data", "artificial intelligence",
                "cryptocurrency", "digital transformation", "automation", "biotechnology", "cybersecurity", "data privacy", "tech industry", "electronic waste"
        });

        categories.put("Sports", new String[]{
                "sports", "football", "cricket", "tennis", "olympics", "athlete", "tournament", "basketball", "fifa", "nba", "score", "olympics",
                "world cup", "medals", "athletics", "stadium", "fan", "team", "coach", "referee", "match", "championship", "finals", "league", "boxing",
                "rugby", "cycling", "swimming", "f1", "athleticism"
        });

        categories.put("Health", new String[]{
                "health", "medicine", "covid", "disease", "mental health", "vaccine", "hospital", "treatment", "wellness", "public health",
                "outbreak", "pandemic", "epidemic", "medical research", "doctors", "healthcare", "mental illness", "nutrition", "mental wellness",
                "vaccination", "cancer", "obesity", "diabetes", "cardiovascular", "hygiene", "global health", "public safety", "drug abuse", "surgery"
        });

        categories.put("Science", new String[]{
                "science", "space", "climate", "environment", "research", "discovery", "energy", "nature", "wildlife", "sustainability", "experiment",
                "astronomy", "biotechnology", "scientific research", "physics", "chemistry", "genetics", "geology", "marine biology", "climate change",
                "renewable energy", "conservation", "evolution", "quantum physics", "environmental science", "science policy", "earthquake", "oceanography"
        });

        categories.put("Culture", new String[]{
                "culture", "art", "music", "film", "literature", "theater", "fashion", "museum", "heritage", "festival", "tradition", "history",
                "architecture", "cultural heritage", "music industry", "cinema", "dance", "painting", "sculpture", "photography", "design",
                "art history", "theatre", "poetry", "literary", "performing arts", "comedy", "celebrity", "media"
        });

        categories.put("Crime", new String[]{
                "crime", "court", "justice", "police", "fraud", "trial", "arrest", "lawsuit", "corruption", "homicide", "investigation", "robbery",
                "theft", "assault", "victim", "law", "criminal", "law enforcement", "penal system", "drug trafficking", "organized crime",
                "criminal justice", "sentence", "jail", "prison", "human trafficking", "gang", "illegal"
        });

        categories.put("World", new String[]{
                "world", "international", "global", "foreign", "diplomacy", "united nations", "embassy", "alliance", "treaty", "migration", "refugee",
                "UN", "global governance", "international relations", "foreign policy", "international trade", "cross-border", "foreign affairs",
                "peacekeeping", "foreign aid", "intergovernmental", "UNHCR", "climate action", "foreign intervention", "humanitarian aid"
        });

        categories.put("Society", new String[]{
                "society", "education", "community", "rights", "human rights", "inequality", "gender", "poverty", "justice", "social movement",
                "activism", "social justice", "discrimination", "racism", "LGBTQ+", "equality", "cultural diversity", "mental health", "youth",
                "feminism", "labor rights", "unions", "workplace", "accessibility", "poverty reduction", "NGO", "refugees", "social mobility"
        });

        categories.put("Business", new String[]{
                "business", "startup", "entrepreneur", "revenue", "profits", "industry", "market trends", "acquisition", "merger", "shareholders",
                "branding", "investment", "venture capital", "M&A", "corporate", "supply chain", "e-commerce", "disruption", "advertising",
                "business model", "corporate social responsibility", "fintech", "business growth", "digital economy", "business intelligence", "company culture"
        });

        categories.put("Environment", new String[]{
                "environment", "climate change", "pollution", "global warming", "renewable", "sustainability", "biodiversity", "deforestation",
                "carbon", "green energy", "natural resources", "ecosystem", "greenhouse gases", "carbon footprint", "climate action",
                "conservation", "wildlife", "solar energy", "wind energy", "water scarcity", "ocean pollution", "environmental policy", "sustainable living",
                "climate crisis", "environmental protection", "nature reserve", "energy efficiency"
        });
    }

    // Method to categorize an article by analyzing its content and matching it with keywords for each category (Encapsulation)
    // This method encapsulates the logic of categorizing articles based on content analysis.
    public static Map<String, Integer> categorizeArticle(int articleId, String content) {
        content = content.toLowerCase();  // Converts content to lowercase for easier keyword matching (Encapsulation)
        Map<String, Integer> scores = new HashMap<>();  // Stores the score for each category (Encapsulation)

        // Loop over each category and its associated keywords (Polymorphism: The same process is applied to different categories)
        for (Map.Entry<String, String[]> entry : categories.entrySet()) {
            String category = entry.getKey();  // Category name (Encapsulation: Limited access to category names)
            String[] keywords = entry.getValue();  // Keywords for the category (Encapsulation: Limited access to keyword data)

            int score = 0;
            // Loop through each keyword to check for its occurrence in the article's content (Polymorphism)
            for (String keyword : keywords) {
                if (content.contains(keyword.toLowerCase())) {  // Case-insensitive matching (Polymorphism: Same method applies to all keywords)
                    score++;  // Increases score when a keyword is found (Encapsulation: Internal logic is hidden)
                }
            }
            scores.put(category, score);  // Store the score for this category (Encapsulation)

            // Determine the top category based on the highest score (Abstraction: Logic for determining top category is abstracted away)
            String topCategory = getTopCategory(scores);

            // Save the keyword count for this category into the database (Abstraction: The implementation of saving data is abstracted)
            saveArticleClassification(articleId, topCategory, score);
        }
        return scores;  // Return the scores map (Encapsulation)
    }

    // Method to get the category with the highest score (Abstraction)
    // It abstracts the logic of determining the top category based on the keyword match count.
    public static String getTopCategory(Map<String, Integer> scores) {
        // Returns the category with the highest score (Abstraction: Hides complexity behind a single line of code)
        return scores.entrySet().stream()  // Stream to process the entries in the map (Functional Programming, Encapsulation)
                .max(Map.Entry.comparingByValue())  // Compare by value (score) to get the top category (Abstraction)
                .orElseThrow()  // Throw exception if no category is found (Encapsulation)
                .getKey();  // Returns the top category (Encapsulation)
    }
}
