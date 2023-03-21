package server_side;

import java.util.ArrayList;

public class Wine {
    private final String id;
    private ArrayList<Integer> stars;

    public Wine(String wine) {
        this.stars = new ArrayList<>();
        this.id = wine;
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

    public static Wine deserialize(String string) {
        String[] content = string.split(":");
        String id = content[0];
        ArrayList<Integer> list = new ArrayList<>();
        for (String str : content[1].replace("[", "")
                .replace("]", "")
                .split(" *, *")) {
            list.add(Integer.valueOf(str));
        }
        Wine wine = new Wine(id);
        wine.setStars(list);
        return wine;
    }

    private void setStars(ArrayList<Integer> stars) {
        this.stars = stars;
    }

    public String serialize() {
        return id + ":" + stars.toString();
    }
}
