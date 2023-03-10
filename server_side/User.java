package server_side;

import java.util.ArrayList;

public class User {
    private final String userID;
    private final ArrayList<Wine> wines = new ArrayList<>();
    private double amount;

    public User(String user) {
        this.userID = user;
        this.amount = 200;
    }

    public User(String user, String wine) {
        this.userID = user;
        wines.add(new Wine(wine));
        this.amount = 200;
    }

    public String getName() {
        return this.userID;
    }

    public void addWine(String wine) {
        this.wines.add(new Wine(wine));
    }

    public ArrayList<Wine> getWines() {
        return this.wines;
    }

    /**
     * @param wineID
     * @requires this User to have a Wine with Wine.getID().equals(wineID)
     * @return
     */
    public Wine getWine(String wineID) {
        for (Wine vinho : this.wines) {
            if (vinho.getId().equals(wineID)) {
                return vinho;
            }
        }

        return null;
    }

    public double getBalance() {
        return this.amount;
    }
}
