package mnadroid.rnginvaders;

import android.util.Log;

class Bullet {
    private float x, y;
    private float speed;

    Bullet(int screenY, float startx, float starty, int length) {
        if (starty >= screenY/2)
            speed = -1 * length*50;
        else
            speed = length*50;

        x = startx;
        y = starty;
    }

    void bulletUpdate(long fps) {
        y += speed / fps;
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }
}
