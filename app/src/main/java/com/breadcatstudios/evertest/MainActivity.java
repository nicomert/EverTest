package com.breadcatstudios.evertest;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.login.EvernoteLoginFragment;

public class MainActivity extends AppCompatActivity implements EvernoteLoginFragment.ResultCallback {

    // credenciales de acceso
    private static final EvernoteSession.EvernoteService EVERNOTE_SERVICE = EvernoteSession.EvernoteService.SANDBOX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // inicializacion de la sesion con evernote
        new EvernoteSession.Builder(this)
                .setEvernoteService(EVERNOTE_SERVICE)
                .build(getResources().getString(R.string.consumerKey), getResources().getString(R.string.consumerSecret))
                .asSingleton();

        EvernoteSession.getInstance().authenticate(MainActivity.this);
    }

    @Override
    public void onLoginFinished(boolean successful) {
        if (successful) {
            Intent intent = new Intent(MainActivity.this, ListaActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        } else {
            Log.e("MAIN", "Error en login");
        }
    }
}
