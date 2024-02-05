package org.progetto_ristorante.progetto_ristorante;

import java.util.List;

public class MenuObserverManager {
    protected List<CustomerController> observers; // menu observer list

    public MenuObserverManager(List<CustomerController> observers) { // constructor
        this.observers = observers;
    }


    public void addObserver(CustomerController observer) { // adds a customer to menu observer list
        observers.add(observer);
    }

    public void removeObserver(CustomerController observer) { // removes a customer from menu observer list
        observers.remove(observer);
    }


    public void notifyObserversMenuUpdate() { // notifies to each customer that the menu has been updated
        System.out.println("Avviso menu aggiornato agli stronzi");
        if (!observers.isEmpty()) {
            for (MenuObserver observer : observers) {
                observer.updateMenu();
            }
        }
    }

    public void notifyObserversMenuNotUpdated() { // notifies to each customer that the menu hasn't been updated
        System.out.println("Avviso menu non aggiornato agli stronzi");
        if (!observers.isEmpty()) {
            for (MenuObserver observer : observers) {
                observer.notifyMenuNotUpdate();
            }
        }
    }
}
