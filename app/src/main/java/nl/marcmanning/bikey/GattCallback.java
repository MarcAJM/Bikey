package nl.marcmanning.bikey;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;
import android.widget.Button;

import java.util.UUID;

public class GattCallback extends BluetoothGattCallback {
    private BluetoothGattCharacteristic characteristic;
    private UUID serviceUUID;
    private UUID characteristicUUID;
    private Button unlockButton;

    public GattCallback(Button unlockButton) {
        this.unlockButton = unlockButton;
        this.serviceUUID = UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214");
        this.characteristicUUID = UUID.fromString("19b10001-e8f2-537e-4f6c-d104768a1214");
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices();
        }  else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            unlockButton.setEnabled(false);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BluetoothGattService service = gatt.getService(serviceUUID);
            if (service != null) {
                characteristic = service.getCharacteristic(characteristicUUID);
                unlockButton.setEnabled(true);
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }
}
