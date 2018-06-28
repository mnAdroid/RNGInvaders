package mnadroid.rnginvaders;

import android.util.Log;

class Bullet {
    private float x, y;
    private float speed;

    Bullet(int screenY, float startx, float starty, long fps, int length) {
        fps = 24;

        if (starty >= screenY/2)
            speed = -1 * length / fps;
        else
            speed = length / fps;

        Log.d("Speed", "" + speed);
        x = startx;
        y = starty;
    }

    void bulletUpdate() {
        y += speed;
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }
}
