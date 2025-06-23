package com.technathon.vmedicine;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.technathon.vmedicine.data.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // Ensure this is imported if used elsewhere
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FindMedicineActivity extends AppCompatActivity {

    private static final String TAG = "FindMedicineActivity";
    // Removed OPEN_FDA_API_KEY constant - as discussed, it's not typically needed for basic OpenFDA queries
    private static final String OPEN_FDA_BASE_URL = "https://api.fda.gov/drug/label.json";
    private static final int LOCATION_PERMISSION_REQUEST = 1001; // Keeping if still used for manual search

    private static final String PREFS_NAME = "VMedicinePrefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";

    private Spinner medicineSpinner;
    private TextView tvRarity;
    private TextView tvOnlineLink;
    private TextView tvLocalStock;
    private Button btnBackToMain;
    private Button btnFindPharmacies;
    private TextView tvLoadingApiData;

    private FirebaseFirestore db;
    private String currentUserId;

    private OkHttpClient okHttpClient;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_medicine);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = preferences.getString(KEY_CURRENT_USER_ID, null);

        if (currentUserId == null) {
            Toast.makeText(this, "No user selected. Please select a profile.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, UserSelectionActivity.class); // Or LoginActivity, depending on your flow
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        Log.d(TAG, "Current User ID in FindMedicineActivity: " + currentUserId);

        db = FirebaseFirestore.getInstance();
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cache(new Cache(getCacheDir(), 10 * 1024 * 1024))
                .build();
        executorService = Executors.newSingleThreadExecutor();

        initializeViews();
        fetchUserProfileForMedicines();
    }

    private void initializeViews() {
        medicineSpinner = findViewById(R.id.medicineSpinner);
        tvRarity = findViewById(R.id.tvRarity);
        tvOnlineLink = findViewById(R.id.tvOnlineLink);
        tvLocalStock = findViewById(R.id.tvLocalStock);
        btnBackToMain = findViewById(R.id.btnBackToMain);
        btnFindPharmacies = findViewById(R.id.btnFindPharmacies);
        tvLoadingApiData = findViewById(R.id.tvLoadingApiData);

        btnBackToMain.setOnClickListener(v -> finish());
        btnFindPharmacies.setOnClickListener(v -> findNearbyPharmacies());
    }

    private void fetchUserProfileForMedicines() {
        Log.d(TAG, "Fetching user profile for medicines for user: " + currentUserId);
        db.collection("users").document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            UserProfile userProfile = document.toObject(UserProfile.class);
                            if (userProfile != null && userProfile.getCurrentMedications() != null &&
                                    !userProfile.getCurrentMedications().isEmpty()) {
                                populateMedicineSpinner(userProfile.getCurrentMedications());
                            } else {
                                showMessageAndSetupEmptySpinner("No medications found in profile for this user.");
                            }
                        } else {
                            showMessageAndSetupEmptySpinner("User profile not found for this user.");
                        }
                    } else {
                        showMessageAndSetupEmptySpinner("Failed to load profile: " +
                                task.getException().getMessage());
                        Log.e(TAG, "Error getting user profile", task.getException());
                    }
                });
    }

    private void showMessageAndSetupEmptySpinner(String message) {
        runOnUiThread(() -> {
            Toast.makeText(FindMedicineActivity.this, message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, message);
            setupEmptySpinner();
        });
    }

    private void setupEmptySpinner() {
        List<String> emptyList = new ArrayList<>();
        emptyList.add("No medications available");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, emptyList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        medicineSpinner.setAdapter(adapter);
        medicineSpinner.setEnabled(false);
    }

    private void populateMedicineSpinner(List<String> medications) {
        List<String> spinnerItems = new ArrayList<>();
        spinnerItems.add("Select a medicine...");
        spinnerItems.addAll(medications);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        medicineSpinner.setAdapter(adapter);

        medicineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String selectedMedicine = spinnerItems.get(position);
                    String medicineName = extractMedicineName(selectedMedicine);
                    displayMedicineDetails(medicineName);
                } else {
                    resetMedicineDetailsDisplay();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void resetMedicineDetailsDisplay() {
        runOnUiThread(() -> {
            tvRarity.setText("Rarity: ");
            tvOnlineLink.setText("Online Link: ");
            tvOnlineLink.setOnClickListener(null);
            tvLocalStock.setText("Local Stock: ");
            tvLoadingApiData.setVisibility(View.GONE);
        });
    }

    private String extractMedicineName(String combinedString) {
        int parenthesisIndex = combinedString.indexOf(" (");
        return parenthesisIndex != -1 ?
                combinedString.substring(0, parenthesisIndex).trim() :
                combinedString.trim();
    }

    private void displayMedicineDetails(String medicineName) {
        showLoadingState(medicineName);
        executorService.execute(() -> fetchMedicineDataFromAPI(medicineName));
    }

    private void showLoadingState(String medicineName) {
        runOnUiThread(() -> {
            tvLoadingApiData.setText("Fetching details for " + medicineName + "...");
            tvLoadingApiData.setVisibility(View.VISIBLE);
            tvRarity.setText("Rarity: Loading...");
            tvOnlineLink.setText("Online Link: Loading...");
            tvOnlineLink.setOnClickListener(null);
            tvLocalStock.setText("Local Stock: Loading...");
        });
    }

    private void fetchMedicineDataFromAPI(String medicineName) {
        try {
            String apiUrl = buildApiUrl(medicineName);
            Log.d(TAG, "OpenFDA API URL: " + apiUrl);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    handleApiFailure(medicineName, e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    handleApiResponse(medicineName, response);
                }
            });
        } catch (Exception e) {
            handleUnexpectedError(e);
        }
    }

    private String buildApiUrl(String medicineName) throws UnsupportedEncodingException {
        // Encode the brand name (exact phrase search)
        String encodedBrandName = URLEncoder.encode("\"" + medicineName + "\"", "UTF-8");
        // Encode the generic name (plain search)
        String encodedGenericName = URLEncoder.encode(medicineName, "UTF-8");

        // Combine search for brand_name (exact phrase) OR generic_name (plain)
        String query = "(openfda.brand_name:" + encodedBrandName + "+OR+openfda.generic_name:" + encodedGenericName + ")";

        String apiUrl = OPEN_FDA_BASE_URL + "?search=" + query + "&limit=1";
        Log.d(TAG, "OpenFDA API URL (final construction): " + apiUrl);
        return apiUrl;
    }

    private void handleApiFailure(String medicineName, IOException e) {
        runOnUiThread(() -> {
            tvLoadingApiData.setVisibility(View.GONE);
            tvRarity.setText("Rarity: Data unavailable");
            tvOnlineLink.setText("Online Link: Search manually");
            tvLocalStock.setText("Local Stock: Check pharmacies");

            String searchUrl = "https://www.google.com/search?q=" +
                    Uri.encode("buy " + medicineName + " online");
            tvOnlineLink.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl));
                startActivity(intent);
            });

            Toast.makeText(FindMedicineActivity.this,
                    "Failed to fetch details. Check connection.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "API call failed: " + e.getMessage(), e);
        });
    }

    private void handleApiResponse(String medicineName, Response response) throws IOException {
        runOnUiThread(() -> tvLoadingApiData.setVisibility(View.GONE));

        if (!response.isSuccessful()) {
            handleUnsuccessfulResponse(response);
            return;
        }

        try {
            String responseBody = response.body() != null ? response.body().string() : "";
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray results = jsonResponse.optJSONArray("results");

            String rarityText = results != null && results.length() > 0 ?
                    "Rarity: Commonly Available (FDA approved)" :
                    "Rarity: May be rare or not FDA approved";

            String onlineSearchUrl = "https://www.google.com/search?q=buy+" +
                    Uri.encode(medicineName) + "+online";

            updateUIWithMedicineDetails(rarityText, onlineSearchUrl);

        } catch (JSONException e) {
            handleJsonParsingError(e);
        }
    }

    private void handleUnsuccessfulResponse(Response response) throws IOException {
        String errorMessage = "API Error: " + response.code() + " - " +
                (response.body() != null ? response.body().string() : "No error details");
        Log.e(TAG, errorMessage);

        runOnUiThread(() -> {
            tvRarity.setText("Rarity: API Error (" + response.code() + ")");
            tvOnlineLink.setText("Online Link: Error");
            tvLocalStock.setText("Local Stock: Error");
            Toast.makeText(FindMedicineActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUIWithMedicineDetails(String rarityText, String onlineSearchUrl) {
        runOnUiThread(() -> {
            tvRarity.setText(rarityText);
            tvOnlineLink.setText("Online Link: Search online");
            tvOnlineLink.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(onlineSearchUrl));
                startActivity(intent);
            });
            tvOnlineLink.setTextColor(ContextCompat.getColor(FindMedicineActivity.this, R.color.blue_link_color));


            tvLocalStock.setText("Local Stock: Click 'Find Pharmacies'");
            tvLocalStock.setTextColor(ContextCompat.getColor(FindMedicineActivity.this, android.R.color.black));
        });
    }

    private void handleJsonParsingError(JSONException e) {
        Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
        runOnUiThread(() -> {
            tvRarity.setText("Rarity: Data error");
            tvOnlineLink.setText("Online Link: Error");
            tvLocalStock.setText("Local Stock: Error");
            Toast.makeText(FindMedicineActivity.this,
                    "Error processing medicine data", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleUnexpectedError(Exception e) {
        Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
        runOnUiThread(() -> {
            tvLoadingApiData.setVisibility(View.GONE);
            tvRarity.setText("Rarity: System error");
            tvOnlineLink.setText("Online Link: Error");
            tvLocalStock.setText("Local Stock: Error");
            Toast.makeText(FindMedicineActivity.this,
                    "System error occurred", Toast.LENGTH_SHORT).show();
        });
    }

    private void findNearbyPharmacies() {
        try {
            String uri = "https://www.google.com/maps/search/pharmacy+near+me";
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening pharmacy search", e);
            Toast.makeText(this, "Error opening pharmacy search", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findNearbyPharmacies();
            } else {
                Toast.makeText(this, "Location permission denied. Cannot find nearby pharmacies using precise location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
