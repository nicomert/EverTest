package com.breadcatstudios.evertest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.evernote.edam.type.Note;
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
    private List<String> listaNotasGuid;

    // cliente para la consulta de las notas
    private NoteStoreClient noteStoreClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        listaNotasTexto = new ArrayList<String>();
        listaNotasGuid = new ArrayList<String>();
        listaNotasScroll = (ListView)findViewById(R.id.listaNotasScroll);

        // se comprueba que el usuario este autorizado
        if (!EvernoteSession.getInstance().isLoggedIn())
            return;

        getSupportActionBar().setTitle(R.string.app_name);

        // se consulta la lista de notas
        ConsultaListaNotas(true);
    }

    private void ConsultaListaNotas(final boolean ordenaPorFecha) {
        // se vacia la lista en caso de que tenga contenido
        if(listaNotasTexto.size() > 0) {
            listaNotasTexto = new ArrayList<String>();
            listaNotasGuid = new ArrayList<String>();
            listaNotasScroll.setAdapter(new ArrayAdapter<String>(ListaActivity.this, android.R.layout.simple_list_item_1, listaNotasTexto));
        }

        // se crea un nuevo hilo para lanzar las peticiones a evernote
        Thread hiloNotas = new Thread(new Runnable(){
            @Override
            public void run() {
                try {

                    // se crea el cliente para acceder a las notas
                    EvernoteAuth evernoteAuth = new EvernoteAuth(EvernoteService.SANDBOX, developerToken);
                    ClientFactory clientFactory = new ClientFactory(evernoteAuth);
                    noteStoreClient = clientFactory.createNoteStoreClient();

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
                            listaNotasGuid.add(nota.getGuid());
                        }
                        offset = offset + notas.getNotesSize();
                    } while (notas.getTotalNotes() > offset);

                    // si se trata de una busqueda por nombre, se invierte el orden de la coleccion
                    if(!ordenaPorFecha) {
                        Collections.reverse(listaNotasTexto);
                        Collections.reverse(listaNotasGuid);
                    }

                    // se actualiza la vista con la lista scrollable de notas
                    ActualizaListaNotas();

                }catch(Exception e){
                    Log.e("LISTA", "ERROR: " + e);
                }
            }
        });
        hiloNotas.start();
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
                        // si se hace tap, se consulta la informacion que contiene dicha nota
                        ConsultaNota(arg2);
                    }
                });
            }
        });
    }

    // realiza la consulta de la informacion de una nota pasada por parametro
    private void ConsultaNota(final int index){
        // se crea el hilo que realiza la consulta de la nota
        Thread hiloNota = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    // se carga el contenido completo de la nota
                    final Note fullNote = noteStoreClient.getNote(listaNotasGuid.get(index), true, true, false, false);

                    // se crea la pantalla flotante que contiene el contenido de la nota
                    ListaActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            new AlertDialog.Builder(ListaActivity.this)
                                    .setTitle("" + DesmontaXML(fullNote.getTitle()))
                                    .setMessage("" + DesmontaXML(fullNote.getContent()))
                                    .setNegativeButton(R.string.button_cerrar, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // do nothing
                                        }
                                    })
                                    .show();
                        }
                    });
                }catch(Exception e){
                    Log.e("LISTA", "ERROR cargando la nota " + index + ": " + e);
                }
            }
        });
        hiloNota.start();
    }

    // desmonta un xml y obtiene el contenido plano del mismo
    private String DesmontaXML(String xmltext){
        String resultado = "" + '\n';
        // se comprueba si se esta consultando una etiqueta del xml
        boolean tag = false;

        for(int i=0; i<xmltext.length(); i++){
            if(xmltext.charAt(i) == '<'){
                tag = true;
                if(resultado.charAt(resultado.length()-1) != '\n')
                    resultado += '\n';
            }else if(xmltext.charAt(i) == '>'){
                tag = false;
            }else{
                // si no hay ningun tag abierto, se trata del contenido del xml
                if(tag == false)
                    if(xmltext.charAt(i) != '\n')
                        resultado += xmltext.charAt(i);
            }
        }
        return resultado;
    }

    // crea una ventana para crear una nueva nota
    private void NuevaNota(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.texto_nuevanota);

        // box para introducir el titulo de la nota
        final EditText tituloInput = new EditText(this);
        tituloInput.setHint(R.string.texto_titulo);
        tituloInput.setInputType(InputType.TYPE_CLASS_TEXT);

        // box para introducir el contenido de la nota
        final EditText contenidoInput = new EditText(this);
        contenidoInput.setHint(R.string.texto_contenido);
        contenidoInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(tituloInput);
        layout.addView(contenidoInput);
        builder.setView(layout);

        // definicion de los botones de aceptar y cancelar
        builder.setPositiveButton(R.string.button_guardar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //m_Text = input.getText().toString();
                GuardaNota(tituloInput.getText().toString(), contenidoInput.getText().toString());

                ConsultaListaNotas(true);
            }
        });
        builder.setNegativeButton(R.string.button_cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void GuardaNota(String noteTitle, String noteBody) {

        String nBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        nBody += "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">";
        nBody += "<en-note>" + noteBody + "</en-note>";

        // se crea el objeto nota
        final Note nuevaNota = new Note();
        nuevaNota.setTitle(noteTitle);
        nuevaNota.setContent(nBody);

        // se crea el hilo que realiza la consulta de la nota
        Thread hiloNuevaNota = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    // se crea una nueva nota a traves del cliente
                    noteStoreClient.createNote(nuevaNota);
                } catch (Exception e) {
                    // Other unexpected exceptions
                    e.printStackTrace();
                }
            }
        });
        hiloNuevaNota.start();
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
                NuevaNota();
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
