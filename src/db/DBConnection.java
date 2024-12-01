package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/news_scraper";
    private static final String USER = "root";  // Replace with your MySQL username
    private static final String PASSWORD = "amna";  // Replace with your MySQL password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
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
}
