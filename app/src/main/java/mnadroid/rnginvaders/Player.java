package mnadroid.rnginvaders;

class Player {
    private int hitpoints;

    Player() {
        hitpoints = 100;
    }

    int getHitpoints() {
        return hitpoints;
    }

    void setHitpoints(int hitpoints) {
        this.hitpoints += hitpoints;
    }
}
