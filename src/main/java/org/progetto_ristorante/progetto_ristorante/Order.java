package org.progetto_ristorante.progetto_ristorante;

public class Order {

    protected final String name; // order's name
    protected final float price; // order's price

    public Order (String name, float price) { // default constructor
        this.name = name;
        this.price = price;
    }

    public String getName() { // returns order's name
        return name;
    }

    public float getPrice() { // returns order's price
        return price;
    }

    public String toString() { // returns the order as a string (override of the method toString)
        return String.format("%-30s%10.2f €", name, price);
    }
}
