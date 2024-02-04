package org.progetto_ristorante.progetto_ristorante;

import java.util.List;

public class MenuObserverManager {
    protected List<CustomerController> observers;

    // Metodo per aggiungere un osservatore

    public MenuObserverManager(List<CustomerController> observers) {
        this.observers = observers;
    }


    public void addObserver(CustomerController observer) {
        System.out.println("Aggiungo Customer");
        observers.add(observer);
    }

    public void removeObserver(CustomerController observer) {
        observers.remove(observer);
    }


    // Metodo per notificare tutti gli osservatori che il menu è stato aggiornato
    public void notifyObserversMenuUpdate() {
        System.out.println("Avviso menu aggiornato agli stronzi");
        if (!observers.isEmpty()) {
            for (MenuObserver observer : observers) {
                observer.updateMenu();
            }
        } else {
            System.out.println("Lista vuota");
        }
    }

    // Metodo per notificare tutti gli osservatori che il menu non è stato aggiornato
    public void notifyObserversMenuNotUpdated() {
        System.out.println("Avviso menu non aggiornato agli stronzi");
        if (!observers.isEmpty()) {
            for (MenuObserver observer : observers) {
                observer.notifyMenuNotUpdate();
            }
        } else {
            System.out.println("Lista vuota");
        }
    }
}
