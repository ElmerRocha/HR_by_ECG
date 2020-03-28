package com.dye.heartratebyecg;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Menu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
    }

    public void IrInstrucciones(View view) {
        Intent instrucciones = new Intent(this, Instrucciones.class);
        startActivity(instrucciones);
    }


    public void IrGrafica(View view) {
        Intent grafica = new Intent(this, Grafica.class);
        startActivity(grafica);
    }

    public void IrInformacion(View view) {
        Intent info = new Intent(this, Informacion.class);
        startActivity(info);
    }
}
