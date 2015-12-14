package com.breadcatstudios.evertest;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.asyncclient.EvernoteNoteStoreClient;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.NoteSortOrder;

import java.util.ArrayList;
import java.util.List;

public class ListaActivity extends AppCompatActivity {

    // token de desarrollador para verificar las operaciones realizadas
    String developerToken = "S=s1:U=91cd8:E=158f927b841:C=151a1768998:P=1cd:A=en-devtoken:V=2:H=721e02b60e8f3fb7b12ef52c1e7377a9";

    // lista que contiene las notas creadas por el usuario
    public ListView listaNotasScroll;
    private List<String> listaNotasTexto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        listaNotasTexto = new ArrayList<String>();
        listaNotasScroll = (ListView)findViewById(R.id.listaNotasScroll);

        // se comprueba que el usuario este autorizado
        if (!EvernoteSession.getInstance().isLoggedIn())
            return;

        // se crea un nuevo hilo para lanzar las peticiones a evernote
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {

                    // se crea el cliente para acceder a las notas
                    EvernoteAuth evernoteAuth = new EvernoteAuth(EvernoteService.SANDBOX, developerToken);
                    ClientFactory clientFactory = new ClientFactory(evernoteAuth);
                    NoteStoreClient noteStoreClient = clientFactory.createNoteStoreClient();

                    // se inicializan los parametros para la consulta
                    NoteFilter filter = new NoteFilter();
                    filter.setOrder(NoteSortOrder.UPDATED.getValue());

                    int offset = 0;
                    int maxNotes = 10;

                    NotesMetadataResultSpec resultSpec = new NotesMetadataResultSpec();
                    resultSpec.setIncludeTitle(true);

                    // se realiza la consulta para obtener la lista de notas
                    NotesMetadataList notas = null;
                    do {
                        notas = noteStoreClient.findNotesMetadata(filter, offset, maxNotes, resultSpec);
                        for (NoteMetadata nota : notas.getNotes()){
                            // se anyade la nota a la lista scrollable de notas
                            AnyadeNota(nota.getTitle());
                        }
                        offset = offset + notas.getNotesSize();
                    } while (notas.getTotalNotes() > offset);

                }catch(Exception e){
                    Log.e("LISTA", "ERROR: " + e);
                }
            }
        });
        thread.start();
    }

    // permite anyadir una nueva nota a la lista scrollable
    private void AnyadeNota(final String tituloNota) {
        // se ejecuta la incorporacion de las notas desde el hilo principal de la aplicacion
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                // se anyade la nota a la lista de notas
                listaNotasTexto.add(tituloNota);
                listaNotasScroll.setAdapter(new ArrayAdapter<String>(ListaActivity.this, android.R.layout.simple_list_item_1, listaNotasTexto));
                listaNotasScroll.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
                        Log.d("LISTA", "Se ha pulsado el indice " + arg2);
                    }
                });
            }
        });
    }
}
