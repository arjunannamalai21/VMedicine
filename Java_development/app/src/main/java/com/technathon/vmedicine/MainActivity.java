package com.technathon.vmedicine; // Make sure this matches your package name

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.technathon.vmedicine.QRScanActivity; 

public class MainActivity extends AppCompatActivity {

    private Button btnScanQR;
    private Button btnScanPrescription;
    private Button btnAIChatBot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize buttons
        btnScanQR = findViewById(R.id.btnScanQR);
        btnScanPrescription = findViewById(R.id.btnScanPrescription);
        btnAIChatBot = findViewById(R.id.btnAIChatBot);

        // Set OnClickListener for each button
        btnScanQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to QRScanActivity
                Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
                startActivity(intent);
            }
        });

        btnScanPrescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement navigation to Prescription Scan screen
                // Toast.makeText(MainActivity.this, "Scan Doctor Prescription Clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        btnAIChatBot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement navigation to AI Chat Bot screen
                // Toast.makeText(MainActivity.this, "AI Chat Bot Clicked!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}