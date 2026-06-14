package com.iqra.amarbidyutadmin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class DashboardFragment extends Fragment {

    Button logoutBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View myview = inflater.inflate(R.layout.fragment_dashboard, container, false);

        logoutBtn = myview.findViewById(R.id.logoutBtn);

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences prefs = getActivity().getSharedPreferences("amarbidyut_admin", getActivity().MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear().apply();

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();

            }
        });

        return myview;
    }
}