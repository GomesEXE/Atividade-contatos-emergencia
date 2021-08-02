package com.example.fivecontacts.main.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.example.fivecontacts.R;
import com.example.fivecontacts.main.model.Contato;
import com.example.fivecontacts.main.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

public class AlterarContatos_Activity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    Boolean primeiraVezUser = true;
    EditText edtNome;
    ListView lv;
    BottomNavigationView bnv;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alterar_contatos);
        edtNome = findViewById(R.id.edtBusca);
        bnv = findViewById(R.id.bnv);
        bnv.setOnNavigationItemSelectedListener(this);
        bnv.setSelectedItemId(R.id.anvAdicionar);

        // Recuperando dados da Intent anterior
        Intent quemChamou = this.getIntent();
        if (quemChamou != null) {
            Bundle params = quemChamou.getExtras();
            if (params != null) {
                // Recuperando o Usuario
                user = (User) params.getSerializable("usuario");
                setTitle("Alterar Contatos de Emergência");
            }
        }

        lv = findViewById(R.id.listContatosDoCell);

        // Evento de limpar Componente
        edtNome.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (primeiraVezUser){
                    primeiraVezUser = false;
                    edtNome.setText("");
                }

                return false;
            }
        });
    }

    // Função para salvar contato, chamada quando escolhemos um contato dos que foram buscados
    public void salvarContato (Contato c){
        SharedPreferences salvaContatos =
                getSharedPreferences("contatos",Activity.MODE_PRIVATE);

        int num = salvaContatos.getInt("numContatos", 0); // Checando quantos contatos já existem
        SharedPreferences.Editor editor = salvaContatos.edit();
        try {
            ByteArrayOutputStream dt = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(dt);
            dt = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(dt);
            oos.writeObject(c);
            String contatoSerializado = dt.toString(StandardCharsets.ISO_8859_1.name());
            editor.putString("contato" + (num + 1), contatoSerializado); // Salvando o contato com número único
            editor.putInt("numContatos", num + 1); // Salvando quantidade de contatos já salvos
        } catch (Exception e){
            e.printStackTrace();
        }
        editor.commit();
        user.getContatos().add(c);
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.v("PDMMat","Matando a Activity Lista de Contatos");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v("PDMMat","Matei a Activity Lista de Contatos");
    }

    // Ações realizadas ao clicar no botão "Buscar"
    public void onClickBuscar(View v){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            Log.v("PDMMat", "Pedir permissão");
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 3333);
            return;
        }
        Log.v("PDMMat", "Tenho permissão");

        // Com a permissão autorizada, busca os contatos com o nome informado
        ContentResolver cr = getContentResolver();
        String consulta = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
        String [] argumentosConsulta =  {"%"+edtNome.getText()+"%"};
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
                consulta,argumentosConsulta, null);

        // Depois de recuperar a quantidade de resultados da busca, cria duas strings com a quantidade de resultados
        final String[] nomesContatos = new String[cursor.getCount()];
        final String[] telefonesContatos = new String[cursor.getCount()];
        Log.v("PDMMat","Tamanho do cursor:" + cursor.getCount());

        int i = 0;
        while (cursor.moveToNext()) {
            int indiceNome = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
            String contatoNome = cursor.getString(indiceNome);
            Log.v("PDMMat", "Contato " + i + ", Nome:" + contatoNome);
            nomesContatos[i] = contatoNome;
            int indiceContatoID = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            String contactID = cursor.getString(indiceContatoID);
            String consultaPhone = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactID;
            Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, consultaPhone, null, null);

            while (phones.moveToNext()) {
                String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                telefonesContatos[i] = number; // Salva quando chega no último telefone
            }

            i++;
        }

        if (nomesContatos != null) {
            for(int j = 0; j <= nomesContatos.length; j++) {
                ArrayAdapter<String> adaptador;
                adaptador = new ArrayAdapter<String>(this, R.layout.list_view_layout, nomesContatos);
                lv.setAdapter(adaptador);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Contato c = new Contato();
                        c.setNome(nomesContatos[i]);
                        c.setNumero("tel:+"+telefonesContatos[i]);
                        salvarContato(c); // Chama função para salvar contato
                        Intent intent = new Intent(getApplicationContext(), ListaDeContatos_Activity.class);
                        intent.putExtra("usuario", user);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Checagem de o Item selecionado é o do perfil
        if (item.getItemId() == R.id.anvPerfil) {
            //Abertura da Tela MudarDadosUsario
            Intent intent = new Intent(this, PerfilUsuario_Activity.class);
            intent.putExtra("usuario", user);
            startActivity(intent);

        }
        // Checagem de o Item selecionado é o menu de ligar
        if (item.getItemId() == R.id.anvLigar) {
            // Abertura da Tela contatos salvos
            Intent intent = new Intent(this, ListaDeContatos_Activity.class);
            intent.putExtra("usuario", user);
            startActivity(intent);

        }
        return true;
    }
}