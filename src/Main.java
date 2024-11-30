import db.UserManager;
import model.User;
import scraper.Webscraper;
import scraper.Webscraper;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("Welcome to your Personalized New Recommendation System!");
        System.out.println();

        // Register or Login
        System.out.println("1. Register\n2. Login");
        int choice = scanner.nextInt();
        scanner.nextLine();  // Consume newline

        if (choice == 1) {
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            User user = new User(username, password);

            if (UserManager.registerUser(user)) {
                System.out.println("User registered successfully!");
                System.out.println();
                System.out.println("Let's login now to get started!");
                System.out.print("Enter username: ");
                username = scanner.nextLine();
                System.out.print("Enter password: ");
                password = scanner.nextLine();

                if (UserManager.loginUser(username, password)) {
                    System.out.println("Login successful!");
                    Webscraper.scrapeArticles();  // Scrape articles when logged in
                } else {
                    System.out.println("Login failed.");
                }

            } else {
                System.out.println("Registration failed.");
            }
        } else if (choice == 2) {
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            if (UserManager.loginUser(username, password)) {
                System.out.println("Login successful!");
                Webscraper.scrapeArticles();  // Scrape articles when logged in
            } else {
                System.out.println("Login failed.");
            }
        }
    }
}
