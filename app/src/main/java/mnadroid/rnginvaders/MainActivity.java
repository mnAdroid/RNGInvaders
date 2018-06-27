package mnadroid.rnginvaders;

import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;

public class MainActivity extends AppCompatActivity {
    //Worker Thread (In unserem Fall macht der quasi alles)
    private GameView gameView;

    //Erster Start (in dem RAM (bei Moritz jeder Start))
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Loadingscreen soll weggehen
        //setContentView(R.layout.activity_main); //notwendig?

        //Display Objekt erstellen
        Display display = getWindowManager().getDefaultDisplay();
        //Ergebnis in ein Punkt laden
        Point size = new Point();
        display.getSize(size);
        //Um die Bildschirmgrößen an die Gameview weiterzugeben
        int screenX = size.x;
        int screenY = size.y;

        //Initialisierung der Gameview
        gameView = new GameView(this, screenX, screenY);

        //Anzeigen des besten GameViews der Welt
        setContentView(gameView);
    }
    //Wenn das Spiel (wieder) gestartet wird
    @Override
    protected void onResume() {
        super.onResume();

        gameView.resume();
    }

    //Wenn das Spiel geschlossen wird
    @Override
    protected void onPause() {
        super.onPause();

        gameView.pause();
    }

    //Wenn der Zurückbutton geklickt wird.
    @Override
    public void onBackPressed() {
        gameView.onBackPressed();
    }
}
