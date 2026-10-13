package lk.tideline.cleanup.service;

import java.util.List;

/** Checks and tidies the details people type about themselves, wherever they type them. */
public final class ContactDetails {

    /** The nine provinces of Sri Lanka, the only places a member can say they are in. */
    public static final List<String> PROVINCES = List.of(
            "Western Province",
            "Central Province",
            "Southern Province",
            "Northern Province",
            "Eastern Province",
            "North Western Province",
            "North Central Province",
            "Uva Province",
            "Sabaragamuwa Province");

    /** Sri Lanka's twenty-five districts, the unit an officer works to. */
    public static final List<String> DISTRICTS = List.of(
            "Colombo", "Gampaha", "Kalutara",
            "Kandy", "Matale", "Nuwara Eliya",
            "Galle", "Matara", "Hambantota",
            "Jaffna", "Kilinochchi", "Mannar", "Vavuniya", "Mullaitivu",
            "Batticaloa", "Ampara", "Trincomalee",
            "Kurunegala", "Puttalam",
            "Anuradhapura", "Polonnaruwa",
            "Badulla", "Monaragala",
            "Ratnapura", "Kegalle");

    /** Every district and the province it belongs to, for figures kept by one but asked for by the other. */
    private static final java.util.Map<String, String> PROVINCE_OF_DISTRICT = java.util.Map.ofEntries(
            java.util.Map.entry("Colombo", "Western Province"),
            java.util.Map.entry("Gampaha", "Western Province"),
            java.util.Map.entry("Kalutara", "Western Province"),
            java.util.Map.entry("Kandy", "Central Province"),
            java.util.Map.entry("Matale", "Central Province"),
            java.util.Map.entry("Nuwara Eliya", "Central Province"),
            java.util.Map.entry("Galle", "Southern Province"),
            java.util.Map.entry("Matara", "Southern Province"),
            java.util.Map.entry("Hambantota", "Southern Province"),
            java.util.Map.entry("Jaffna", "Northern Province"),
            java.util.Map.entry("Kilinochchi", "Northern Province"),
            java.util.Map.entry("Mannar", "Northern Province"),
            java.util.Map.entry("Vavuniya", "Northern Province"),
            java.util.Map.entry("Mullaitivu", "Northern Province"),
            java.util.Map.entry("Batticaloa", "Eastern Province"),
            java.util.Map.entry("Ampara", "Eastern Province"),
            java.util.Map.entry("Trincomalee", "Eastern Province"),
            java.util.Map.entry("Kurunegala", "North Western Province"),
            java.util.Map.entry("Puttalam", "North Western Province"),
            java.util.Map.entry("Anuradhapura", "North Central Province"),
            java.util.Map.entry("Polonnaruwa", "North Central Province"),
            java.util.Map.entry("Badulla", "Uva Province"),
            java.util.Map.entry("Monaragala", "Uva Province"),
            java.util.Map.entry("Ratnapura", "Sabaragamuwa Province"),
            java.util.Map.entry("Kegalle", "Sabaragamuwa Province"));

    /** The province a district belongs to, or null when the district is unknown or not given. */
    public static String provinceOfDistrict(String district) {
        return district == null ? null : PROVINCE_OF_DISTRICT.get(district.trim());
    }

    private static final String PHONE_HELP =
            "That phone number isn't valid. Use a Sri Lankan number such as +94 71 234 5678 or 071 234 5678.";

    private ContactDetails() {
    }

    /**
     * Returns the number as {@code +94XXXXXXXXX}, so the same phone is stored one way however it was
     * typed. Blank is fine — a phone number is optional.
     */
    public static String normalisePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("[\\s()./-]", "");
        if (digits.startsWith("+94")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("0094")) {
            digits = digits.substring(4);
        } else if (digits.startsWith("94") && digits.length() == 11) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0")) {
            digits = digits.substring(1);
        } else {
            throw new IllegalArgumentException(PHONE_HELP);
        }

        // What is left is the national number: nine digits that cannot start with a zero.
        if (!digits.matches("[1-9]\\d{8}")) {
            throw new IllegalArgumentException(PHONE_HELP);
        }
        return "+94" + digits;
    }

    /** Blank is fine — a district is optional — but a made-up one is not. */
    public static String requireKnownDistrict(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        return DISTRICTS.stream()
                .filter(district -> district.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Choose one of Sri Lanka's twenty-five districts."));
    }

    /** Blank is fine — a province is optional — but a made-up one is not. */
    public static String requireKnownProvince(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        return PROVINCES.stream()
                .filter(province -> province.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Choose one of Sri Lanka's nine provinces."));
    }
}
