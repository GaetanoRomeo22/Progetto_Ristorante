package org.progetto_ristorante.progetto_ristorante;

public interface OrderFactory {
    ConcreteOrder createOrder(String name, float price, String category); // creates an order with name and price
}
