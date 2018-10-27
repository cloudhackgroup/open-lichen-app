package org.cloudhack.openlichen.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.cloudhack.openlichen.R;
import org.cloudhack.openlichen.models.LichenReport;
import org.cloudhack.openlichen.models.SpecimenInfo;
import org.cloudhack.openlichen.models.SpecimenData;
import org.cloudhack.openlichen.services.RestApi;
import org.cloudhack.openlichen.ui.adapters.SpecimenInfoSpinnerAdapter;
import org.cloudhack.openlichen.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ImageAreaSelectorActivity extends AppCompatActivity implements View.OnClickListener {
    private final static int STROKE = 2;
    private final static int VIEW_PAD_DP = 10;
    private final static Integer ROWS = 5;
    private final static Integer COLS = 2;
    private static final String BOX = "VIEWBOX";
    private static final int CODE = 20000;
    private Integer HEIGHT_PROP;
    private Integer WIDTH_PROP;
    private Integer FRAME_HEIGHT;
    private Integer FRAME_WIDTH;
    private String fileName;
    private ImageView imageView;
    private RelativeLayout contourLayout;
    private RelativeLayout rootLayoutEditor;
    private List<SpecimenInfo> infoList;
    private Spinner spinner;
    private SpecimenInfo actualSpecimen;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private MenuItem acceptItem;
    private Runnable locationTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_area_selector);
        getSupportActionBar()
                .setTitle(R.string.select_area_title);
        Bundle extraData = getIntent()
                .getExtras();
        WIDTH_PROP = extraData.getInt(Utils.WIDTH_PROP_KEY,0);
        HEIGHT_PROP = extraData.getInt(Utils.HEIGHT_PROP_KEY,0);
        FRAME_WIDTH = extraData.getInt(Utils.FRAME_WIDTH_KEY,0);
        FRAME_HEIGHT = extraData.getInt(Utils.FRAME_HEIGHT_KEY,0);
        fileName = extraData.getString(Utils.PHOTO_FILE_KEY,"");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        rootLayoutEditor = findViewById(R.id.rootLayoutEditor);
        spinner = findViewById(R.id.specimen_selection);
        imageView = findViewById(R.id.image);

        imageView.setImageBitmap(fixRotationImage());
        infoList = Arrays.stream(getResources().getStringArray(R.array.speciesList))
                .map(name -> SpecimenInfo.builder()
                        .affectdViews(new ArrayList<>())
                        .name(name)
                        .build())
                .collect(Collectors.toList());
        SpecimenInfoSpinnerAdapter spinnerAdapter = new SpecimenInfoSpinnerAdapter(this, R.layout.species_item);
        spinnerAdapter.setDataSet(infoList);
        spinner.setAdapter(spinnerAdapter);
        actualSpecimen = (SpecimenInfo)spinner.getSelectedItem();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                actualSpecimen = infoList.get(i);
                IntStream.range(0,contourLayout.getChildCount())
                        .forEach(child -> contourLayout.getChildAt(child)
                                .setBackgroundResource(R.drawable.mini_contour));
                actualSpecimen.getAffectdViews()
                        .forEach(view1 -> view1.setBackgroundResource(R.drawable.mini_contour_selected));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        createFrames();

    }

    private Bitmap fixRotationImage() {
        File lastPhoto = new File(Environment.getExternalStorageDirectory(), fileName);
        Bitmap loadedBitmap = BitmapFactory.decodeFile(lastPhoto.getAbsolutePath());
        Integer orientation = ExifInterface.ORIENTATION_NORMAL;
        Integer degrees = 0;
        try {
            orientation = new ExifInterface(lastPhoto.getAbsolutePath())
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        switch (orientation){
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(loadedBitmap, 0, 0, loadedBitmap.getWidth(),
                loadedBitmap.getHeight(), matrix, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.accept_action, menu);
        acceptItem = menu.getItem(0);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.accept_menu){
            sendToServer();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CODE && permissions.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                locationTask.run();
            } else {

            }
        }
    }

    private void createFrames() {
        contourLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams lp  = new RelativeLayout.LayoutParams(
                FRAME_WIDTH,
                FRAME_HEIGHT
        );
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
        lp.setMargins(0,dpToPx(VIEW_PAD_DP),0,0);
        lp.addRule(RelativeLayout.BELOW);
        contourLayout.setLayoutParams(lp);
        contourLayout.setBackgroundResource(R.drawable.contour);
        // Draw subviews, deberiamos tener en cuenta el error < 1px
        Integer boxwpx = (int)(FRAME_WIDTH.floatValue() / COLS.floatValue());
        Integer boxhpx = (int)(FRAME_HEIGHT.floatValue() / ROWS.floatValue());
        rootLayoutEditor.addView(contourLayout);
        IntStream.range(0,COLS)
                .forEach(col -> IntStream.range(0,ROWS)
                .forEach(row -> {
                    View box = new View(this);
                    RelativeLayout.LayoutParams lpb = new RelativeLayout.LayoutParams(
                            boxwpx,boxhpx);
                    lpb.addRule(RelativeLayout.ALIGN_PARENT_START,RelativeLayout.TRUE);
                    lpb.addRule(RelativeLayout.ALIGN_PARENT_TOP,RelativeLayout.TRUE);
                    lpb.setMargins((col * boxwpx) - dpToPx(STROKE), (row * boxhpx) - dpToPx(STROKE)
                            ,0,0);
                    box.setLayoutParams(lpb);
                    box.setBackgroundResource(R.drawable.mini_contour);
                    box.setOnClickListener(this);
                    box.setTag(new SelectionInfo(false));
                    contourLayout.addView(box);
                }));
    }

    public void sendToServer() {
        //Check empty-ness
        List<SpecimenInfo> choosen = infoList.stream()
                .filter(specimenInfo -> specimenInfo.getAffectdViews().size() > 0)
                .collect(Collectors.toList());
        if(choosen.size() < 1){
            Snackbar.make(findViewById(android.R.id.content),
                    "Please select some species before send nothing !",
                    Snackbar.LENGTH_LONG)
                    .show();
        } else {
            OnSuccessListener<? super Location> successTask = location -> {
                if (location != null) {
                    Map<String, SpecimenData> sample = new HashMap<>();
                    choosen.forEach(specimenInfo -> sample.put(specimenInfo.getName(),
                                    SpecimenData.builder()
                                            .tilesCovered(specimenInfo.getAffectdViews().size())
                                            .milimetersCovered(-1)
                                            .build()));
                    new RestApi()
                            .execute(LichenReport.builder()
                                    .lat(location.getLatitude())
                                    .lng(location.getLongitude())
                                    .datetime((int)(Calendar.getInstance()
                                            .getTimeInMillis() / 1000))
                                    .samples(sample)
                                    .reportId(UUID.randomUUID().toString())
                                    .build()
                                    .toJson());
                    Toast.makeText(this,
                            "Yay ! Report sended to server",
                            Toast.LENGTH_SHORT)
                            .show();
                    finish();
                } else {
                    Snackbar.make(findViewById(android.R.id.content),
                            "Oops we couldn't get your location !",
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            };
            locationTask = () -> {
                try {
                    fusedLocationProviderClient.getLastLocation()
                            .addOnSuccessListener(successTask);
                } catch (SecurityException e) {
                    Snackbar.make(findViewById(android.R.id.content),
                            "This should never happen ... ",
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            };
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, CODE);
            } else {
                locationTask.run();
            }
        }

    }

    private int dpToPx(float dps){
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    @Override
    public void onClick(View view) {
        if(view.getTag() instanceof SelectionInfo &&
                !infoList.get(0).equals(actualSpecimen)){
            SelectionInfo info = ((SelectionInfo)view.getTag());
            if(info.selected){
                view.setBackgroundResource(R.drawable.mini_contour);
                actualSpecimen.getAffectdViews()
                        .remove(view);
            } else {
                view.setBackgroundResource(R.drawable.mini_contour_selected);
                actualSpecimen.getAffectdViews()
                        .add(view);
            }
            info.selected = !info.selected;
            ((ArrayAdapter)spinner.getAdapter())
                    .notifyDataSetChanged();
        }
    }

    public static class SelectionInfo {
        public boolean selected;
        public SelectionInfo(boolean selected) {
            this.selected = selected;
        }
    }


}
