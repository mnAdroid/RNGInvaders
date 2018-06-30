package mnadroid.rnginvaders;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
    private volatile boolean isOpen;

    //Sind alle Elemente geladen
    private boolean loaded;
    //Ist gerade Pause?
    private boolean pause;
    private long pauseTimer; //Um gleichmäßiges Schießsen trotz Pause zu ermöglichen
    //Menu
    private boolean menu;
    //Einstellungen direkt vor dem Spiel
    private boolean playMenu;

    //Play Button im Main Menu
    private Rect menuPlayButton;
    private float menuPlayTextX, menuPlayTextY;
    private Rect menuReadyButtonPlayer1, menuReadyButtonPlayer2;
    private float menuReadyTextX, menuReadyTextY;
    private Rect menuShipPlayer1, menuShipPlayer2;

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

        //mögliche SpielerBullets erzeugen
        player1Bullets = new Bullet[BULLET_COUNT];
        player2Bullets = new Bullet[BULLET_COUNT];

        menu = true;
        playMenu = false;
    }

    //App wird (wieder) gestartet
    void resume() {
        isOpen = true;
        pause = true;
        pauseTimer = System.currentTimeMillis();

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
        isOpen = false;

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
        if (menu) {
            playMenu = false;
            if (player1.getReady())
                player1.setReady();
            if (player2.getReady())
                player2.setReady();
        }
        else {
            //Pause
            pause = true;
            pauseTimer = System.currentTimeMillis();

            //Variablen abspeichern
            setSharedPrefernces();

            //Pause Screen noch zeichnen
        }
    }

    //run() ist quasi eine Endlosschleife (solange das Game läuft) in dem alles passiert
    @Override
    public void run() {
        while (isOpen) {
            if (!loaded) {
                continue;
            }
            if (menu)
                runMenu();
            else
                runGame();
        }
    }

    private void runGame() {
        //Start des Frames wird gespeichert
        long startFrameTime = System.currentTimeMillis();

        //Alles wird gezeichnet
        drawGame();

        if (!pause) {
            //Alle Berechnungen werden begangen
            updateGame();
        }

        //FPS Berechnung
        long timeThisFrame = System.currentTimeMillis() - startFrameTime;
        if (timeThisFrame > 0) {
            fps = 1000 / timeThisFrame;
        }
    }

    private void runMenu() {
        //Menu wird gezeichnet
        drawMenu();
    }

    //Gespeicherte Variablen auslesen
    private void getSharedPreferences() {
        //Wo sind die Variablen abgespeichert?
        SharedPreferences sharedPreferences = fullContext.getSharedPreferences("AidsInvaders", 0);
        musicOn = sharedPreferences.getBoolean("musicOn", false);
        soundOn = sharedPreferences.getBoolean("soundOn", false);
        menu = sharedPreferences.getBoolean("menu", true);
    }

    //Variablen Speichern
    private void setSharedPrefernces() {
        //Wohin speichern wir die Variablen?
        SharedPreferences sharedPreferences = fullContext.getSharedPreferences("AidsInvaders", 0);
        //Editor initialisieren (um Daten zu speichern)
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //Variablen in den Editor schreiben
        editor.putBoolean("musicOn", musicOn);
        editor.putBoolean("soundOn", soundOn);
        editor.putBoolean("menu", menu);

        //Editor abspeichern
        editor.apply();
    }

    //Grafiken einlesen
    private void initialiseGrafics() {
        //Size of Playership
        rectSize = getScaledBitmapSize(screenX, 1080, 100);

        //Size of Bullets
        bulletWidth = getScaledBitmapSize(screenX, 1080, 25);
        bulletLength = getScaledBitmapSize(screenY, 1920, 25);

        //Starting positions for ships
        touchX1_finger1 = screenX/2;
        touchY1_finger1 = rectSize + bulletLength;

        touchX1_finger2 = screenX/2;
        touchY1_finger2 = screenY - rectSize - bulletLength;

        player1 = new Player(touchX1_finger1, touchY1_finger1, rectSize, Color.argb(255, 100, 100, 100));
        player2 = new Player(touchX1_finger2, touchY1_finger2, rectSize, Color.argb(255, 100, 100, 100));

        //Textproperties
        textSize = getScaledBitmapSize(screenX, 1080, 50);
        textSizeBig = getScaledBitmapSize(screenX, 1080, 100);

        scoreX = getScaledCoordinates(screenX, 1080, screenX/2);
        scoreY = getScaledCoordinates(screenY, 1920, screenY/2);

        paint.setTextAlign(Paint.Align.CENTER);

        //Menu
        menuPlayButton = new Rect(getScaledCoordinates(screenX, 1080, 200), 2*screenY/3,
                getScaledBitmapSize(screenX, 1080, 880), 2*screenY/3 + getScaledCoordinates(screenY, 1920, 300));

        menuPlayTextX = screenX/2;
        menuPlayTextY = 2*screenY/3 + getScaledCoordinates(screenY, 1920, 190);

        //Alle vier Button werden 200 Pixel hoch
        //Ready Button wird 340 Pixel breit
        menuReadyButtonPlayer1 = new Rect(getScaledCoordinates(screenX, 1080, 710), getScaledCoordinates(screenY, 1920, 1690),
                getScaledBitmapSize(screenX, 1080, 1050), getScaledCoordinates(screenY, 1920, 1890));

        menuReadyButtonPlayer2 = new Rect(getScaledCoordinates(screenX, 1080, 30), getScaledCoordinates(screenY, 1920, 30),
                getScaledBitmapSize(screenX, 1080, 370), getScaledCoordinates(screenY, 1920, 230));

        menuReadyTextX = getScaledCoordinates(screenX, 1080, 880);
        menuReadyTextY = getScaledCoordinates(screenY, 1920, 1810);

        menuShipPlayer1 = new Rect(getScaledCoordinates(screenX, 1080, 710), getScaledCoordinates(screenY, 1920, 1490),
                getScaledBitmapSize(screenX, 1080, 870), getScaledCoordinates(screenY, 1920, 1660));

        menuShipPlayer2 = new Rect(getScaledCoordinates(screenX, 1080, 210), getScaledCoordinates(screenY, 1920, 260),
                getScaledBitmapSize(screenX, 1080, 370), getScaledCoordinates(screenY, 1920, 430));

    }

    //Alle Berechnungen der App
    private void updateGame() {
        //Spieler Schiffe
        player1.updateShip(touchX1_finger1, touchY1_finger1, fps);
        player2.updateShip(touchX1_finger2, touchY1_finger2, fps);
        //BULLETS:

        //Damit Pause nicht die Abschussgeschwindigkeit beeinflusst
        if (pauseTimer > 0) {
            bulletTimer -= pauseTimer - System.currentTimeMillis();
            pauseTimer = 0;
        }
        //Neue Schüsse erstellen SHOOT!
        if (System.currentTimeMillis() - bulletTimer >= 350) { //Alle 250 Millisekunden wird zurück geschossen
            boolean tmp1 = false;
            boolean tmp2 = false;
            for (int i = 0; i < BULLET_COUNT; i++) {
                //Bullets von Player1 kommen von oben
                if (player1Bullets[i] == null && !tmp1) {
                    player1Bullets[i] = new Bullet(screenY + rectSize, player1.getShipX() - (bulletWidth / 2), player1.getShipY() + rectSize, bulletLength);
                    tmp1 = true;
                }
                //Bullets von Player2 kommen von unten
                if (player2Bullets[i] == null && !tmp2) {
                    player2Bullets[i] = new Bullet(screenY - rectSize - bulletLength, player2.getShipX() - (bulletWidth / 2), player2.getShipY() - rectSize - bulletLength, bulletLength);
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
            //Bullets von Player1 kommen von oben
            if (player1Bullets[i] != null && player1Bullets[i].getY() >= screenY + bulletLength) { //Unten raus
                player1Bullets[i] = null;
            }
            //Spieler 2 getroffen?
            if (player1Bullets[i] != null && player1Bullets[i].getY() >= player2.getShipY() - rectSize - bulletLength && player1Bullets[i].getY() < player2.getShipY() + rectSize) { //Gegner Höhe getroffen
                if (player1Bullets[i].getX() >= player2.getShipX() - rectSize - bulletWidth && player1Bullets[i].getX() < player2.getShipX() + rectSize) { //Gegner Breite getroffen
                    player1Bullets[i] = null;
                    player2.setHitpoints(-10);
                }
            }

            //Bullets von Player2 kommen von unten
            if (player2Bullets[i] != null && player2Bullets[i].getY() < -bulletLength) { //Oben raus
                player2Bullets[i] = null;
            }
            //Spieler 1 getroffen?
            if (player2Bullets[i] != null && player2Bullets[i].getY() >= player1.getShipY() - rectSize - bulletLength && player2Bullets[i].getY() < player1.getShipY() + rectSize) { //Gegner Höhe getroffen
                if (player2Bullets[i].getX() >= player1.getShipX() - rectSize - bulletWidth && player2Bullets[i].getX() < player1.getShipX() + rectSize) { //Gegner Breite getroffen
                    player2Bullets[i] = null;
                    player1.setHitpoints(-10);
                }
            }
        }

        //died player 1?
        if (player1.getHitpoints() <= 0) {
            player1.setWinCount();
            finishGame();
        }
        //died player 2?
        if (player2.getHitpoints() <= 0) {
            player2.setWinCount();
            finishGame();
        }
    }

    //Spielrunde beenden
    private void finishGame() {
        player1.resetRound(touchX1_finger1, touchY1_finger1);
        player2.resetRound(touchX1_finger2, touchY1_finger2);

        for (int i = 0; i < BULLET_COUNT; i++) {
            player1Bullets[i] = null;
            player2Bullets[i] = null;
        }

        if(player1.getWinCount() >= 2 || player2.getWinCount() >= 2) {
            menu = true;
        }
    }

    //Alle Zeichnungen der App
    private void drawGame() {
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
                canvas.drawRect(player1.getShipX() - rectSize, player1.getShipY() - rectSize, player1.getShipX() + rectSize, player1.getShipY() + rectSize, paint);
                canvas.drawRect(player2.getShipX() - rectSize, player2.getShipY() - rectSize, player2.getShipX() + rectSize, player2.getShipY() + rectSize, paint);

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

                //Hitpoints
                if (player1 != null && player2 != null) {
                    paint.setTextSize(textSizeBig);
                    canvas.drawText(player1.getHitpoints() + " vs " + player2.getHitpoints(), scoreX, scoreY + textSizeBig, paint);
                    canvas.rotate(-180, scoreX, scoreY); //kommt an den Rand
                    canvas.drawText(player1.getHitpoints() + " vs " + player2.getHitpoints(), scoreX, scoreY + textSizeBig, paint);
                    canvas.rotate(180, scoreX, scoreY); //kommt an den Rand
                }
                //Score Links
                paint.setTextSize(textSize);
                canvas.rotate(-90, scoreX, scoreY); //kommt an den Rand
                canvas.drawText(player1.getWinCount() + " vs " + player2.getWinCount(), scoreX, scoreY/2, paint);
                canvas.rotate(90, scoreX, scoreY); //reset

                //Score Rechts
                canvas.rotate(90, scoreX, scoreY); //kommt an den Rand
                canvas.drawText(player2.getWinCount() + " vs " + player1.getWinCount(), scoreX, scoreY/2, paint);
                canvas.rotate(-90, scoreX, scoreY); //reset
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

    //Menu
    private void drawMenu() {
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

                //Textfarbe
                paint.setColor(Color.argb(255, 100, 100, 100));

                if (!playMenu)
                    drawMainMenu();
                else {
                    drawPlayMenu();
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

    //Hauptmenü
    void drawMainMenu() {
        paint.setTextSize(textSizeBig);
        canvas.drawText("AIDS INVADERS" , scoreX, scoreY/2, paint);

        canvas.drawText("PLAY!", menuPlayTextX, menuPlayTextY, paint);

        if (menuPlayButton != null) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(20);
            canvas.drawRect(menuPlayButton, paint);
            paint.setStrokeWidth(0);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        paint.setTextSize(textSize);
        if (player1.getWinCount() > player2.getWinCount())
            canvas.drawText("Player 1 WON!", scoreX, scoreY/2 + textSizeBig, paint);
        else
            canvas.drawText("Player 2 WON!", scoreX, scoreY/2 + textSizeBig, paint);
    }

    //Spieleinstellungen
    void drawPlayMenu() {
        //Rechts jeweils vier Quadrate:
        //1x für die Farbauswahl des Schiffs
        paint.setStrokeWidth(20);
        paint.setColor(player1.getShipColor());
        canvas.drawRect(menuShipPlayer1, paint);

        paint.setColor(player2.getShipColor());
        canvas.drawRect(menuShipPlayer2, paint);

        paint.setColor(Color.argb(255, 100, 100, 100));
        //3x Auswahl für die Runen

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20);

        //READY BUTTONS
        if (player1.getReady()) {
            paint.setColor(Color.argb(255, 0, 200, 0));
            canvas.drawRect(menuReadyButtonPlayer1, paint);
            paint.setColor(Color.argb(255, 100, 100, 100));
        }
        else {
            canvas.drawRect(menuReadyButtonPlayer1, paint);
        }
        if (player2.getReady()) {
            paint.setColor(Color.argb(255, 0, 200, 0));
            canvas.drawRect(menuReadyButtonPlayer2, paint);
            paint.setColor(Color.argb(255, 100, 100, 100));
        }
        else {
            canvas.drawRect(menuReadyButtonPlayer2, paint);
        }

        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        canvas.drawText("READY", menuReadyTextX, menuReadyTextY, paint);
        canvas.rotate(-180, scoreX, scoreY);
        canvas.drawText("READY", menuReadyTextX, menuReadyTextY, paint);
        canvas.rotate(180, scoreX, scoreY);
    }

    //Was passiert wenn man den Touchscreen berührt?
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (menu)
            return onTouchMenu(motionEvent);
        else
            return onTouchGame(motionEvent);
    }

    //Touch im Spiel
    private boolean onTouchGame(MotionEvent motionEvent) {
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
                if (finger1_index == INVALID_POINTER_ID && motionEvent.getY(pointerIndex) < screenY/2) {
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
                    if (pointerId == finger1_index)
                        touchX1_finger1 = motionEvent.getX(pointerIndex);
                    if (pointerId == finger1_index && motionEvent.getY(pointerIndex) < screenY/2 - rectSize) {
                        touchY1_finger1 = motionEvent.getY(pointerIndex);
                        continue;
                    }
                    //Spieler 2
                    if (pointerId == finger2_index)
                        touchX1_finger2 = motionEvent.getX(pointerIndex);
                    if (pointerId == finger2_index && motionEvent.getY(pointerIndex) > screenY/2 + rectSize) {
                        touchY1_finger2 = motionEvent.getY(pointerIndex);
                    }
                }
                return true;
            //Zweiter Finger kommt dazu:
            case MotionEvent.ACTION_POINTER_DOWN:
                //Spieler 1 kommt dazu
                if (finger1_index == INVALID_POINTER_ID && motionEvent.getY(pointerIndex) < screenY/2) {
                    touchX1_finger1 = motionEvent.getX(pointerIndex);
                    touchY1_finger1 = motionEvent.getY(pointerIndex);

                    finger1_index = pointerId;
                }
                //Spieler 2 kommt dazu
                else if (finger2_index == INVALID_POINTER_ID && motionEvent.getY(pointerIndex) > screenY/2) {
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
                    pauseTimer = System.currentTimeMillis();
                }

                else if (pointerId == finger2_index) {
                    //onBackPressed();
                    finger2_index = INVALID_POINTER_ID;
                    pause = true;
                    pauseTimer = System.currentTimeMillis();
                }

                return true;

            case MotionEvent.ACTION_POINTER_UP:
                //Wenn es einer der Spielerfinger ist: Pause
                if (pointerId == finger1_index) {
                    //onBackPressed();
                    finger1_index = INVALID_POINTER_ID;
                    pause = true;
                    pauseTimer = System.currentTimeMillis();
                }

                else if (pointerId == finger2_index) {
                    //onBackPressed();
                    finger2_index = INVALID_POINTER_ID;
                    pause = true;
                    pauseTimer = System.currentTimeMillis();
                }

                return true;
        }
        return false;
    }
    //Touch im Menü
    private boolean onTouchMenu(MotionEvent motionEvent) {
        //Alle Arten von Bewegung (auf dem Screen) die man bearbeiten will
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            //Spieler berührt den Bildschirm
            case MotionEvent.ACTION_DOWN:
                float touchX = motionEvent.getX();
                float touchY = motionEvent.getY();

                if (!playMenu) {
                    if (menuPlayButton.top <= touchY && menuPlayButton.bottom >= touchY
                            && menuPlayButton.left <= touchX && menuPlayButton.right >= touchX) {
                        playMenu = true;
                        return true;
                    }
                }
                else {
                    //when both are ready the game starts
                    if (menuReadyButtonPlayer1.top <= touchY && menuReadyButtonPlayer1.bottom >= touchY
                            && menuReadyButtonPlayer1.left <= touchX && menuReadyButtonPlayer1.right >= touchX) {
                        player1.setReady();
                        if (player1.getReady() && player2.getReady())
                            initialiseGame();
                        return true;
                    }
                    if (menuReadyButtonPlayer2.top <= touchY && menuReadyButtonPlayer2.bottom >= touchY
                            && menuReadyButtonPlayer2.left <= touchX && menuReadyButtonPlayer2.right >= touchX) {
                        player2.setReady();
                        if (player1.getReady() && player2.getReady())
                            initialiseGame();
                        return true;
                    }
                    return true;
                }
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

    private void initialiseGame() {
        //Starting positions for ships
        touchX1_finger1 = screenX/2;
        touchY1_finger1 = rectSize + bulletLength;

        touchX1_finger2 = screenX/2;
        touchY1_finger2 = screenY - rectSize - bulletLength;

        player1 = new Player(touchX1_finger1, touchY1_finger1, rectSize, Color.argb(255, 100, 100, 100));
        player2 = new Player(touchX1_finger2, touchY1_finger2, rectSize, Color.argb(255, 100, 100, 100));

        menu = false;
        playMenu = false;
    }
}
