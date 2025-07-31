package factory.model;

public abstract class Element {
    private final int id;

    public Element(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
