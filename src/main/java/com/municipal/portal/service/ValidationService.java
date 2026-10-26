package com.municipal.portal.service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ValidationService {

    private static final List<String> VALID_CATEGORIES = Arrays.asList(
        "Roads & Potholes", "Solid Waste Management", "Water Supply & Leakage",
        "Streetlights & Electricity", "Drainage & Sewage", "Public Health & Sanitation"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[^@\\s]+\\.[^@\\s]+$");

    public boolean isValidCategory(String category) {
        return category != null && VALID_CATEGORIES.contains(category.trim());
    }

    public boolean isValidContact(String contact) {
        if (contact == null) return false;
        String trimmed = contact.trim();
        return PHONE_PATTERN.matcher(trimmed).matches() || EMAIL_PATTERN.matcher(trimmed).matches();
    }

    public boolean isValidWard(String ward) {
        return ward != null && !ward.trim().isEmpty() && ward.trim().length() <= 10;
    }

    public boolean isValidDescription(String description) {
        // Resolved conflict: combined dev (min 15 chars) and feature branch (max 1500 chars) improvements
        return description != null && description.trim().length() >= 15 && description.trim().length() <= 1500;
    }

    public String sanitizeInput(String input) {
        if (input == null) return "";
        return input.replaceAll("<[^>]*>", "").replace("'", "''").trim();
    }

    public boolean validateComplaint(String name, String contact, String category, String ward, String description) {
        return name != null && !name.trim().isEmpty() && name.length() <= 100
            && contact != null && contact.length() <= 150
            && isValidContact(contact)
            && isValidCategory(category)
            && isValidWard(ward)
            && isValidDescription(description);
    }
}
