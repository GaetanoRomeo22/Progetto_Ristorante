package org.progetto_ristorante.progetto_ristorante;

public class Order {

    // private variables
    protected final String name;
    protected final float price;

    // default constructor
    public Order (String name, float price) {
        this.name = name;
        this.price = price;
    }

    // returns order's name
    public String getName() {
        return name;
    }

    // returns order's price
    public float getPrice() {
        return price;
    }

    // returns the order as a string (override of the method toString)
    public String toString() {
        return String.format("%-30s%10.2f €", name, price);
    }
}
