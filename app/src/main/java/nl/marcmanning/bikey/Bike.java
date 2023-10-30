package nl.marcmanning.bikey;

import android.content.Context;
import com.google.android.gms.maps.model.LatLng;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

public class Bike implements Serializable {
    private String macAddress;
    private double latitude;
    private double longitude;
    private boolean verified;

    public Bike(String macAddress) {
        this.macAddress = macAddress;
    }

    public Bike(Bike bike) {
        this.macAddress = bike.getMacAddress();
        this.latitude = bike.getLocation().latitude;
        this.longitude = bike.getLocation().longitude;
        this.verified = bike.isVerified();

    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Bike) {
            Bike bike = (Bike) object;
            if (bike.getMacAddress().equals(macAddress) && bike.isVerified() == verified) {
                return true;
            }
        }
        return false;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setLocation(LatLng latLng) {
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
    }

    public LatLng getLocation() {
        return new LatLng(latitude, longitude);
    }

    public boolean hasLocation() {
        if (this.latitude == 0 && this.longitude == 0) {
            return false;
        }
        return true;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isVerified() {
        return verified;
    }
}
