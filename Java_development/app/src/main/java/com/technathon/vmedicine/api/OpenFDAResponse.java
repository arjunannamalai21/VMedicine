package com.technathon.vmedicine.api;

import java.util.List;

// Top-level response object for OpenFDA drug label search
public class OpenFDAResponse {
    public List<Result> results; // Corresponds to the "results" array in OpenFDA JSON
}