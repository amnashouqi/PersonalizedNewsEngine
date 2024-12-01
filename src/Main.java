import db.UserManager;
import model.User;
import model.Article;
import scraper.Webscraper;
import scraper.Webscraper;

import java.util.Scanner;

import static db.DBConnection.clearExistingNews;


public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("Hold onto your hat! Welcome to your Personalized News Recommendation System! Let’s get you updated!");
        System.out.println();

        // Register or Login
        System.out.println("What’s your choice, adventurer? 🧙‍♂️ Are you here to register for the first time or login to your news kingdom?");
        System.out.println("1. Register\n2. Login");
        System.out.println();

        int choice = scanner.nextInt();
        scanner.nextLine();  // Consume newline
        System.out.println();

        if (choice == 1) {
            System.out.print("What’s your name, O wise one? (Enter your username): ");
            String username = scanner.nextLine();
            System.out.print("Shh, keep it secret, keep it safe! (Enter your password): ");
            String password = scanner.nextLine();

            User user = new User(username, password);

            if (UserManager.registerUser(user)) {
                System.out.println("🎉 Yasss! Welcome aboard, "+username+"! You’re officially part of our news-hungry community!");

                //carrying out the login process here for first timers

                System.out.println();
                System.out.println("Ready to dive back in? Let's login and jump into the latest news!");
                System.out.print("Welcome back, hero! Enter your username to unlock your news: ");
                username = scanner.nextLine();
                System.out.print("Your secret code, please. (We promise to keep it safe.): ");
                password = scanner.nextLine();

                int userId = UserManager.loginUser(username, password);
                if (userId != -1) {
                    System.out.println("Welcome " + username + "!");
                    Webscraper.scrapeArticles(userId); // Pass the userId to scrapeArticles
                } else {
                    System.out.println( "Oops! That didn’t work. Double-check your username and password, and try again!");
                }

            } else {
                System.out.println("Whoopsie-daisy! Something went wrong with your registration. Try again, and we’ll make it right!");
            }

            //repeating users
        } else if (choice == 2) {
            System.out.print("Welcome back, hero! Enter your username to unlock your news: ");
            String username = scanner.nextLine();
            System.out.print("Your secret code, please. (We promise to keep it safe): ");
            String password = scanner.nextLine();

            int userId = UserManager.loginUser(username, password);
            if (userId != -1) {
                System.out.println("Welcome back, "+username+"! It’s good to see you again. Your personalized news awaits!");
                Webscraper.scrapeArticles(userId); // Pass the userId to scrapeArticles
            } else {
                System.out.println("Oops! That didn’t work. Double-check your username and password, and try again!");
            }
            clearExistingNews();

            System.out.println();
            System.out.println("And that’s a wrap! Thanks for visiting, "+username+". Catch you on the flip side. 😎");
        }
    }
}
