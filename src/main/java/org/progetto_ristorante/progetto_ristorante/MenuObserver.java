package org.progetto_ristorante.progetto_ristorante;

public interface MenuObserver {
    void updateMenu(boolean isMenuUpdated);
    void notifyMenuNotUpdate();
}
