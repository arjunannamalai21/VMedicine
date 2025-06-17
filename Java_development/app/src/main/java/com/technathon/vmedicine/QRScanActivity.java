package com.technathon.vmedicine;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View; // Import View for OnClickListener
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

// Retrofit and OpenFDA API imports
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.technathon.vmedicine.api.OpenFDAService;
import com.technathon.vmedicine.api.OpenFDAResponse;
import com.technathon.vmedicine.api.Result;
import com.technathon.vmedicine.api.OpenFDA;

public class QRScanActivity extends AppCompatActivity {

    private TextView tvMedicineNameData;
    private TextView tvHowToUseData;
    private TextView tvPostReactionsData;
    private Button btnScanAgain; // A button to trigger the scan

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    // Base URL for OpenFDA API
    private static final String OPEN_FDA_BASE_URL = "https://api.fda.gov/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);

        // Initialize TextViews from activity_qrscan.xml
        tvMedicineNameData = findViewById(R.id.tvMedicineNameData);
        tvHowToUseData = findViewById(R.id.tvHowToUseData);
        tvPostReactionsData = findViewById(R.id.tvPostReactionsData);

        // Initialize the Button from XML
        btnScanAgain = findViewById(R.id.btnScanAgain);

        // Set OnClickListener for the Scan Again button
        btnScanAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When Scan Again button is clicked, check permission and start scanner
                if (ContextCompat.checkSelfPermission(QRScanActivity.this, android.Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(QRScanActivity.this,
                            new String[]{android.Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST_CODE);
                } else {
                    startQRScanner(); // Permission already granted, start scanner
                }
            }
        });

        // Initial Request camera permission and start scan if no data is present
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, start scanner automatically on launch
            startQRScanner();
        }
    }

    // Method to start the QR scanner
    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a QR code"); // Message displayed to the user
        integrator.setOrientationLocked(false); // Allow rotation
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE); // Only scan QR codes
        integrator.initiateScan(); // Start the scanning activity
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start scanner
                startQRScanner();
            } else {
                // Permission denied, inform the user and close activity
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                finish(); // Close the activity if permission is denied
            }
        }
    }

    // Get the scan result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
                finish(); // Go back to previous activity if scan cancelled
            } else {
                // QR Code content found!
                String scannedData = result.getContents();
                Toast.makeText(this, "Scanned: " + scannedData, Toast.LENGTH_LONG).show();

                // Display a loading message while API call is in progress
                tvMedicineNameData.setText("Searching for: " + scannedData + "...");
                tvHowToUseData.setText("Fetching usage instructions...");
                tvPostReactionsData.setText("Fetching post reactions...");

                // Call the method to fetch drug details from OpenFDA API
                fetchDrugDetails(scannedData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Method to fetch drug details from OpenFDA API
    private void fetchDrugDetails(String drugName) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OPEN_FDA_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenFDAService service = retrofit.create(OpenFDAService.class);

        // The query for OpenFDA to search by brand name
        // For accurate results, the scanned QR code should ideally contain a precise drug name or identifier.
        String searchQuery = "openfda.brand_name:\"" + drugName + "\"";

        // Limit to 1 result for simplicity
        Call<OpenFDAResponse> call = service.searchDrugLabel(searchQuery, 1);

        call.enqueue(new Callback<OpenFDAResponse>() {
            @Override
            public void onResponse(Call<OpenFDAResponse> call, Response<OpenFDAResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().results != null && !response.body().results.isEmpty()) {
                    Result drugResult = response.body().results.get(0); // Get the first result

                    // Update UI with fetched data
                    if (drugResult.openfda != null && !drugResult.openfda.brandName.isEmpty()) {
                        tvMedicineNameData.setText("Medicine Name: " + drugResult.openfda.brandName.get(0));
                    } else if (drugResult.openfda != null && !drugResult.openfda.genericName.isEmpty()) {
                        tvMedicineNameData.setText("Medicine Name: " + drugResult.openfda.genericName.get(0) + " (Generic)");
                    } else {
                        tvMedicineNameData.setText("Medicine Name: Not found or unknown");
                    }

                    if (drugResult.indicationsAndUsage != null && !drugResult.indicationsAndUsage.isEmpty()) {
                        // Join list of strings into a single string for display
                        tvHowToUseData.setText("How to use: " + android.text.TextUtils.join("\n", drugResult.indicationsAndUsage));
                    } else {
                        tvHowToUseData.setText("How to use: Information not available.");
                    }

                    if (drugResult.warnings != null && !drugResult.warnings.isEmpty()) {
                        tvPostReactionsData.setText("Post Reactions/Warnings: " + android.text.TextUtils.join("\n", drugResult.warnings));
                    } else {
                        tvPostReactionsData.setText("Post Reactions/Warnings: Information not available.");
                    }

                } else {
                    Toast.makeText(QRScanActivity.this, "No drug details found for '" + drugName + "'", Toast.LENGTH_LONG).show();
                    tvMedicineNameData.setText("Medicine Name: Not found.");
                    tvHowToUseData.setText("How to use: N/A.");
                    tvPostReactionsData.setText("Post Reactions: N/A.");
                }
            }

            @Override
            public void onFailure(Call<OpenFDAResponse> call, Throwable t) {
                Toast.makeText(QRScanActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                tvMedicineNameData.setText("Medicine Name: Error fetching data.");
                tvHowToUseData.setText("How to use: Error fetching data.");
                tvPostReactionsData.setText("Post Reactions: Error fetching data.");
                t.printStackTrace(); // Log the error for debugging
            }
        });
    }
}
