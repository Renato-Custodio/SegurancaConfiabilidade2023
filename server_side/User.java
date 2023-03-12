package server_side;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private final String userID;
    private final ArrayList<WineSell> wines;
    private double amount;
    private Map<String, List<String>> messages;

    public User(String user) {
        this.wines = new ArrayList<>();
        this.userID = user;
        this.amount = 200;
        this.messages = new HashMap<>();
    }

    public User(String user, Wine wine) {
        this.wines = new ArrayList<>();
        this.userID = user;
        wines.add(new WineSell(wine));
        this.amount = 200;
        this.messages = new HashMap<>();
    }

    public String getName() {
        return this.userID;
    }

    public void sellWine(WineSell wine) {
        this.wines.add(wine);
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
            if (vinho.getWine().equals(new Wine(wineID))) {
                return vinho;
            }
        }

        return null;
    }

    public double getBalance() {
        return this.amount;
    }

    public void setBalance(Double amount) {
        this.amount = amount;
    }

    public String readMessages() {
        if (messages.isEmpty()) {
            return "Nao tem novas mesnagens";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : messages.entrySet()) {
            sb.append(entry.getKey() + ":\n");
            for (String message : entry.getValue()) {
                sb.append("\t" + message + "\n");
            }
        }
        messages.clear();
        return sb.toString();
    }

    public void reciveMessage(String name, String message) {
        if (messages.get(name) != null) {
            messages.get(name).add(message);
        } else {
            List<String> tempList = new ArrayList<>();
            tempList.add(message);
            messages.put(name, tempList);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof User)) {
            return false;
        }

        User user = (User) o;
        return this.userID.equals(user.userID);
    }
}
