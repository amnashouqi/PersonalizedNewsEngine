import db.UserManager;
import model.User;
import model.Article;
import scraper.Webscraper;
import java.util.concurrent.*;
import java.util.Scanner;
import static db.DBConnection.*;

public class Main {
    private static final ConcurrencyHandler concurrencyHandler = new ConcurrencyHandler(10); // Using a pool of 10 threads

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

        Runnable task = () -> {
            if (choice == 1) {
                handleRegistration(scanner);
            } else if (choice == 2) {
                handleLogin(scanner);
            }
        };

        // Submit the task and wait for its completion
        Future<?> future = concurrencyHandler.submitTask(task);
        try {
            future.get(); // Wait for task completion
        } catch (Exception e) {
            System.err.println("Error while executing task: " + e.getMessage());
            e.printStackTrace();
        }
        System.exit(0); // Terminates the program



    }

    private static void handleRegistration(Scanner scanner){
        System.out.print("What’s your name, O wise one? (Enter your username): ");
        String username = scanner.nextLine();
        System.out.print("Shh, keep it secret, keep it safe! (Enter your password): ");
        String password = scanner.nextLine();

        User user = new User(username, password);

        if (UserManager.registerUser(user)) {
            System.out.println("🎉 Yasss! Welcome aboard, "+username+"! You’re officially part of our news-hungry community!");
            System.out.println();

            concurrencyHandler.submitTask(() -> handleLogin(scanner));

        } else {
            System.out.println("Whoopsie-daisy! Something went wrong with your registration. Try again, and we’ll make it right!");
        }
    }

    private static void handleLogin(Scanner scanner){

        System.out.print("Enter your username to unlock your news: ");
        String username = scanner.nextLine();
        System.out.print("Your secret code, please: ");
        String password = scanner.nextLine();

        int userId = UserManager.loginUser(username, password);
        if (userId != -1) {
            System.out.println("It’s good to see you again "+username+"!. Your personalized news awaits!");
            Webscraper.scrapeArticles(userId); // Pass the userId to scrapeArticles
        } else {
            System.out.println("Oops! That didn’t work. Double-check your username and password, and try again!");
        }
        clearExistingNews();

        System.out.println();
        System.out.println("And that’s a wrap! Thanks for visiting, "+username+". Catch you on the flip side. 😎");


    }

    // Graceful shutdown of ExecutorService
//    private static void addShutdownHook() {
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            System.out.println("Shutdown hook triggered. Cleaning up...");
//            concurrencyHandler.shutdown();
//        }));
//    }


}