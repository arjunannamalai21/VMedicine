package com.technathon.vmedicine.data;

import java.util.List;
import java.util.ArrayList;

// POJO for storing user's medical profile in Firestore
public class UserProfile {
    private String userId;
    private List<String> allergies;
    private List<String> currentMedications; // Corrected: was currentMedations
    private List<String> medicalConditions;
    private int age;

    // Required public no-argument constructor for Firestore deserialization
    public UserProfile() {
        this.allergies = new ArrayList<>();
        this.currentMedications = new ArrayList<>();
        this.medicalConditions = new ArrayList<>();
        this.age = 0;
    }

    // Constructor with arguments for creating new UserProfile objects
    public UserProfile(String userId, List<String> allergies, List<String> currentMedications, List<String> medicalConditions, int age) {
        this.userId = userId;
        this.allergies = allergies != null ? allergies : new ArrayList<>();
        this.currentMedications = currentMedications != null ? currentMedications : new ArrayList<>(); // Corrected typo here
        this.medicalConditions = medicalConditions != null ? medicalConditions : new ArrayList<>();
        this.age = age;
    }

    // --- Getters ---
    public String getUserId() {
        return userId;
    }

    public List<String> getAllergies() {
        return allergies;
    }

    public List<String> getCurrentMedications() { // Corrected: was currentMedations
        return currentMedications;
    }

    public List<String> getMedicalConditions() {
        return medicalConditions;
    }

    public int getAge() {
        return age;
    }

    // --- Setters --- (Required for Firestore's automatic data mapping)
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAllergies(List<String> allergies) {
        this.allergies = allergies != null ? allergies : new ArrayList<>();
    }

    public void setCurrentMedications(List<String> currentMedications) { // Corrected: was currentMedations
        this.currentMedications = currentMedications != null ? currentMedications : new ArrayList<>();
    }

    public void setMedicalConditions(List<String> medicalConditions) {
        this.medicalConditions = medicalConditions != null ? medicalConditions : new ArrayList<>();
    }

    public void setAge(int age) {
        this.age = age;
    }
}