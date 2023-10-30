package nl.marcmanning.bikey;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

public class Verification {
    public static final int REQUEST_PERMISSIONS = 2;
    private static final int SCAN_PERIOD = 5000;
    private Bike bike;
    private MainActivity mainActivity;
    private ConstraintLayout buttonLayout;
    private BluetoothAdapter btAdapter;
    private boolean scanning;
    private ScanCallback scanCallback;
    private LocationCallback locationCallback;
    private HandlerThread handlerThread;
    private BluetoothLeScanner bleScanner;
    private boolean bleFound;

    public Verification(Bike bike, MainActivity mainActivity, ConstraintLayout buttonLayout) {
        this.bike = bike;
        this.mainActivity = mainActivity;
        this.buttonLayout = buttonLayout;
        this.scanning = false;
        this.bleFound = false;
        scanCallback = new ScanCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                String address = result.getDevice().getAddress();
                if (address.equals(bike.getMacAddress())) {
                    bleFound = true;
                }
            }
        };
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                bike.setLocation(new LatLng(location.getLatitude(), location.getLongitude()));
            }
        };
        showButtonLoadingState();
        startVerification();
    }

    public void showButtonLoadingState() {
        TextView questionMark = buttonLayout.findViewById(R.id.question_mark);
        ProgressBar progressBar = buttonLayout.findViewById(R.id.progress_bar);
        questionMark.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void showButtonUnknownState() {
        TextView questionMark = buttonLayout.findViewById(R.id.question_mark);
        ProgressBar progressBar = buttonLayout.findViewById(R.id.progress_bar);
        questionMark.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    public void startVerification() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            showButtonUnknownState();
        } else {
            if (!btAdapter.isEnabled()) {
                handleDisabledBluetooth();
            } else {
                if (!arePermissionsGranted()) return;
                startTrackingLocation();
                if (!arePermissionsGranted()) return;
                startScan();
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void handleDisabledBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        showButtonUnknownState();
        if (!arePermissionsGranted()) return;
        mainActivity.startActivity(enableBtIntent);
        terminate();
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        bleScanner = btAdapter.getBluetoothLeScanner();
        Handler handler = new Handler();
        if (!scanning) {
            handler.postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    scanning = false;
                    bleScanner.stopScan(scanCallback);
                    terminate();
                }
            }, SCAN_PERIOD);

            scanning = true;
            bleScanner.startScan(scanCallback);
        } else {
            scanning = false;
            bleScanner.stopScan(scanCallback);
        }
    }

    @SuppressLint("MissingPermission")
    private void startTrackingLocation() {
        LocationManager locationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            handleDisabledProvider();
        } else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity);
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
        mainActivity.startActivity(intent);
    }

    private boolean arePermissionsGranted() {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
            terminate();
            return false;
        }
        return true;
    }

    private void terminate() {
        if (bleFound) {
            runVerifiedTermination();
        } else {
            showButtonUnknownState();
        }
        mainActivity.setOccupied(false);
    }

    @SuppressLint("MissingPermission")
    private void runVerifiedTermination() {
        mainActivity.showButtonVerifiedState(buttonLayout);
        Bike oldBike = new Bike(bike);
        bike.setVerified(true);
        MainActivity.replaceBikeInFile(oldBike, bike, mainActivity);
    }
}
