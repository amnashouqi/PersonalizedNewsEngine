import db.UserManager;
import model.User;
import scraper.Webscraper;
import scraper.Webscraper;

import java.util.Scanner;

import static scraper.Webscraper.clearExistingNews;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("Welcome to your Personalized New Recommendation System!");
        System.out.println();

        // Register or Login
        System.out.println("1. Register\n2. Login");
        System.out.println();

        int choice = scanner.nextInt();
        scanner.nextLine();  // Consume newline
        System.out.println();

        if (choice == 1) {
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            User user = new User(username, password);

            if (UserManager.registerUser(user)) {
                System.out.println("Congrats "+username+"! You have registered to our system successfully!");

                //carrying out the login process here for first timers

                System.out.println();
                System.out.println("Let's login now to get started!");
                System.out.print("Enter username: ");
                username = scanner.nextLine();
                System.out.print("Enter password: ");
                password = scanner.nextLine();

                int userId = UserManager.loginUser(username, password);
                if (userId != -1) {
                    System.out.println("Welcome " + username + "!");
                    Webscraper.scrapeArticles(userId); // Pass the userId to scrapeArticles
                } else {
                    System.out.println("Login failed.");
                }

            } else {
                System.out.println("Registration failed.");
            }

            //repeating users
        } else if (choice == 2) {
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            int userId = UserManager.loginUser(username, password);
            if (userId != -1) {
                System.out.println("Welcome " + username + "!");
                Webscraper.scrapeArticles(userId); // Pass the userId to scrapeArticles
            } else {
                System.out.println("Login failed.");
            }
            clearExistingNews();
            System.out.println("cleaning outside the loop");

            System.out.println();
            System.out.println("Thank you! See ya soon. 😎");
        }
    }
}
