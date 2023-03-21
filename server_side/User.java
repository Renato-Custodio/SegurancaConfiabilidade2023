package server_side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class User {
    private final String userID;
    private ArrayList<WineSell> wines;
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

    private void setWines(ArrayList<WineSell> wines) {
        this.wines = wines;
    }

    private void setMessages(Map<String, List<String>> messages) {
        this.messages = messages;
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

    public String serialize() {
        StringBuilder string = new StringBuilder();
        for (WineSell wineSell : wines) {
            string.append(wineSell.serialize() + "\\");
        }

        return this.userID + "&" + string.toString() + "&" + amount + "&" + serializeMessage();
    }

    private String serializeMessage() {
        StringBuilder message = new StringBuilder();
        Iterator it = messages.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            message.append(entry.getKey() + "%");
            if (it.hasNext()) {
                entry = (Map.Entry) it.next();
                message.append(entry.getValue().toString() + "\\");
            } else {
                message.append(entry.getValue().toString());
            }
        }
        return message.toString();
    }

    public static User deserialize(String string) {
        String[] content = string.split("&");
        User user = new User(content[0]);
        String[] wineSells = content[1].split(" *\\ *"); // TODO deserialize wineSell
        user.setWines(null); // todo
        user.setBalance(Double.parseDouble(content[2]));
        user.setMessages(deserializeMessages(content[3]));
        return user;
    }

    private static Map<String, List<String>> deserializeMessages(String string) {
        Map<String, List<String>> msg = new HashMap<>();
        if (string.contains("\\")) {
            for (String temp : string.split(" *\\ *")) {
                msg.put(string.split(" *% *")[0], Arrays.asList(string.split(" *% *")[1]));
            }
        } else {
            msg.put(string.split(" *% *")[0], Arrays.asList(string.split(" *% *")[1]));
        }
        return msg;
    }

    public static void main(String[] args) {
        User user = new User("roberto");
        user.wines.add(new WineSell(new Wine("joana")));
        user.wines.add(new WineSell(new Wine("ines")));
        List<String> list = new ArrayList<String>();
        list.add("ines");
        list.add("carlos");
        user.messages.put("user", list);
        user.messages.put("alberto", list);
        System.out.println(user.serialize());

        User rec = User.deserialize(user.serialize());
        System.out.println(rec.userID + " " + user.amount + " " + user.messages);

    }
}
