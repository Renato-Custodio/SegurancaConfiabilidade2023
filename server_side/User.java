package server_side;

import java.util.ArrayList;

public class User {
    private final String userID;
    private final ArrayList<Wine> wines = new ArrayList<>();
    private double amount;

    public User(String user) {
        this.userID = user;
    }

    public User(String user, String wine) {
        this.userID = user;
        wines.add(new Wine(wine));
    }

    public String getUser() {
        return this.userID;
    }

    public void addWine(String wine) {
        this.wines.add(new Wine(wine));
    }

    public ArrayList<Wine> getWines() {
        return this.wines;
    }

    public double getBalance() {
        return this.amount;
    }
}
