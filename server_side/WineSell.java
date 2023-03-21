package server_side;

import java.util.List;

public class WineSell {

    private final Wine wine;
    private int quantity;
    private double value;

    public WineSell(Wine wine) {
        this.wine = wine;
    }

    public WineSell(Wine wine, int quantity, double value) {
        this.quantity = quantity;
        this.value = value;
        this.wine = wine;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Wine getWine() {
        return this.wine;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public double getValue() {
        return this.value;
    }

    public String serialize() {
        return wine.getId() + ":" + quantity + ":" + value;
    }

    public static WineSell deserialize(String string, List<Wine> wineList) {
        String[] content = string.split(":");
        Wine w = null;
        for (Wine tempWine : wineList) {
            if (tempWine.getId().equals(content[0])) {
                w = tempWine;
                break;
            }
        }
        return new WineSell(w, Integer.valueOf(content[1]), Double.parseDouble(content[2]));
    }
}
