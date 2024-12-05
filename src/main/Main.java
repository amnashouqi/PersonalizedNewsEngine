package main;

import concurrency.ConcurrencyHandler;
import db.UserManager;
import model.User;
import scraper.Webscraper;
import java.util.concurrent.*;
import java.util.Scanner;
import static db.DBConnection.*;

public class Main {
    private static final ConcurrencyHandler concurrencyHandler = new ConcurrencyHandler(10); // Using a pool of 10 threads

    /**
     * The main entry point for the application. This method drives the user interaction and initiates tasks for registration or login.
     * OOP Principle: **Encapsulation** - The internal workings of the registration/login process are hidden from the user.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println();
        System.out.println("Hold onto your hat! Welcome to your Personalized News Recommendation System! Let’s get you updated!");
        System.out.println();

        try{
            // Register or Login
            System.out.println("What’s your choice, adventurer? 🧙‍♂️ Are you here to register for the first time or login to your news kingdom?");
            System.out.println("1. Register\n2. Login");
            System.out.println();

            int choice = getValidChoice(scanner);  // Input validation for choice
            System.out.println();

            Runnable task = () -> {
                try {
                    if (choice == 1) {
                        handleRegistration(scanner);  // Registration process
                    } else if (choice == 2) {
                        handleLogin(scanner);  // Login process
                    }
                } catch (Exception e) {
                    System.err.println("Error while executing task: " + e.getMessage());
                    e.printStackTrace();
                }
            };

            // Submit the task and wait for its completion (Concurrency: **Parallel Execution** with ExecutorService)
            Future<?> future = concurrencyHandler.submitTask(task);
            future.get(); // Wait for task completion

        } catch (Exception e) {
            System.err.println("Error while executing task: " + e.getMessage());
            e.printStackTrace();
        }finally {
            scanner.close(); // Ensure the scanner is closed to avoid resource leaks
        }

        System.exit(0); // Terminates the program after task completion
    }

    /**
     * Ensures that the user input is a valid choice (1 or 2) for registering or logging in.
     * OOP Principle: **Validation and Error Handling** - Ensures user input is valid and handles errors gracefully.
     * @param scanner the scanner to read user input
     * @return the valid choice from the user
     */
    private static int getValidChoice(Scanner scanner) {
        int choice = -1;
        while (choice != 1 && choice != 2) { // Validate the choice
            try {
                System.out.print("Enter your choice (1 or 2): ");
                choice = Integer.parseInt(scanner.nextLine());
                if (choice != 1 && choice != 2) {
                    System.out.println("Invalid choice. Please choose 1 or 2.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        return choice;
    }

    /**
     * Handles the user registration process.
     * OOP Principle: **Single Responsibility Principle (SRP)** - This method focuses solely on handling registration.
     * It uses the `UserManager` class for the actual registration logic.
     * @param scanner the scanner to read user input
     */
    private static void handleRegistration(Scanner scanner){
        System.out.print("What’s your name, O wise one? (Enter your username): ");
        String username = scanner.nextLine();
        System.out.print("Shh, keep it secret, keep it safe! (Enter your password): ");
        String password = scanner.nextLine();

        User user = new User(username, password);  // Create User object

        // Register the user using the UserManager
        if (UserManager.registerUser(user)) {
            System.out.println("🎉 Yasss! Welcome aboard, "+username+"! You’re officially part of our news-hungry community!");
            System.out.println();

            // Wait for the login task to complete after successful registration
            Future<?> future = concurrencyHandler.submitTask(() -> handleLogin(scanner));  // Handle login after registration
            try {
                future.get(); // Block until the login task completes
            } catch (Exception e) {
                System.err.println("Error during login after registration: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Whoopsie-daisy! Something went wrong with your registration. Try again, and we’ll make it right!");
        }
    }

    /**
     * Handles the user login process and fetches personalized news.
     * OOP Principle: **Abstraction** - Hides the complexity of login handling and news fetching from the user.
     * @param scanner the scanner to read user input
     */
    public static void handleLogin(Scanner scanner){

        System.out.print("Enter your username to unlock your news: ");
        String username = scanner.nextLine();
        System.out.print("Your secret code, please: ");
        String password = scanner.nextLine();

        int userId = UserManager.loginUser(username, password);  // User login validation
        if (userId != -1) {
            System.out.println("It’s good to see you again "+username+"! Your personalized news feed is being scraped!🕸");
            Webscraper.scrapeArticles(userId);  // Scrape articles based on user ID
            System.out.println();
        } else {
            System.out.println("Oops! That didn’t work. Double-check your username and password, and try again!");
        }
        clearExistingNews();  // Clear previous news (perhaps for user session or cleanup)
        System.out.println();
        System.out.println("And that’s a wrap! Thanks for visiting, "+username+". Catch you on the flip side. 😎");
    }

    // Graceful shutdown of ExecutorService
    // OOP Principle: **Resource Management** - Ensuring proper shutdown of resources.
    // private static void addShutdownHook() {
    //     Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    //         System.out.println("Shutdown hook triggered. Cleaning up...");
    //         concurrencyHandler.shutdown();
    //     }));
    // }
}
