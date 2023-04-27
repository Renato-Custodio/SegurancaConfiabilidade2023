package server_side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;

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

    public Map<String, List<String>> readMessages() {
        Map<String, List<String>> temp = new HashMap<>(messages);
        messages.clear();
        return temp;
    }

    public void receiveMessage(String name, String message) {
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
        Iterator it = wines.iterator();
        while (it.hasNext()) {
            WineSell wineSell = (WineSell) it.next();
            if (it.hasNext()) {
                string.append(wineSell.serialize() + "\\");
            } else {
                string.append(wineSell.serialize());
            }
        }
        return this.userID + "&" + string.toString() + "&" + amount + "&" + serializeMessage();
    }

    private String serializeMessage() {
        StringBuilder message = new StringBuilder();
        Iterator it = messages.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            message.append(entry.getKey() + "%");
            List<String> messageList = (List<String>) entry.getValue();
            for (String string : messageList) {
                message.append(string);
                if (messageList.indexOf(string) < messageList.size() - 1) {
                    message.append("!");
                }
            }
            if (it.hasNext()) {
                message.append("\\");
            }
        }
        return message.toString();
    }

    public static User deserialize(String string, List<Wine> wineList) {
        String[] content = string.split("&");
        User user = new User(content[0]);
        if (!content[1].equals(""))
            user.setWines(deserializeWineSell(content[1], wineList));
        user.setBalance(Double.parseDouble(content[2]));
        if (content.length > 3)
            user.setMessages(deserializeMessages(content[3]));
        return user;
    }

    private static ArrayList<WineSell> deserializeWineSell(String string, List<Wine> wineList) {
        ArrayList<WineSell> list = new ArrayList<>();
        if (string.contains("\\")) {

            for (String wine : string.split("\\\\")) {
                WineSell tempSell = WineSell.deserialize(wine, wineList);
                list.add(tempSell);
            }
        } else {
            list.add(WineSell.deserialize(string, wineList));
        }
        return list;

    }

    private static Map<String, List<String>> deserializeMessages(String string) {
        Map<String, List<String>> msg = new HashMap<>();

        for (String temp : string.split("\\\\")) {
            String message = temp.split("%")[1];
            List<String> list = new LinkedList<>();
            for (String string2 : message.split("!")) {
                list.add(string2);
            }
            msg.put(temp.split("%")[0], list);
        }

        return msg;
    }
}
