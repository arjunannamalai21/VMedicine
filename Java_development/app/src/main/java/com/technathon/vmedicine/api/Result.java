package com.technathon.vmedicine.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Result {
    @SerializedName("openfda") // Matches the "openfda" object in the JSON
    public OpenFDA openfda;

    @SerializedName("indications_and_usage") // Example field for usage
    public List<String> indicationsAndUsage;

    @SerializedName("warnings") // Example field for warnings
    public List<String> warnings;

    // Add other fields you need from the API response
    // For simplicity, we're only including a few. You'll expand this later.
}