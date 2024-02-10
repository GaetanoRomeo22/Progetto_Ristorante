package org.progetto_ristorante.progetto_ristorante;

import java.util.ArrayList;
import java.util.List;

public class MenuOriginator {
    private List<Order> initialMenuState = new ArrayList<>(); // initial menu's state

    public void setMenu(List<Order> menuState) { // sets menu's current state
        this.initialMenuState = menuState;
    }

    public List<Order> getMenu() { // returns menu's current state
        return this.initialMenuState;
    }

    public MenuMemento saveMenuState() { // creates a new menu's state
        return new ConcreteMenuMemento();
    }

    public class ConcreteMenuMemento implements MenuMemento {
        protected List<Order> currentMenuState; // current menu's state

        public ConcreteMenuMemento() { // constructor (creates a menu's state)
            this.currentMenuState = initialMenuState;
        }

        public void restoreMenu() { // restores previous menu's state
            currentMenuState = new ArrayList<>(initialMenuState);
        }
    }
}
