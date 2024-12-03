package db;
import model.User;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

import static model.Article.getArticleTitleById;

public class UserManager {
    private static final double CONTENT_WEIGHT = 0.5;
    private static final double COLLABORATIVE_WEIGHT = 0.5;
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Map<Integer, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    public static boolean registerUser(User user) {
        lock.lock();
        try (Connection conn = DBConnection.getConnection()) {
            String query = "INSERT INTO Users (username, password) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());  // Consider hashing passwords
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }finally {
            lock.unlock();  // Always unlock in the finally block
        }
    }

    public static int loginUser(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id"); // Return user_id if login is successful
            } else {
                return -1; // Return -1 if login fails
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static Map<String, Integer> getUserPreferences(int userId) {
        Map<String, Integer> userPreferences = new HashMap<>();

        String query = "SELECT category, score FROM user_preferences WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId); // Bind userId to prevent SQL injection
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category");
                int preferenceWeight = rs.getInt("score");
                userPreferences.put(category, preferenceWeight);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userPreferences;
    }
    public static void setUserPreferences(int userId, Map<String, Integer> scores) {
        userLocks.putIfAbsent(userId, new ReentrantLock());
        ReentrantLock userLock = userLocks.get(userId);
        userLock.lock();
        String upsertQuery = "INSERT INTO user_preferences (user_id, category, score) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE score = score + VALUES(score)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertQuery)) {
            // Loop through the category scores and update user preferences
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                String category = entry.getKey();
                int score = entry.getValue();

                // Set the parameters for the upsert query
                pstmt.setInt(1, userId);       // User ID
                pstmt.setString(2, category); // Category
                pstmt.setInt(3, score);       // Score

                pstmt.addBatch(); // Add to batch for efficient execution
            }

            // Execute the batch update
            pstmt.executeBatch();
            System.out.println("User preferences updated successfully for user ID: " + userId);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            userLock.unlock();  // Always unlock in the finally block
        }
    }

    public static List<String> rankArticlesForUser(int userId) {
        lock.lock();
        List<String> rankedArticles = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            // Retrieve user preferences
            Map<String, Integer> userPreferences = UserManager.getUserPreferences(userId);

            // Prepare the SQL query to rank articles by keyword count for preferred categories
            String query = """
        SELECT a.title, SUM(ac.keyword_count * up.score) AS total_score
        FROM Articles a
        JOIN article_classification ac ON a.id = ac.article_id
        JOIN user_preferences up ON ac.category = up.category AND up.user_id = ?
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
                pstmt.setInt(index++, userId); // Set the user ID in the query

                // Set categories dynamically
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
        } finally {
            lock.unlock();
        }
        return rankedArticles;
    }
    public static void updateInteractionScore(int userId, int articleId, float interactionScore) {
        String updateQuery = "INSERT INTO user_article_interactions (user_id, article_id, interaction) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE interaction = interaction + ?"; // Add to existing interaction score

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {

            pstmt.setInt(1, userId); // Set user_id
            pstmt.setInt(2, articleId); // Set article_id
            pstmt.setFloat(3, interactionScore); // Set the new interaction score (for new record)
            pstmt.setFloat(4, interactionScore); // Add the interaction score to the existing value

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

//    public static List<String> PythonIntegration(int userId) {
//        List<String> recommendations = new ArrayList<>();
//        try {
//            // Specify the Python script path and Python executable
//            String pythonScriptPath = "MLmodel.py"; // Update with your Python script's path
//            String pythonExecutable = "python";  // Or specify full path to your python executable if needed
//
//            // Create a process to run the Python script with the userId as an argument
//            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, pythonScriptPath, String.valueOf(userId));
//            processBuilder.redirectErrorStream(true); // Redirect error to output stream
//            Process process = processBuilder.start();  // Start the process
//
//            // Capture output of the Python script
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                // Assuming the Python script prints the recommendations as a JSON array
//                // For example: ["101", "102", "103"]
//                recommendations.add(line);  // Add the line to the list (you may need to parse this)
//            }
//
//            // Wait for the Python process to complete
//            int exitCode = process.waitFor();
//            //System.out.println("Python script executed with exit code: " + exitCode);
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Assuming the Python script prints a list of article IDs in JSON format
//        // You might need to parse the string properly if it's returned in a non-plain format.
//        return recommendations;
//    }

    public static List<String> hybridRecommendations(int userId) {
        List<String> contentBased = rankArticlesForUser(userId);  // Content-based recommendations
        List<String> collaborative = recommendCollaborative(userId,5);  // Collaborative filtering via Python

        // Combine and rank
        Map<String, Double> hybridScores = new HashMap<>();

        // Add content-based scores
        for (String article : contentBased) {
            hybridScores.put(article, hybridScores.getOrDefault(article, 0.0) + CONTENT_WEIGHT);
        }

        // Add collaborative filtering scores
        for (String article : collaborative) {
            hybridScores.put(article, hybridScores.getOrDefault(article, 0.0) + COLLABORATIVE_WEIGHT);
        }

        // Sort by scores
        return hybridScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }


    // Collaborative Filtering Methods

    private static Map<Integer, Map<Integer, Float>> buildUserItemMatrix() {
        Map<Integer, Map<Integer, Float>> matrix = new HashMap<>();
        String query = "SELECT user_id, article_id, interaction FROM user_article_interactions";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                int articleId = rs.getInt("article_id");
                float interaction = rs.getFloat("interaction");

                matrix.putIfAbsent(userId, new HashMap<>());
                matrix.get(userId).put(articleId, interaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return matrix;
    }

    private static Map<Integer, Map<Integer, Double>> computeUserSimilarities(Map<Integer, Map<Integer, Float>> userItemMatrix) {
        Map<Integer, Map<Integer, Double>> userSimilarities = new HashMap<>();
        for (Integer userA : userItemMatrix.keySet()) {
            userSimilarities.putIfAbsent(userA, new HashMap<>());
            for (Integer userB : userItemMatrix.keySet()) {
                if (userA.equals(userB)) continue;

                double similarity = calculateCosineSimilarity(userItemMatrix.get(userA), userItemMatrix.get(userB));
                userSimilarities.get(userA).put(userB, similarity);
            }
        }
        return userSimilarities;
    }

    private static double calculateCosineSimilarity(Map<Integer, Float> userA, Map<Integer, Float> userB) {
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (Integer articleId : userA.keySet()) {
            float ratingA = userA.get(articleId);
            float ratingB = userB.getOrDefault(articleId, 0.0f);
            dotProduct += ratingA * ratingB;
            normA += Math.pow(ratingA, 2);
        }
        for (float ratingB : userB.values()) {
            normB += Math.pow(ratingB, 2);
        }
        return (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    public static List<String> recommendCollaborative(int userId, int topN) {
        Map<Integer, Map<Integer, Float>> userItemMatrix = buildUserItemMatrix();
        Map<Integer, Map<Integer, Double>> userSimilarities = computeUserSimilarities(userItemMatrix);

        if (!userSimilarities.containsKey(userId)) return new ArrayList<>();

        Map<Integer, Double> scores = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : userSimilarities.get(userId).entrySet()) {
            int similarUser = entry.getKey();
            double similarity = entry.getValue();

            for (Map.Entry<Integer, Float> item : userItemMatrix.getOrDefault(similarUser, Collections.emptyMap()).entrySet()) {
                int articleId = item.getKey();
                if (!userItemMatrix.get(userId).containsKey(articleId)) { // Skip already interacted items
                    scores.put(articleId, scores.getOrDefault(articleId, 0.0) + similarity * item.getValue());
                }
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> getArticleTitleById(entry.getKey()))
                .collect(Collectors.toList());
    }



}

