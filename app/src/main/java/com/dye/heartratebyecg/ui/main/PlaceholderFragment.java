package com.dye.heartratebyecg.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.arch.lifecycle.ViewModelProviders;

import com.dye.heartratebyecg.Paso1;
import com.dye.heartratebyecg.Paso2;
import com.dye.heartratebyecg.Paso3;
import com.dye.heartratebyecg.Paso4;
import com.dye.heartratebyecg.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    public static Fragment newInstance(int indice) {
        Fragment fragment = null;

        switch (indice) {
            case 1: fragment = new Paso1(); break;
            case 2: fragment = new Paso2(); break;
            case 3: fragment = new Paso3(); break;
            case 4: fragment = new Paso4(); break;
        }

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_instrucciones, container, false);

        return root;
    }
}