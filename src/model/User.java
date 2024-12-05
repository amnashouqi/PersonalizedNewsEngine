package model;

import db.DBConnection;
import db.UserManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Scanner;


import static model.RecommendationSystem.*;

// UserInterface: Interface defining methods for user actions (like, dislike, skip, rate)
interface UserInterface {
    int getId();  // Get user ID
    String getUsername();  // Get username
    String getPassword();  // Get password
    void setPassword(String password);  // Set password

    void userLikes(int articleId);  // Like an article
    void userDislikes(int articleId);  // Dislike an article
    void userSkips(int articleId);  // Skip an article
    void userRates(int articleId);  // Rate an article
}

// User class implementing the UserInterface, encapsulating user details and actions
public class User implements UserInterface {
    private int id;  // Unique identifier for the user
    private String username;  // User's username
    private String password;  // User's password

    // Setters for username and id (use of encapsulation for data protection)
    public void setUsername(String username) {
        this.username = username;
    }


    public void setId(int id) {
        this.id = id;
    }

    // Getters (Encapsulation: Access private data via getters)
    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }
    @Override
    public void setPassword(String password) {
        this.password = password;
    }
    @Override
    public String getPassword() {
        return password;
    }

    // Constructor with ID and password (Use of constructor for object initialization)
    public User(int id, String username, String password){
        this.id=id;
        this.username=username;
        this.password=password;
    }

    // Overloaded constructor (Polymorphism: Same method signature with different parameters)
    public User(String username, String password){
        //next_id=id;
        this.username=username;
        this.password=password;
    }

    @Override
    public void userLikes(int articleId) {
        UserManager.userLikes(articleId);
    }

    @Override
    public void userDislikes(int articleId) {
        UserManager.userDislikes(articleId);
    }

    @Override
    public void userRates(int articleId) {
        UserManager.userRates(articleId);
    }

    @Override
    public void userSkips(int articleId) {
        UserManager.userSkips(articleId);
    }
}