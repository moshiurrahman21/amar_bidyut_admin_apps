package com.iqra.amarbidyutadmin;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.navigationView);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                if (R.id.dashboard == menuItem.getItemId()){

                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout,new DashboardFragment()).commit();

                }else if (R.id.feeder == menuItem.getItemId()){

                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout,new FeederFragment()).commit();



                } else if (R.id.notice == menuItem.getItemId()) {

                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout,new NoticeFragment()).commit();

                }

                return false;
            }
        });




    }
}