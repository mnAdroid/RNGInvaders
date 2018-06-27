package mnadroid.rnginvaders;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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

    //flüchtiger Boolean ob App gerade offen ist
    private volatile boolean isPlaying;

    //Musiksettings
    private boolean musicOn;
    private boolean soundOn;

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
        isPlaying = true;

        //gespeicherte Variablen auslesen
        getSharedPreferences();

        //Gamethread starten
        gameThread = new Thread(this);
        gameThread.start();
    }

    //App wird geschlossen
    void pause() {
        isPlaying = false;

        //Variablen abspeichern
        setSharedPrefernces();

        try {
            //GameThread (Endlosloop) beenden
            gameThread.join();
        } catch(InterruptedException e) {
            Log.e("Error: ", "joining thread");
        }
    }

    //Handy-Zurück-Button wird gedrückt
    void onBackPressed() {

    }

    //run() ist quasi eine Endlosschleife (solange das Game läuft) in dem alles passiert
    @Override
    public void run() {
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

    //Alle Berechnungen der App
    private void update() {

    }

    //Alle Zeichnungen der App
    private void draw() {

    }
}
