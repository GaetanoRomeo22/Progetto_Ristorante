package org.progetto_ristorante.progetto_ristorante;

public class SimpleOrderFactory implements OrderFactory {
    public Order createOrder(String name, float price) { // creates a menu's order with name and price
        return new Order(name,price);
    }
}
