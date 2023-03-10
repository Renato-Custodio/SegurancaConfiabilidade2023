package server_side;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final String userID;
    private final ArrayList<WineSell> wines;
    private double amount;

    public User(String user) {
        this.wines = new ArrayList<>();
        this.userID = user;
        this.amount = 200;
    }

    public User(String user, Wine wine) {
        this.wines = new ArrayList<>();
        this.userID = user;
        wines.add(new WineSell(wine));
        this.amount = 200;
    }

    public String getName() {
        return this.userID;
    }

    public void addWine(Wine wine) {
        this.wines.add(new WineSell(wine));
    }

    public List<WineSell> getWines() {
        return this.wines;
    }

    // acho que temos de mudar isto
    /**
     * @param wineID
     * @requires this User to have a Wine with Wine.getID().equals(wineID)
     * @return
     */
    public WineSell getWine(String wineID) {
        for (WineSell vinho : this.wines) {
            if (vinho.getWine().equals(wineID)) {
                return vinho;
            }
        }

        return null;
    }

    public double getBalance() {
        return this.amount;
    }
}
