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

    //FPS
    //Um gleichmäßige Animationen auf allen Handys zu haben
    private long fps;

    //Boolean ob Spiel gerade läuft oder gepaused ist
    //volatile ist quasi ein synchronised ohne overhead
    private volatile boolean isPlaying;

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
    }

    //App wird (wieder) gestartet
    void resume() {
        //gespeicherte Variablen auslesen
        getSharedPreferences();

        //Gamethread starten
        gameThread = new Thread(this);
        gameThread.start();

        //Laden der Grafiken und Sounds
        initialiseGrafics();
        initialiseSound();

        //Spiel geht los wenn alles geladen ist
        isPlaying = true;
    }


    //App wird geschlossen
    void pause() {
        isPlaying = false;

        //Variablen abspeichern
        setSharedPrefernces();

        //Dem Garbagecollector helfen
        recycle();

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
        isPlaying = false;

        //Variablen abspeichern
        setSharedPrefernces();

        //Pause Screen noch zeichnen
    }

    //run() ist quasi eine Endlosschleife (solange das Game läuft) in dem alles passiert
    @Override
    public void run() {
        while (isPlaying) {
            //Start des Frames wird gespeichert
            long startFrameTime = System.currentTimeMillis();

            //Alle Berechnungen werden begangen
            update();

            //Alles wird gezeichnet
            draw();

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

    }


    //Alle Berechnungen der App
    private void update() {

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
            }
            try {
                //Tatsächliches Zeichnen
                paint.setColor(Color.argb(255, 255, 255, 255));
                canvas.drawPaint(paint);
                paint.setColor(Color.argb(255, 0, 0, 0));
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawRect(touchX1_finger1, touchY1_finger1, touchX1_finger1 +  400, touchY1_finger1 + 400, paint);
                canvas.drawRect(touchX1_finger2, touchY1_finger2, touchX1_finger2 +  400, touchY1_finger2 + 400, paint);

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
                    if (pointerId == finger1_index && motionEvent.getY(pointerIndex) <= screenY/2) {
                        touchX1_finger1 = motionEvent.getX(pointerIndex);
                        touchY1_finger1 = motionEvent.getY(pointerIndex);
                    }
                    else if (pointerId == finger2_index && motionEvent.getY(pointerIndex) > screenY/2) {
                        touchX1_finger2 = motionEvent.getX(pointerIndex);
                        touchY1_finger2 = motionEvent.getY(pointerIndex);
                    }
                }
                return true;
            //Zweiter Finger kommt dazu:
            case MotionEvent.ACTION_POINTER_DOWN:
                //Spieler 1 kommt dazu
                Log.d("Pointer down", "" + pointerId);
                Log.d("getY", "" + motionEvent.getY(pointerIndex));
                Log.d("screen/2", "" + screenY/2);

                Log.d("finger1_index", "" + finger1_index);
                Log.d("finger2_index", "" + finger2_index);

                if (finger1_index == INVALID_POINTER_ID && motionEvent.getY(pointerIndex) <= screenY/2) { //&& motionEvent.getY(pointerIndex) <= screenY/2
                    touchX1_finger1 = motionEvent.getX(pointerIndex);
                    touchY1_finger1 = motionEvent.getY(pointerIndex);

                    finger1_index = pointerId;

                    Log.d("finger 1 Pointer", "jetzt");
                }
                //Spieler 2 kommt dazu
                else if (finger2_index == INVALID_POINTER_ID && motionEvent.getY(pointerIndex) > screenY/2) { // && motionEvent.getY(pointerIndex) > screenY/2
                    touchX1_finger2 = motionEvent.getX(pointerIndex);
                    touchY1_finger2 = motionEvent.getY(pointerIndex);

                    finger2_index = pointerId;

                    Log.d("finger 2 Pointer", "jetzt");
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
                }

                 else if (pointerId == finger2_index) {
                    //onBackPressed();
                    finger2_index = INVALID_POINTER_ID;
                }

                return true;

            case MotionEvent.ACTION_POINTER_UP:
                //Wenn es einer der Spielerfinger ist: Pause
                if (pointerId == finger1_index) {
                    //onBackPressed();
                    finger1_index = INVALID_POINTER_ID;
                }

                else if (pointerId == finger2_index) {
                    //onBackPressed();
                    finger2_index = INVALID_POINTER_ID;
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
