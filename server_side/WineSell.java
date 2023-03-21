package server_side;

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

    // ir buscar os vinho à lista e criar um wineSell
    public static String[] deserialize(String string) {
        return string.split(":");
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

    public static void main(String[] args) {
        Wine wine = new Wine("wine");
        WineSell sell = new WineSell(wine, 1, 20);
        System.out.println(sell.serialize());
        WineSell.deserialize(null);
    }

}
