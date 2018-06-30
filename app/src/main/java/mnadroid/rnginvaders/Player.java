package mnadroid.rnginvaders;

import android.graphics.Color;

class Player {
    //Wie viele Leben hat der Spieler
    private int hitpoints;

    //Wo befindet sich das Schiff des Spielers
    private float shipX, shipY;
    //Wie schnell bewegt sich so ein Schiff
    private float speed;

    //Ist der Spieler bereit ins Spiel zu gehen?
    private boolean ready;

    //Wie oft hat der Spieler in dem Match gewonnen?
    private int winCount;

    private int shipColor;
    private boolean shipColorPicker;

    Player(float startX, float startY, float speed, int color) {
        hitpoints = 100;
        shipX = startX;
        shipY = startY;
        this.speed = speed * 28;
        ready = false;
        winCount = 0;
        shipColor = color;
        shipColorPicker = false;
    }

    void resetRound(float startX, float startY) {
        hitpoints = 100;
        shipX = startX;
        shipY = startY;
        shipColorPicker = false;
    }
    int getHitpoints() {
        return hitpoints;
    }

    void setHitpoints(int hitpoints) {
        this.hitpoints += hitpoints;
    }

    void updateShip(float fingerX, float fingerY, long fps) {
        //Soviel muss ich mich insgesamt bewegen
        float dx = shipX - fingerX;
        float dy = shipY - fingerY;

        //Normalisierung der dx und dy damit insgesamt immer
        //maximal speed bewegt wird und es keine random jumps gibt
        float speedX, speedY;

        speedX = (dx / (Math.abs(dx) + Math.abs(dy)))*speed;
        speedY = (dy / (Math.abs(dx) + Math.abs(dy)))*speed; //speedX + speedY = speed

        //Wenn es nicht weit weg ist kann man einfach auf Finger setzen
        if (Math.abs(dx) + Math.abs(dy) <= speed/fps) {
            shipX = fingerX;
            shipY = fingerY;
        }
        else {
            shipX -= speedX/fps;
            shipY -= speedY/fps;
        }
    }

    float getShipX() {
        return shipX;
    }

    float getShipY() {
        return shipY;
    }

    boolean getReady() {
        return ready;
    }

    void setReady() {
        ready = !ready;
    }

    int getWinCount() {
        return winCount;
    }

    void setWinCount() {
        winCount++;
    }

    int getShipColor() {
        return shipColor;
    }

    void setShipColor(int color) {
        shipColor = color;
    }

    boolean getShipColorPicker() {
        return getShipColorPicker();
    }

    void setShipColorPicker() {
        shipColorPicker = !shipColorPicker;
    }
}
