package server_side;

import java.util.ArrayList;

public class Wine {
    private final String id;

    private ArrayList<Integer> stars;

    public Wine(String wine) {
        this.id = wine;
        this.stars = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setClassification(int stars) {
        this.stars.add(stars);
    }

    public Double getClassificationAvarage() {
        if (this.stars.isEmpty()) {
            return 0.0;
        }

        return this.stars.stream().mapToInt(Integer::intValue).sum() / (double) this.stars.size();
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

    public class WineSell extends Wine {
        private int quantity;
        private double value;

        public WineSell(String wine, int quantity, double value) {
            super(wine);
            this.quantity = quantity;
            this.value = value;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getValue() {
            return this.value;
        }

    }
}
