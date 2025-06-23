package com.technathon.vmedicine.api;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// This interface defines the endpoints for the OpenFDA API
public interface OpenFDAService {

    // Example: Search drug labels by brand name
    // Endpoint: https://api.fda.gov/drug/label.json?search=openfda.brand_name:"{drug_name}"&limit=1
    @GET("drug/label.json")
    Call<OpenFDAResponse> searchDrugLabel(
            @Query("search") String query,
            @Query("limit") int limit
    );

    // You might add more methods here for other OpenFDA endpoints,
    // e.g., to search for adverse events, etc.
}
