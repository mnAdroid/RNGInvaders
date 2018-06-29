package mnadroid.rnginvaders;

import android.util.Log;

class Player {
    //Wie viele Leben hat der Spieler
    private int hitpoints;

    //Wo befindet sich das Schiff des Spielers
    private float shipX, shipY;
    //Wie schnell bewegt sich so ein Schiff
    private float speed;

    Player(float startX, float startY, float speed) {
        hitpoints = 100;
        shipX = startX;
        shipY = startY;
        this.speed = speed * 28;
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

        float speedX, speedY;

        speedX = (dx / (Math.abs(dx) + Math.abs(dy)))*speed;
        speedY = (dy / (Math.abs(dx) + Math.abs(dy)))*speed;

        Log.d("speed", "" + speed);
        Log.d("dx importance", "" + (dx / (Math.abs(dx) + Math.abs(dy))));
        Log.d("dy importance", "" + (dy / (Math.abs(dx) + Math.abs(dy))));
        Log.d("speedCalc", "" + ((dx / (Math.abs(dx) + Math.abs(dy)))*speed + (dy / (Math.abs(dx) + Math.abs(dy)))*speed));

        //Wenn es nicht weit weg ist kann man einfach auf Finger setzen
        if (Math.abs(dx) + Math.abs(dy) <= speed/fps) {
            shipX = fingerX;
            shipY = fingerY;
            return;
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
}
