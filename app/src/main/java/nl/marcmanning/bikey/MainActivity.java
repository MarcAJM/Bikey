package nl.marcmanning.bikey;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private boolean occupied;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setOccupied(false);
        showBikeButtons();
    }

    public void showMap(Bike bike) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
        finish();
    }

    public void onAddNewBikeClicked(View view) {
        ConstraintLayout menuBar = findViewById(R.id.menu_bar);
        ConstraintLayout anbBar = findViewById(R.id.anb_bar);
        menuBar.setVisibility(View.GONE);
        anbBar.setVisibility(View.VISIBLE);
    }

    public void onContinueClicked(View view) {
        ConstraintLayout menuBar = findViewById(R.id.menu_bar);
        ConstraintLayout anbBar = findViewById(R.id.anb_bar);
        EditText macAddressEditText = findViewById(R.id.mac_address_edit_text);
        String macAddress = macAddressEditText.getText().toString();
        if (macAddress.length() == 17 && macAddressIsUnique(macAddress)) {
            macAddressEditText.setText("");
            menuBar.setVisibility(View.VISIBLE);
            anbBar.setVisibility(View.GONE);
            Bike bike = new Bike(macAddress);
            addBikeToFile(bike, this);
            addBikeButton(bike);
        }
    }

    private void showBikeButtons() {
        List<Bike> bikes = loadBikesFromFile(this);
        for(Bike bike : bikes) {
            addBikeButton(bike);
        }
    }

    private void addBikeButton(Bike bike) {
        ConstraintLayout noBikesLayout = findViewById(R.id.no_bikes_layout);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ConstraintLayout buttonLayout = (ConstraintLayout) inflater.inflate(R.layout.bike_button_layout, null);
        MainActivity mainActivity = this;
        Button bikeButton = buttonLayout.findViewById(R.id.bike_button);
        LinearLayout linearLayout = findViewById(R.id.linear_layout);
        noBikesLayout.setVisibility(View.GONE);
        bikeButton.setText(bike.getMacAddress());
        if (bike.isVerified()) {
            showButtonVerifiedState(buttonLayout);
        }
        linearLayout.addView(buttonLayout);
        bikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOccupied()) return;
                if (bike.isVerified()) {
                    showMap(bike);
                } else {
                    setOccupied(true);
                    new Verification(bike, mainActivity, buttonLayout);
                }
            }
        });
    }

    public void showButtonVerifiedState(ConstraintLayout buttonLayout) {
        TextView questionMark = buttonLayout.findViewById(R.id.question_mark);
        ProgressBar progressBar = buttonLayout.findViewById(R.id.progress_bar);
        Button bikeButton = buttonLayout.findViewById(R.id.bike_button);
        questionMark.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        bikeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#90EE90")));
        bikeButton.setGravity(Gravity.CENTER);
    }

    public static List<Bike> loadBikesFromFile(Context context) {
        List<Bike> bikes = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(context.openFileInput("registered_bikes"));
            bikes = (List<Bike>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }
        if (bikes == null) {
            bikes = new ArrayList<>();
        }
        return bikes;
    }

    public static void addBikeToFile(Bike bike, Context context) {
        List<Bike> bikes = loadBikesFromFile(context);
        bikes.add(bike);
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(context.openFileOutput("registered_bikes", Context.MODE_PRIVATE));
            objectOutputStream.writeObject(bikes);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void replaceBikeInFile(Bike oldBike, Bike newBike, Context context) {
        List<Bike> bikes = loadBikesFromFile(context);
        for (Bike bikeInFile : bikes) {
            if (bikeInFile.equals(oldBike)) {
                int index = bikes.indexOf(bikeInFile);
                bikes.set(index, newBike);
            }
        }
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(context.openFileOutput("registered_bikes", Context.MODE_PRIVATE));
            objectOutputStream.writeObject(bikes);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void onExitClicked(View view) {
        ConstraintLayout menuBar = findViewById(R.id.menu_bar);
        ConstraintLayout anbBar = findViewById(R.id.anb_bar);
        EditText macAddressEditText = findViewById(R.id.mac_address_edit_text);
        macAddressEditText.setText("");
        menuBar.setVisibility(View.VISIBLE);
        anbBar.setVisibility(View.GONE);
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    private boolean macAddressIsUnique(String macAddress) {
        List<Bike> bikes = loadBikesFromFile(this);
        for (Bike bike : bikes) {
            if (bike.getMacAddress().equals(macAddress)) {
                return false;
            }
        }
        return true;
    }
}