package mnadroid.rnginvaders;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import static mnadroid.rnginvaders.BitmapCalculations.getScaledBitmapSize;
import static mnadroid.rnginvaders.BitmapCalculations.getScaledCoordinates;

class GameView extends SurfaceView implements Runnable {

    //Context abspeichern
    private Context fullContext;

    //Thread der Gameloop ist
    private Thread gameThread;

    //Surfaceholder braucht man um im Thread zu malen
    private SurfaceHolder ourHolder;

    //Standard Canvas und Paint Objekte (zum Malen verwendet)
    private Canvas canvas; //wodrauf wird gemalt
    private Paint paint; //was wird gemalt

    //Bildschirmgröße (zum Normalisieren aller Zeichnungen)
    private int screenX, screenY;

    //Wie groß muss das Rechteck sein
    private int rectSize;

    //Texte
    private int textSize, textSizeBig, scoreX, scoreY;

    //FPS
    //Um gleichmäßige Animationen auf allen Handys zu haben
    private long fps;

    //Boolean ob Spiel gerade läuft oder gepaused ist
    //volatile ist quasi ein synchronised ohne overhead
    private volatile boolean isPlaying;

    //Sind alle Elemente geladen
    private boolean loaded;
    //Ist gerade Pause?
    private boolean pause;

    //Musiksettings
    private boolean musicOn;
    private boolean soundOn;
    private SoundPool soundPool; //Soundeffekte nutzen
    private MediaPlayer backgroundLoop; //Hintergrundmusik
    private int shoot = -1; //Shootsound

    private static final int INVALID_POINTER_ID = -1;

    //Ort der letzten Berührungen auf dem Bildschirm
    private float touchX1_finger1, touchY1_finger1;
    private float touchX1_finger2, touchY1_finger2;
    private int finger1_index = INVALID_POINTER_ID;
    private int finger2_index = INVALID_POINTER_ID;

    //Spielerobjekte
    private Player player1, player2;
    private int lastPlayerWon;

    //Jeder Spieler schiesst derzeit zum selben Zeitpunkt
    //alle 0,25 Sekunden
    private long bulletTimer;
    //Wie groß sind die Bullets?
    private int bulletLength, bulletWidth;

    //BulletArray
    //Jeder Spieler bekommt 32 Plätze zum Schiessen
    private Bullet[] player1Bullets;
    private Bullet[] player2Bullets;
    private static final int BULLET_COUNT = 32;

    //Konstruktor
    GameView(Context context, int screenX, int screenY) {
        super(context);
        //Default Variablenbelegung
        gameThread = null;
        fps = 0;

        //Context abspeichern
        fullContext = context;

        //Bildschirmkoordinaten
        this.screenX = screenX;
        this.screenY = screenY;

        //Initialisierung der "Zeichen" Objekte
        ourHolder = getHolder();
        paint = new Paint();

        //Spieler erzeugen
        player1 = new Player();
        player2 = new Player();
        lastPlayerWon = -1;
        player1Bullets = new Bullet[BULLET_COUNT];
        player2Bullets = new Bullet[BULLET_COUNT];
    }

    //App wird (wieder) gestartet
    void resume() {
        isPlaying = true;

        //gespeicherte Variablen auslesen
        getSharedPreferences();

        //Gamethread starten
        gameThread = new Thread(this);
        gameThread.start();

        //Laden der Grafiken und Sounds
        initialiseGrafics();
        initialiseSound();

        //Spiel geht los wenn alles geladen ist
        loaded = true;
    }


    //App wird geschlossen
    void pause() {
        isPlaying = false;

        //Variablen abspeichern
        setSharedPrefernces();

        //Dem Garbagecollector helfen
        recycle();

        //Jetzt ist nicht mehr alles geladen
        loaded = false;

        //GameThread (Endlosloop) beenden
        try {
            gameThread.join();
        } catch(InterruptedException e) {
            Log.e("Error: ", "joining thread");
        }
    }

    //Handy-Zurück-Button wird gedrückt
    void onBackPressed() {
        //Pause
        pause = true;

        //Variablen abspeichern
        setSharedPrefernces();

        //Pause Screen noch zeichnen
    }

    //run() ist quasi eine Endlosschleife (solange das Game läuft) in dem alles passiert
    @Override
    public void run() {
        while (isPlaying) {
            if (!loaded) {
                continue;
            }

            //Start des Frames wird gespeichert
            long startFrameTime = System.currentTimeMillis();

            //Alles wird gezeichnet
            draw();

            //Alle Berechnungen werden begangen
            update();

            //FPS Berechnung
            long timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame > 0) {
                fps = 1000 / timeThisFrame;
            }
        }
    }

    //Gespeicherte Variablen auslesen
    private void getSharedPreferences() {
        //Wo sind die Variablen abgespeichert?
        SharedPreferences sharedPreferences = fullContext.getSharedPreferences("AidsInvaders", 0);
        musicOn = sharedPreferences.getBoolean("musicOn", false);
        soundOn = sharedPreferences.getBoolean("soundOn", false);

    }

    //Variablen Speichern
    private void setSharedPrefernces() {
        //Wohin speichern wir die Variablen?
        SharedPreferences sharedPreferences = fullContext.getSharedPreferences("AidsInvaders", 0);
        //Editor initialisieren (um Daten zu speichern)
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //Editor abspeichern
        editor.apply();
    }

    //Grafiken einlesen
    private void initialiseGrafics() {
        //Size of Playership
        rectSize = getScaledBitmapSize(screenX, 1080, 100);

        //Size of Bullets
        bulletWidth = getScaledBitmapSize(screenX, 1080, 25);
        bulletLength = getScaledBitmapSize(screenY, 1920, 75);

        //Starting positions for ships
        touchX1_finger1 = screenX/2;
        touchY1_finger1 = 0;

        touchX1_finger2 = screenX/2;
        touchY1_finger2 = screenY;

        //Textproperties
        textSize = getScaledBitmapSize(screenX, 1080, 50);
        textSizeBig = getScaledBitmapSize(screenX, 1080, 100);

        scoreX = getScaledCoordinates(screenX, 1080, screenX/2);
        scoreY = getScaledCoordinates(screenY, 1920, screenY/2);

        paint.setTextAlign(Paint.Align.CENTER);
    }

    //Alle Berechnungen der App
    private void update() {
        //Neue Schüsse erstellen SHOOT!
        if (System.currentTimeMillis() - bulletTimer >= 500) { //Alle 250 Millisekunden wird zurück geschossen
            boolean tmp1 = false;
            boolean tmp2 = false;
            for (int i = 0; i < BULLET_COUNT; i++) {
                if (player1Bullets[i] == null && !tmp1) {
                    player1Bullets[i] = new Bullet(screenY, touchX1_finger1, touchY1_finger1, bulletLength);
                    tmp1 = true;
                }
                if (player2Bullets[i] == null && !tmp2) {
                    player2Bullets[i] = new Bullet(screenY, touchX1_finger2, touchY1_finger2 - rectSize, bulletLength);
                    tmp2 = true;
                }
                if (tmp1 && tmp2) {
                    bulletTimer = System.currentTimeMillis();
                    break;
                }
            }
        }

        //Bullets bewegen sich
        for (int i = 0; i < BULLET_COUNT; i++) {
            if (player1Bullets[i] != null)
                player1Bullets[i].bulletUpdate(fps);
            if (player2Bullets[i] != null)
                player2Bullets[i].bulletUpdate(fps);
        }

        //Collision Detection
        for (int i = 0; i < BULLET_COUNT; i++) {
            //Bullet von Player1 kommen von oben
            if (player1Bullets[i] != null && player1Bullets[i].getY() >= screenY + bulletLength) { //Unten raus
                player1Bullets[i] = null;
            }
            //Spieler 2 getroffen?
            if (player1Bullets[i] != null && player1Bullets[i].getY() >= touchY1_finger2 - rectSize - bulletLength && player1Bullets[i].getY() < touchY1_finger2 + rectSize) { //Gegner Höhe getroffen
                if (player1Bullets[i].getX() >= touchX1_finger2 - rectSize - bulletWidth && player1Bullets[i].getX() < touchX1_finger2 + rectSize) { //Gegner Breite getroffen
                    player1Bullets[i] = null;
                    player2.setHitpoints(-10);
                }
            }

            //Bullet von Player2 kommen von unten
            if (player2Bullets[i] != null && player2Bullets[i].getY() < -bulletLength) { //Oben raus
                player2Bullets[i] = null;
            }
            //Spieler 1 getroffen?
            if (player2Bullets[i] != null && player2Bullets[i].getY() >= touchY1_finger1 - rectSize - bulletLength && player2Bullets[i].getY() < touchY1_finger1 + rectSize) { //Gegner Höhe getroffen
                if (player2Bullets[i].getX() >= touchX1_finger1 - rectSize - bulletWidth && player2Bullets[i].getX() < touchX1_finger1 + rectSize) { //Gegner Breite getroffen
                    player2Bullets[i] = null;
                    player1.setHitpoints(-10);
                }
            }
        }

        //died player 1?
        if (player1.getHitpoints() <= 0) {
            player1.resetHitpoints();
            player2.resetHitpoints();
            lastPlayerWon = 1;
        }
        //died player 2?
        if (player2.getHitpoints() <= 0) {
            player1.resetHitpoints();
            player2.resetHitpoints();
            lastPlayerWon = 2;
        }
    }

    //Alle Zeichnungen der App
    private void draw() {
        //Standardfehlerabfangen
        if (ourHolder.getSurface().isValid()) {
            try {
                //canvas wird das Zeichenobjekt
                canvas = ourHolder.lockCanvas();
            } catch (IllegalArgumentException e) {
                ourHolder.unlockCanvasAndPost(canvas);
                Log.e("UnlockCanvasError", e.toString());
            }
            //Tatsächliches Zeichnen
            try {
                //Hintergrundbild
                paint.setColor(Color.argb(255, 255, 255, 255));
                canvas.drawPaint(paint);

                //Spielerobjekte
                paint.setColor(Color.argb(255, 0, 0, 0));
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawRect(touchX1_finger1 - rectSize, touchY1_finger1 - rectSize, touchX1_finger1 + rectSize, touchY1_finger1 + rectSize, paint);
                canvas.drawRect(touchX1_finger2 - rectSize, touchY1_finger2 - rectSize, touchX1_finger2 + rectSize, touchY1_finger2 + rectSize, paint);

                //Bullets
                for (int i = 0; i < BULLET_COUNT; i++) {
                    if (player1Bullets[i] != null) {
                        paint.setColor(Color.argb(255, 0, 0, 200));
                        canvas.drawRect(player1Bullets[i].getX(), player1Bullets[i].getY(), player1Bullets[i].getX() + bulletWidth, player1Bullets[i].getY() + bulletLength, paint);
                    }
                    if (player2Bullets[i] != null) {
                        paint.setColor(Color.argb(255, 200, 0, 0));
                        canvas.drawRect(player2Bullets[i].getX(), player2Bullets[i].getY(), player2Bullets[i].getX() + bulletWidth, player2Bullets[i].getY() + bulletLength, paint);
                    }

                }
                //Textfarbe
                paint.setColor(Color.argb(255, 100, 100, 100));

                //Score
                if (player1 != null && player2 != null) {
                    paint.setTextSize(textSizeBig);
                    canvas.drawText("" + player1.getHitpoints() + " vs " + player2.getHitpoints(), scoreX, scoreY, paint);
                }

                //is there a recent winner?
                if(lastPlayerWon > 0) {
                    paint.setTextSize(textSize);
                    canvas.drawText("Player " + lastPlayerWon + " won!", scoreX, scoreY + bulletLength, paint);
                }

            } catch (NullPointerException e) {
                Log.e("DrawError", e.toString());
                //Da es bedeutet, dass eine Grafik nicht eingelesen wurde,
                //machen wir das einfach nochmal
                initialiseGrafics();
            }
            //Alles auf den Bildschirm malen
            //Und Canvas wieder freilassen
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    //Was passiert wenn man den Touchscreen berührt?
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int pointerIndex;
        int pointerId;

        //Finger bedeutet LOS
        pause = false;

        //Welcher Finger bewegt sich?
        pointerIndex = (motionEvent.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        pointerId = motionEvent.getPointerId(pointerIndex);

        //Alle Arten von Bewegung (auf dem Screen) die man bearbeiten will
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            //Spieler berührt den Bildschirm
            case MotionEvent.ACTION_DOWN:
                //Spieler 1 ist untere Hälfte
                if (finger1_index == INVALID_POINTER_ID && motionEvent.getY(pointerIndex) <= screenY/2) {
                    touchX1_finger1 = motionEvent.getX(pointerIndex);
                    touchY1_finger1 = motionEvent.getY(pointerIndex);

                    finger1_index = pointerId;
                } else if (finger2_index == INVALID_POINTER_ID && motionEvent.getY(pointerIndex) > screenY/2) {
                    touchX1_finger2 = motionEvent.getX(pointerIndex);
                    touchY1_finger2 = motionEvent.getY(pointerIndex);

                    finger2_index = pointerId;
                }
                return true;
            //Spieler bewegt den Finger auf dem Bildschirm
            case MotionEvent.ACTION_MOVE:
                int pointerCount = motionEvent.getPointerCount();

                //Move bekommt als PointerIndex immer 0 übergeben, deshalb muss man das hier extra auswählen
                for(int i = 0; i < pointerCount; ++i)
                {
                    pointerIndex = i;
                    pointerId = motionEvent.getPointerId(pointerIndex);
                    //Spieler 1
                    if (pointerId == finger1_index && motionEvent.getY(pointerIndex) < screenY/2 - rectSize) {
                        touchX1_finger1 = motionEvent.getX(pointerIndex);
                        touchY1_finger1 = motionEvent.getY(pointerIndex);
                    }
                    else if (pointerId == finger2_index && motionEvent.getY(pointerIndex) > screenY/2 + rectSize) {
                        touchX1_finger2 = motionEvent.getX(pointerIndex);
                        touchY1_finger2 = motionEvent.getY(pointerIndex);
                    }
                }
                return true;
            //Zweiter Finger kommt dazu:
            case MotionEvent.ACTION_POINTER_DOWN:
                //Spieler 1 kommt dazu
                if (finger1_index == INVALID_POINTER_ID && motionEvent.getY(pointerIndex) <= screenY/2) { //&& motionEvent.getY(pointerIndex) <= screenY/2
                    touchX1_finger1 = motionEvent.getX(pointerIndex);
                    touchY1_finger1 = motionEvent.getY(pointerIndex);

                    finger1_index = pointerId;
                }
                //Spieler 2 kommt dazu
                else if (finger2_index == INVALID_POINTER_ID && motionEvent.getY(pointerIndex) > screenY/2) { // && motionEvent.getY(pointerIndex) > screenY/2
                    touchX1_finger2 = motionEvent.getX(pointerIndex);
                    touchY1_finger2 = motionEvent.getY(pointerIndex);

                    finger2_index = pointerId;
                }
                //Dritter Finger wird ignoriert
                return true;
                //https://android-developers.googleblog.com/2010/06/making-sense-of-multitouch.html
            //Finger wird vom Bildschirm genommen:
            case MotionEvent.ACTION_UP:
                //Wenn es einer der Spielerfinger ist: Pause
                if (pointerId == finger1_index) {
                    //onBackPressed();
                    finger1_index = INVALID_POINTER_ID;
                    pause = true;
                }

                 else if (pointerId == finger2_index) {
                    //onBackPressed();
                    finger2_index = INVALID_POINTER_ID;
                    pause = true;
                }

                return true;

            case MotionEvent.ACTION_POINTER_UP:
                //Wenn es einer der Spielerfinger ist: Pause
                if (pointerId == finger1_index) {
                    //onBackPressed();
                    finger1_index = INVALID_POINTER_ID;
                    pause = true;
                }

                else if (pointerId == finger2_index) {
                    //onBackPressed();
                    finger2_index = INVALID_POINTER_ID;
                    pause = true;
                }

                return true;
        }
        return false;
    }

    //Aufräumen der größeren Objekte
    private void recycle() {

        //Musik aufräumen
        if (backgroundLoop != null) {
            backgroundLoop.pause();
            backgroundLoop.release();
            backgroundLoop = null;
        }
        //Sound aufräumen
        if (soundPool !=  null) {
            soundPool.autoPause();
            soundPool.release();
            soundPool = null;
            //Die einzelnen ints lohnen nicht zu resetten
        }
    }

    private void initialiseSound() {
        //Wenn wir sound aktiviert haben
        if (soundOn) {
            //In neuen Versionen soll man das halt jetzt so machen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                soundPool = new SoundPool.Builder()
                        .setMaxStreams(3)
                        .build();
            } else {
                //Aber ich will die alten Versionen trz nicht verlieren deshalb lassen wir das mal drin
                soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
            }

            try {
                AssetManager assetManager = fullContext.getAssets();
                AssetFileDescriptor descriptor;

                //Sounds tatsächlich einlesen
                descriptor = assetManager.openFd("hit1.wav");
                if (shoot == -1)
                    shoot = soundPool.load(descriptor, 1);
            } catch (IOException e) {
                Log.e("SoundLoadError", "Failed to load sound files: " + e.toString());
            }
        }

        //Wenn wir Musik anhaben
        if (musicOn) {
            try {
                //Beim ersten Start der Farmmusik
                if (backgroundLoop == null) {
                    backgroundLoop = MediaPlayer.create(fullContext, R.raw.gameloop1);
                    backgroundLoop.setLooping(true);
                    backgroundLoop.start();
                }
                //Wenn wir nachträglich wieder in den Farmmodus wechseln
                if (backgroundLoop != null && !backgroundLoop.isPlaying()) {
                    backgroundLoop.start();
                }
            } catch (IllegalStateException e) {
                Log.e("gamemode1 Error", e.toString());
            }
        }
    }
}
