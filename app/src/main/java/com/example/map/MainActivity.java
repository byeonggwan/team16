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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
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

    //Map
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
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
    private final int MY_PERMISSION_REQUEST_CAMERA = 1001;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //sensor for direction
        directionView = findViewById(R.id.direction);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Load Views
        previewView = findViewById(R.id.previewView);

        // For Camera Preview
        if (cameraPermissionsGranted()) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);

            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {

                }
        }

        //For naver map
        if(locationPermissionGranted()) {
            FragmentManager fm = getSupportFragmentManager();
            MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);

            if (mapFragment == null) {
                mapFragment = MapFragment.newInstance();
                fm.beginTransaction().add(R.id.map, mapFragment).commit();
            }

            mapFragment.getMapAsync(this);
            locationSource =
                    new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
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

    private boolean cameraPermissionsGranted(){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA},MY_PERMISSION_REQUEST_CAMERA);

            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA},MY_PERMISSION_REQUEST_CAMERA);
            }
        }

        return true;
    }

    private boolean locationPermissionGranted(){
        //location permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }

            return;
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
    /*public void distance(city city, double cam_lat, double cam_lon){
        int earth_radius = 6371;
        double lat_dis = (city.lat - cam_lat)*Math.PI/180;
        double lon_dis = (city.lon - cam_lon)*Math.PI/180;
        /*
         Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
         Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
         */

    /*    double buffer_const_a = Math.pow(Math.sin(lat_dis/2),2) +
                Math.cos((city.lat)*Math.PI/180)*Math.cos((cam_lat)*Math.PI/180)*Math.pow(Math.sin(lon_dis/2),2);
        double buffer_const_c = 2*Math.atan2(Math.sqrt(buffer_const_a),Math.sqrt(1-buffer_const_a));
        double distance = earth_radius * buffer_const_c;

        city.distance = distance;
    }
    */

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
                cityView.setText(cityNameList.get(val) + "\n" + cityDesc1List.get(val) + "\n" + cityDesc2List.get(val));
            }
        }
    }

}