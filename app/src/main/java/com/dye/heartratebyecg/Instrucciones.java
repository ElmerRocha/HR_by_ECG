package com.dye.heartratebyecg;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.dye.heartratebyecg.ui.main.SectionsPagerAdapter;

public class Instrucciones extends AppCompatActivity implements Paso1.OnFragmentInteractionListener, Paso2.OnFragmentInteractionListener,
        Paso3.OnFragmentInteractionListener, Paso4.OnFragmentInteractionListener {

    ViewPager viewPager;
    private LinearLayout linearPuntos;
    private TextView[] puntosSlide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instrucciones);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        linearPuntos = findViewById(R.id.LinearPuntos);
        agregarIndicadorPuntos(0);

        viewPager.addOnPageChangeListener(viewListener);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private void agregarIndicadorPuntos(int pos) {
        puntosSlide = new TextView[4];
        linearPuntos.removeAllViews();

        for (int i=0; i<puntosSlide.length; i++) {
            puntosSlide[i] = new TextView(this);
            puntosSlide[i].setText(Html.fromHtml("&#8226;"));
            puntosSlide[i].setTextSize(35);
            puntosSlide[i].setTextColor(getResources().getColor(R.color.BlancoTransparente));
            linearPuntos.addView(puntosSlide[i]);
        }

        if (puntosSlide.length > 0 ) {
            puntosSlide[pos].setTextSize(45);
            puntosSlide[pos].setTextColor(getResources().getColor(R.color.Blanco));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int posicion) {
            agregarIndicadorPuntos(posicion);

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}