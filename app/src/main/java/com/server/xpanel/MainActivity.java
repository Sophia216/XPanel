package com.server.xpanel;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private XPanelView xPanelView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xPanelView = findViewById(R.id.xpanelview);

        xPanelView.setChuttyMode(false);
        xPanelView.setCanFling(true);
        xPanelView.setExposedPercent(0.25f);
        xPanelView.setKickBackPercent(0.65f);
    }
}
