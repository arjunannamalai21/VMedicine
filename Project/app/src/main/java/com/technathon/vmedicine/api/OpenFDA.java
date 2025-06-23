package com.technathon.vmedicine.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenFDA {
    @SerializedName("brand_name") // Matches "brand_name" within "openfda"
    public List<String> brandName;

    @SerializedName("generic_name") // Matches "generic_name"
    public List<String> genericName;

    @SerializedName("route") // How the drug is administered (oral, topical, etc.)
    public List<String> route;

    // Add other relevant fields like "rxcui", "unii", etc. if you need to link to RxNorm
}