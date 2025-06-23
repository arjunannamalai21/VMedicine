package com.technathon.vmedicine;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class QRScanActivity extends AppCompatActivity {

    private static final String TAG = "QRScanActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);

        // Initialize the ZXing IntentIntegrator
        // This will start the QR scanner activity
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a QR code on medicine packaging"); // Custom prompt for the scanner UI
        integrator.setOrientationLocked(false); // Allow orientation changes
        integrator.initiateScan(); // Start the scan
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Get the result of the QR scan
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                // Handle cases where scanning was cancelled or failed to get content
                Toast.makeText(this, "Scan cancelled or no content found.", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Scan cancelled or no content.");
                finish(); // Go back if scan cancelled
            } else {
                // Successfully scanned a QR code
                String scannedText = result.getContents();
                Log.d(TAG, "QR Scanned: " + scannedText);
                handleScannedText(scannedText);
            }
        } else {
            // This else branch is for other onActivityResult calls not related to QR scanning
            Log.w(TAG, "onActivityResult: Result is null for requestCode: " + requestCode);
        }
    }

    /**
     * Handles the scanned text from the QR code.
     * Launches ScannedMedicineDetailActivity and passes the scanned text as medicineName.
     * @param scannedText The text decoded from the QR code.
     */
    private void handleScannedText(String scannedText) {
        if (scannedText != null && !scannedText.trim().isEmpty()) {
            String medicineName = scannedText.trim();

            // Launch ScannedMedicineDetailActivity
            Intent intent = new Intent(QRScanActivity.this, ScannedMedicineDetailActivity.class);
            intent.putExtra("medicine_name", medicineName); // Pass the scanned medicine name
            startActivity(intent);
            finish(); // Finish QRScanActivity after launching the next one
        } else {
            Toast.makeText(this, "Scanned QR code is empty or invalid.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Scanned QR code content is empty.");
            finish(); // Go back if content is empty
        }
    }

    // Removed onResume(), onPause(), onRequestPermissionsResult() as IntentIntegrator handles internally
}