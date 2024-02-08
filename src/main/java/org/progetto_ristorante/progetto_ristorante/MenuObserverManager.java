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

    public void notifyObserversMenuUpdate() {
        if (!observers.isEmpty()) {
            for (CustomerController observer : observers) {
                observer.updateMenu(true);
            }
        }
    }
}
