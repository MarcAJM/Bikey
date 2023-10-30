package nl.marcmanning.bikey;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.nio.charset.Charset;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQUEST_PERMISSIONS = 10;
    private BluetoothAdapter btAdapter;
    private HandlerThread handlerThread;
    private LocationCallback locationCallback;
    private ScanCallback scanCallback;
    private BluetoothLeScanner bleScanner;
    private Location userLocation;
    private boolean bleFound;
    private GattCallback gattCallback;
    private BluetoothGatt gatt;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        mapFragment.getMapAsync(this);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        gattCallback = new GattCallback(findViewById(R.id.unlock_button));
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                userLocation = locationResult.getLastLocation();
            }
        };
        scanCallback = new ScanCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                String address = result.getDevice().getAddress();
                for (Bike bike : MainActivity.loadBikesFromFile(getApplicationContext())) {
                    if (address.equals(bike.getMacAddress())) {
                        BluetoothDevice device = btAdapter.getRemoteDevice(address);
                        gatt = device.connectGatt(getApplicationContext(), false, gattCallback);
                    }
                }
            }
        };
        start();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.setMyLocationEnabled(true);
        showBikeMarkers(googleMap);
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 16);
                googleMap.animateCamera(cameraUpdate);
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        terminate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        terminate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        terminate();
    }

    @SuppressLint("MissingPermission")
    private void terminate() {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        if (bleScanner != null) {
            bleScanner.stopScan(scanCallback);
        }
        if (handlerThread != null) {
            handlerThread.quit();
        }
    }

    public void openMain(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @SuppressLint("MissingPermission")
    public void unlockBike(View view) {
        byte[] data = new byte[] { 1 };
        gattCallback.getCharacteristic().setValue(data);
        boolean success = gatt.writeCharacteristic(gattCallback.getCharacteristic());
        if (!success) {
            unlockBike(view);
        }
    }

    private void showBikeMarkers(GoogleMap googleMap) {
        for (Bike bike : MainActivity.loadBikesFromFile(this)) {
            if (bike.hasLocation()) {
                MarkerOptions markerOptions = createBikeMarker(bike, googleMap);
                googleMap.addMarker(markerOptions);
            }
        }
    }

    private MarkerOptions createBikeMarker(Bike bike, GoogleMap googleMap) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(bike.getLocation());
        markerOptions.title(bike.getMacAddress());
        return markerOptions;
    }

    private void start() {
        if (!btAdapter.isEnabled()) {
            handleDisabledBluetooth();
        } else {
            if (!arePermissionsGranted()) {
                openMain(null);
                return;
            }
            startTrackingLocation();
            if (!arePermissionsGranted()) {
                openMain(null);
                return;
            }
            startScan();
        }
    }

    private boolean arePermissionsGranted() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    private void handleDisabledBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (!arePermissionsGranted()) {
            openMain(null);
            return;
        }
        startActivity(enableBtIntent);
    }

    @SuppressLint("MissingPermission")
    private void startTrackingLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            handleDisabledProvider();
        } else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            handlerThread = new HandlerThread("LocationUpdatesThread");
            handlerThread.start();
            Looper looper = handlerThread.getLooper();
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, looper);
        }
    }

    private void handleDisabledProvider() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        bleScanner = btAdapter.getBluetoothLeScanner();
        bleScanner.startScan(scanCallback);
    }
}
