package org.cloudhack.openlichen.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.cloudhack.openlichen.CameraPreviewActivity;
import org.cloudhack.openlichen.R;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startReportCreatorFlow(View view) {
        Intent intent = new Intent(this, CameraPreviewActivity.class);
        startActivity(intent);
    }

    public void openMaps(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }
}
