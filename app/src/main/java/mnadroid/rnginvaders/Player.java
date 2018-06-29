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
        this.speed = speed * 14;
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

        Log.d("dx", "" + dx);
        Log.d("dy", "" + dy);
        Log.d("dx/dy", "" + dx/dy);
        Log.d("dy/dx", "" + dy/dx);
        Log.d("speed", "" + speed);
        Log.d("speedCalc", "" + (Math.abs(speed * (dx/dy)/speed)));

        //Wenn es nicht weit weg ist kann man einfach auf Finger setzen
        if (Math.abs(dx) + Math.abs(dy) <= speed/fps) {
            shipX = fingerX;
            shipY = fingerY;
            return;
        }
        //Sonst wird so weit bewegt wir pro frame möglich
        if (dx < 0) {
            shipX += speed/fps;
        }
        else {
            shipX -= speed/fps;
        }

        if (dy < 0) {
            shipY += speed/fps;
        }
        else {
            shipY -= speed/fps;
        }
        //Wenn nur x sehr nah dran ist setzen wir nur X auf Finger
        if (Math.abs(dx) <= speed/fps) {
            shipX = fingerX;
        }
        //Wenn nur y sehr nah dran ist setzn wir nur Y auf Finger
        if (Math.abs(dy) <= speed/fps) {
            shipY = fingerY;
        }
    }

    float getShipX() {
        return shipX;
    }

    float getShipY() {
        return shipY;
    }
}
