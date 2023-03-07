package server_side;

import java.util.ArrayList;

public class User {
    private String userID;
    private ArrayList<Wine> wines = new ArrayList<>();

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
}
