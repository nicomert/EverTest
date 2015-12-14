package com.breadcatstudios.evertest;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.Collections;
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

        getSupportActionBar().setTitle("EverTest");

        // se consulta la lista de notas
        ConsultaListaNotas(true);
    }

    private void ConsultaListaNotas(final boolean ordenaPorFecha) {
        // se vacia la lista en caso de que tenga contenido
        if(listaNotasTexto.size() > 0) {
            listaNotasTexto = new ArrayList<String>();
            listaNotasScroll.setAdapter(new ArrayAdapter<String>(ListaActivity.this, android.R.layout.simple_list_item_1, listaNotasTexto));
        }

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
                    if(ordenaPorFecha == true)
                        filter.setOrder(NoteSortOrder.UPDATED.getValue());
                    else
                        filter.setOrder(NoteSortOrder.TITLE.getValue());

                    int offset = 0;
                    int maxNotes = 10;

                    NotesMetadataResultSpec resultSpec = new NotesMetadataResultSpec();
                    resultSpec.setIncludeTitle(true);

                    // se realiza la consulta para obtener la lista de notas
                    NotesMetadataList notas = null;
                    do {
                        notas = noteStoreClient.findNotesMetadata(filter, offset, maxNotes, resultSpec);
                        for (NoteMetadata nota : notas.getNotes()){
                            // se anyade la nota a la lista de notas
                            listaNotasTexto.add(nota.getTitle());
                        }
                        offset = offset + notas.getNotesSize();
                    } while (notas.getTotalNotes() > offset);

                    // si se trata de una busqueda por nombre, se invierte el orden de la coleccion
                    if(!ordenaPorFecha)
                        Collections.reverse(listaNotasTexto);

                    // se actualiza la vista con la lista scrollable de notas
                    ActualizaListaNotas();

                }catch(Exception e){
                    Log.e("LISTA", "ERROR: " + e);
                }
            }
        });
        thread.start();
    }


    // actualiza la lista scrollable de notas
    private void ActualizaListaNotas() {
        // se ejecuta la incorporacion de las notas desde el hilo principal de la aplicacion
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // se anyaden las notas a la lista de notas
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // se infla el menu y se anyaden los botones
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // se especifica la accion para cada uno de los botones
        switch (item.getItemId()) {
            case R.id.action_crear:
                break;
            case R.id.action_ordenar:
                break;
            case R.id.action_pornombre:
                ConsultaListaNotas(false);
                break;
            case R.id.action_porfecha:
                ConsultaListaNotas(true);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
