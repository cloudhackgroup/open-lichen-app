package org.cloudhack.openlichen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import org.cloudhack.openlichen.ui.ImageAreaSelectorActivity;
import org.cloudhack.openlichen.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.selector.FlashSelectorsKt;
import io.fotoapparat.selector.FocusModeSelectorsKt;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.selector.ResolutionSelectorsKt;
import io.fotoapparat.selector.SelectorsKt;
import io.fotoapparat.view.CameraView;

public class CameraPreviewActivity extends AppCompatActivity {
    private static final String TAG = "CameraPreviewActivity";
    private static final String PREFIX = "TESTlsample";
    private static final int REQ_CODE = 1000;
    public static final Integer HEIGHT_PROP = 50;
    public static final Integer WIDTH_PROP = 20;
    private static final int VIEW_PAD_DP= 10;
    private Integer FRAME_WIDTH = 0;
    private Integer FRAME_HEIGHT = 0;

    private String fileName;
    private CameraView cameraView;
    private RelativeLayout noPermission, rootLayout;
    private Fotoapparat fotoapparat;
    private boolean hasCameraPermission = false;
    private boolean hasStoragePermission = false;
    private final String[] permissionsToGet = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver
            .OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            createFrame();
            rootLayout.getViewTreeObserver()
                    .removeOnGlobalLayoutListener(this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        getSupportActionBar()
                .setTitle(R.string.take_picture);
        getSupportActionBar()
                .setDisplayHomeAsUpEnabled(true);
        rootLayout = findViewById(R.id.rootLayout);
        cameraView = findViewById(R.id.camera_view);
        noPermission = findViewById(R.id.no_permission);
        hasCameraPermission = ContextCompat.checkSelfPermission(this,
                permissionsToGet[0]) == PackageManager.PERMISSION_GRANTED;
        hasStoragePermission = ContextCompat.checkSelfPermission(this,
                permissionsToGet[1]) == PackageManager.PERMISSION_GRANTED;
        if(hasCameraPermission && hasStoragePermission){
            cameraView.setVisibility(View.VISIBLE);
            rootLayout.getViewTreeObserver()
                    .addOnGlobalLayoutListener(listener);
        } else {
            requestPermissions(
                    !hasCameraPermission && !hasStoragePermission ?
                            permissionsToGet : !hasCameraPermission ?
                            new String[] { permissionsToGet[0] } :
                            new String[] { permissionsToGet[1] }
                    , REQ_CODE);
        }

        fotoapparat = Fotoapparat.with(this)
                .into(cameraView)                               // view which will draw the camera preview
                .previewScaleType(ScaleType.CenterCrop)         // we want the preview to fill the view
                .photoResolution(ResolutionSelectorsKt.highestResolution())
                .lensPosition(LensPositionSelectorsKt.back())   // we want back camera
                .focusMode(SelectorsKt.firstAvailable(          // (optional) use the first focus mode which is supported by device
                        FocusModeSelectorsKt.continuousFocusPicture(),
                        FocusModeSelectorsKt.autoFocus(),       // in case if continuous focus is not available on device, auto focus will be used
                        FocusModeSelectorsKt.fixed()            // if even auto focus is not available - fixed focus mode will be used
                ))
                .flash(SelectorsKt.firstAvailable(              // (optional) similar to how it is done for focus mode, this time for flash
                        FlashSelectorsKt.autoFlash()
                ))
                .build();
    }

    private void createFrame() {
        View view = new View(this);
        Integer hpx = rootLayout.getHeight();
        Integer wpx = rootLayout.getWidth();
        Log.d(TAG,"Original layout is (h : " + hpx + ", w : " + wpx + ")");
        hpx -= dpToPx(75.0f); //Space between FAB and bottom + FAB Height
        wpx -= dpToPx(2 * VIEW_PAD_DP); // padding gral
        hpx -= dpToPx(2 * VIEW_PAD_DP); // en ambos
        float propSpace =  hpx.floatValue() / wpx.floatValue();
        float propFrame = HEIGHT_PROP.floatValue() / WIDTH_PROP.floatValue();
        if(propFrame < propSpace){
            hpx = (int) (wpx.floatValue() * propFrame);
        } else {
            wpx = (int) (hpx.floatValue() / propFrame);
        }
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(wpx,hpx);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
        lp.setMargins(0,dpToPx(VIEW_PAD_DP),0,0);
        view.setLayoutParams(lp);
        view.setBackgroundResource(R.drawable.contour);
        rootLayout.addView(view);
        FRAME_WIDTH  = wpx;
        FRAME_HEIGHT = hpx;

        fotoapparat.getCurrentParameters()
                .whenDone(cameraParameters -> {
                    Log.d(TAG,cameraParameters.getPreviewResolution().toString()+ " - "
                            + cameraParameters.getPictureResolution());
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasCameraPermission) {
            fotoapparat.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (hasCameraPermission) {
            fotoapparat.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQ_CODE){
            for(int i = 0 ; i < permissions.length; i++){
                switch (permissions[i]){
                    case Manifest.permission.CAMERA :
                        hasCameraPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE :
                        hasStoragePermission = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                        break;
                }
            }
            if (hasCameraPermission && hasStoragePermission){
                cameraView.setVisibility(View.VISIBLE);
                fotoapparat.start();
                rootLayout.getViewTreeObserver()
                        .addOnGlobalLayoutListener(listener);
            } else {
                noPermission.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void takePicture(View view) {
        fileName = PREFIX + Calendar.getInstance().getTime() + ".jpeg";
        //openImageAreaSelectorActivity();
        fotoapparat.takePicture()
                .saveToFile(new File(
                        Environment.getExternalStorageDirectory(),
                        fileName)
                )
                .whenDone(n -> openImageAreaSelectorActivity());
    }

    private void openImageAreaSelectorActivity() {
        Intent intent  = new Intent(this, ImageAreaSelectorActivity.class);
        intent.putExtra(Utils.FRAME_WIDTH_KEY, FRAME_WIDTH);
        intent.putExtra(Utils.FRAME_HEIGHT_KEY, FRAME_HEIGHT);
        intent.putExtra(Utils.HEIGHT_PROP_KEY, HEIGHT_PROP);
        intent.putExtra(Utils.WIDTH_PROP_KEY, WIDTH_PROP);
        intent.putExtra(Utils.PHOTO_FILE_KEY, fileName);
        startActivity(intent);
        finish();
    }

    private int dpToPx(float dps){
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }
}





