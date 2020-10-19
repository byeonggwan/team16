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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    //Map
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private NaverMap naverMap;

    //Camera
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private final int MY_PERMISSION_REQUEST_CAMERA = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            //권한요청 후 권한 다 되면 true
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
            if (!locationSource.isActivated()) { // 권한 거부됨
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
    public class city{
        String name;
        String desc1;
        String desc2;
        double lat;
        double lon;
        double distance;
    }
    public void jsonparsing(){
        try {
            JSONObject jObj = new JSONObject(jsontostring());
            JSONObject Seoul = jObj.getJSONObject("seoul");
            city seoul = new city();
            seoul.name =  Seoul.getString("name");
            seoul.desc1 = Seoul.getString("desc1");
            seoul.desc2 = Seoul.getString("desc2");
            seoul.lat = Seoul.getDouble("lat");
            seoul.lon = Seoul.getDouble("lon");

            JSONObject Busan = jObj.getJSONObject("busan");
            city busan = new city();
            busan.name = Busan.getString("name");
            busan.desc1 = Busan.getString("desc1");
            busan.desc2 = Busan.getString("desc2");
            busan.lat = Busan.getDouble("lat");
            busan.lon = Busan.getDouble("lon");

            JSONObject Ulsan = jObj.getJSONObject("ulsan");
            city ulsan = new city();
            ulsan.name = Ulsan.getString("name");
            ulsan.desc1  = Ulsan.getString("desc1");
            ulsan.desc2 = Ulsan.getString("desc2");
            ulsan.lat = Ulsan.getDouble("lat");
            ulsan.lon = Ulsan.getDouble("lon");

            JSONObject Incheon = jObj.getJSONObject("ulsan");
            city incheon = new city();
            incheon.name = Incheon.getString("name");
            incheon.desc1  = Incheon.getString("desc1");
            incheon.desc2 = Incheon.getString("desc2");
            incheon.lat = Incheon.getDouble("lat");
            incheon.lon = Incheon.getDouble("lon");

            JSONObject DaeJeon = jObj.getJSONObject("ulsan");
            city daejeon = new city();
            daejeon.name = DaeJeon.getString("name");
            daejeon.desc1  = DaeJeon.getString("desc1");
            daejeon.desc2 = DaeJeon.getString("desc2");
            daejeon.lat = DaeJeon.getDouble("lat");
            daejeon.lon = DaeJeon.getDouble("lon");

            JSONObject SeJong = jObj.getJSONObject("ulsan");
            city sejong = new city();
            sejong.name = SeJong.getString("name");
            sejong.desc1  = SeJong.getString("desc1");
            sejong.desc2 = SeJong.getString("desc2");
            sejong.lat = SeJong.getDouble("lat");
            sejong.lon = SeJong.getDouble("lon");

            JSONObject DaeGu = jObj.getJSONObject("ulsan");
            city daegu = new city();
            daegu.name = DaeGu.getString("name");
            daegu.desc1  = DaeGu.getString("desc1");
            daegu.desc2 = DaeGu.getString("desc2");
            daegu.lat = DaeGu.getDouble("lat");
            daegu.lon = DaeGu.getDouble("lon");

            JSONObject GwangJu = jObj.getJSONObject("gwangju");
            city gwangju = new city();
            gwangju.name = DaeGu.getString("name");
            gwangju.desc1  = DaeGu.getString("desc1");
            gwangju.desc2 = DaeGu.getString("desc2");
            gwangju.lat = DaeGu.getDouble("lat");
            gwangju.lon = DaeGu.getDouble("lon");

            JSONObject JeJu = jObj.getJSONObject("jeju");
            city jeju = new city();
            jeju.name = DaeGu.getString("name");
            jeju.desc1  = DaeGu.getString("desc1");
            jeju.desc2 = DaeGu.getString("desc2");
            jeju.lat = DaeGu.getDouble("lat");
            jeju.lon = DaeGu.getDouble("lon");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    public void distance(city city, double cam_lat, double cam_lon){
        int earth_radius = 6371;
        double lat_dis = (city.lat - cam_lat)*Math.PI/180;
        double lon_dis = (city.lon - cam_lon)*Math.PI/180;
        /*
         Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
         Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
         */

        double buffer_const_a = Math.pow(Math.sin(lat_dis/2),2) +
                Math.cos((city.lat)*Math.PI/180)*Math.cos((cam_lat)*Math.PI/180)*Math.pow(Math.sin(lon_dis/2),2);
        double buffer_const_c = 2*Math.atan2(Math.sqrt(buffer_const_a),Math.sqrt(1-buffer_const_a));
        double distance = earth_radius * buffer_const_c;

        city.distance = distance;
    }


}