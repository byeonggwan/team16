package com.example.map;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION};

    //Map
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private NaverMap naverMap;

    //for city description
    private TextView cityView;
    private ArrayList<String> cityList = new ArrayList<>();
    private ArrayList<String> cityNameList = new ArrayList<>();
    private ArrayList<String> cityDesc1List = new ArrayList<>();
    private ArrayList<String> cityDesc2List = new ArrayList<>();
    private ArrayList<Double> cityLatList = new ArrayList<>();
    private ArrayList<Double> cityLonList = new ArrayList<>();

    //Camera
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;

    //Sensor for direction
    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] newRotationMatrix = new float[9];

    //for description
    private double latitude;
    private double longitude;

    private TextView directionView;

    //for city overlay
    private ViewGroup[] viewGroups = new ViewGroup[9];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!hasPermissions(PERMISSIONS)){
            requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
        //sensor for direction
        directionView = findViewById(R.id.direction);

        // Load Views
        previewView = findViewById(R.id.previewView);

        //For naver map
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
        locationSource =
                new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            bindPreview(cameraProvider);
        } catch (ExecutionException | InterruptedException e) {

        }


        //city description
        cityView = findViewById(R.id.description);

        try {
            JSONObject cities = new JSONObject(jsontostring());
            Iterator i = cities.keys();

            while(i.hasNext()) {
                String city = i.next().toString();
                cityList.add(city);
            }

            for(int val = 0; val<cityList.size(); val++)
            {
                JSONObject info = cities.getJSONObject(cityList.get(val));

                cityNameList.add(info.getString("name"));
                cityDesc1List.add(info.getString("desc1"));
                cityDesc2List.add(info.getString("desc2"));
                cityLatList.add(info.getDouble("lat"));
                cityLonList.add(info.getDouble("lon"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        viewGroups[0] = (ViewGroup) findViewById(R.id.layout_seoul);
        viewGroups[0].setY(2440/3);
        viewGroups[4] = (ViewGroup) findViewById(R.id.layout_busan);
        viewGroups[4].setY(2440/3);
        viewGroups[5] = (ViewGroup) findViewById(R.id.layout_ulsan);
        viewGroups[5].setY(2440/3);
        viewGroups[7] = (ViewGroup) findViewById(R.id.layout_gwangju);
        viewGroups[7].setY(2440/3);

    }

    private boolean hasPermissions(String[] permissions) {
        int result;

        for (String perms : permissions){
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED){
                return false;
            }
        }

        return true;
    }

    // For Camera Preview - Connect previewview and camera
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.SURFACE_VIEW);
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }

    /*private boolean PermissionGranted(){
        //location permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA},PERMISSION_REQUEST_CODE);

            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA},PERMISSION_REQUEST_CODE);
            }
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        }

        return true;
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this,"승인이 허가되어 있습니다.",Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(this,"아직 승인받지 않았습니다.",Toast.LENGTH_LONG).show();
                }
                break;
            }
        }


        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }

        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {

        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setScaleBarEnabled(false);   //scale bar
        uiSettings.setZoomControlEnabled(false);    //zoom button

        naverMap.addOnLocationChangeListener(location ->{
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        });

    }


    //JSON PARSING AND OBJECT CREATION
    public String jsontostring(){
        String cityinfo = "";

        try{
            InputStream ctinfo = getAssets().open("cityinfo.json");
            int f_size = ctinfo.available();
            byte[] stringbuff = new byte[f_size];
            ctinfo.read(stringbuff);
            ctinfo.close();

            cityinfo = new String(stringbuff, "UTF-8");

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return cityinfo;
    }
    public int distance(double lat, double lon){

        double dist;
        double x = longitude - lon;
        double dist_temp = Math.sin(Math.toRadians(latitude)) * Math.sin(Math.toRadians(lat))
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(x));

        dist = Math.toDegrees(Math.acos(dist_temp)) * 60 * 1.1515 * 1609.344;


    /*double buffer_const_a = Math.pow(Math.sin(lat_dis/2),2) +
            Math.cos((lat)*Math.PI/180)*Math.cos((latitude)*Math.PI/180)*Math.pow(Math.sin(lon_dis/2),2);
    double buffer_const_c = 2*Math.atan2(Math.sqrt(buffer_const_a),Math.sqrt(1-buffer_const_a));
    int distance = (int)(earth_radius * buffer_const_c);*/

        return (int) dist;
    }


    //sensor activity
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onResume() {

        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this);
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }
        updateOrientationAngles();

    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {

        // Rotation matrix based on current readings from accelerometer and magnetometer.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        //change rotationMatrix (to use a camera direction)
        sensorManager.remapCoordinateSystem(rotationMatrix, 1, 3, newRotationMatrix);

        // Express the updated rotation matrix as three orientation angles.
        SensorManager.getOrientation(newRotationMatrix, orientationAngles);

        orientationAngles[0] = (float) Math.toDegrees(orientationAngles[0]);

        //for direction string
        //float roundDegrees = Math.round(orientationAngles[0] / 10) * 10;
        int azimuth = (int) orientationAngles[0];

        String str = "loading";

        if (azimuth < 22 && azimuth >= -23)
            str = "North";
        else if (azimuth < 67 && azimuth >= 22)
            str = "Northeast";
        else if (azimuth < 112 && azimuth >= 67)
            str = "East";
        else if (azimuth < 157 && azimuth >= 112)
            str = "Southeast";
        else if (azimuth < -158 || azimuth >= 157)
            str = "South";
        else if (azimuth < -113 && azimuth >= -158)
            str = "Southwest";
        else if (azimuth < -68 && azimuth >= -113)
            str = "West";
        else if (azimuth < -23  && azimuth >= -68)
            str = "Northwest";

        if(directionView.getText() != str)
            directionView.setText(str);

        updateDescription(azimuth);
    }

    public void updateDescription(int azimuth){
        for(int val = 0; val<cityList.size(); val++){
            double degree;
            double diff;
            double lat_here = Math.toRadians(latitude);
            double lat_there = Math.toRadians(cityLatList.get(val));
            double lon_diff = Math.toRadians(cityLonList.get(val)-longitude);
            double y = Math.sin(lon_diff) * Math.cos(lat_there);
            double x = Math.cos(lat_here) * Math.sin(lat_there) - Math.sin(lat_here) * Math.cos(lat_there) * Math.cos(lon_diff);
            degree = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
            if(degree > 180.0f){
                degree = degree - 360.0f;
            }

            if (degree >= azimuth && degree < azimuth + 1){
                cityView.setText(cityNameList.get(val) + "\n" + cityDesc1List.get(val) + "\n" + cityDesc2List.get(val) + "\n"
                        + distance(cityLatList.get(val), cityLonList.get(val)) + "m (" + cityLatList.get(val) + ", " + cityLonList.get(val) + ")");
            }

            diff = degree - azimuth;
            if(val == 0 || val == 4 || val == 5 || val == 7){
                int percent = (int)((diff + 25.0f) / 70.0f * 1440) / 100 * 100;
                viewGroups[val].setX(percent);
                /*if (diff > -40.0f && diff < 40.0f){
                    // val, diff 에 맞춰서 로고 출력
                    viewGroups[val].setVisibility(View.VISIBLE);
                    int percent = (int)((diff + 40.0f) / 80.0f * 1440) / 10 * 10;
                    viewGroups[val].setX(percent);
                }
                else{
                    viewGroups[val].setVisibility(View.INVISIBLE);
                }*/
                final double max_distance = 600000.0f;
                double distance = distance(cityLatList.get(val), cityLonList.get(val));
                if( distance > 100000.0f)
                    distance = 100000.0f;
                int layout_xy =  (int)( 800.0f * ( (max_distance - distance) / max_distance ));
                ViewGroup.LayoutParams layoutParam = viewGroups[val].getLayoutParams();
                layoutParam.width = layout_xy;
                layoutParam.height = layout_xy;
                viewGroups[val].setLayoutParams(layoutParam);
            }

        }
    }

}