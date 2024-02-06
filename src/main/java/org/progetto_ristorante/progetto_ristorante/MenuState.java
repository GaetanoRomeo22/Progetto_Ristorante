package org.progetto_ristorante.progetto_ristorante;

import javafx.collections.ObservableList;

public interface MenuState {
    void changeMenuState(MenuContext menuContext); // changes the menu from discounted and full price and vice versa
    ObservableList<Order> getMenu (); // returns the menu
}
