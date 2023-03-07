package server_side;

public class Wine {
    private String id;
    private int quantity;
    private boolean sell;
    private double value;
    private int stars;

    public Wine(String wine) {
        this.id = wine;
    }

    public String getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isSell() {
        return sell;
    }

    public void setSell(boolean sell) {
        this.sell = sell;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }

    public void setClassification(int stars) {
        this.stars = stars;
    }

    public int getClassification() {
        return this.stars;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Wine)) {
            return false;
        }

        Wine wine = (Wine) o;

        return this.id.equals(wine.getId());
    }
}
