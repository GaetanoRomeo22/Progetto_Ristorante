package org.progetto_ristorante.progetto_ristorante;

public class MenuContext {
    protected MenuState menuState; // menu's state (discounted or not)

    public MenuContext() { // constructor (initializes the menu as not discounted)
        this.menuState = new NotDiscountMenu();
    }

    public void setMenuState(MenuState menuState) { // updates menu's state
        this.menuState = menuState;
    }

    public MenuState getMenuState() { // returns menu's state (discounted or not)
        menuState.changeMenuState(this);
        return menuState;
    }
}
