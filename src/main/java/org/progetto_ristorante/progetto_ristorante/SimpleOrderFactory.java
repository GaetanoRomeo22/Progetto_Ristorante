package org.progetto_ristorante.progetto_ristorante;

public class SimpleOrderFactory implements OrderFactory {
    public ConcreteOrder createOrder(String name, float price, String category) { // creates a menu's order with name and price
        return new ConcreteOrder(name, price, category);
    }
}
