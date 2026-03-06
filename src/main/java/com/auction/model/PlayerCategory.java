package com.auction.model;

/**
 * Player categories based on age groups.
 */
public enum PlayerCategory {
    OPEN("Open", "No age restriction"),
    ABOVE_30("30+", "Players aged 30 and above"),
    ABOVE_35("35+", "Players aged 35 and above"),
    ABOVE_40("40+", "Players aged 40 and above");

    private final String label;
    private final String description;

    PlayerCategory(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Find a PlayerCategory from a label or name string.
     * Accepts: enum name ("ABOVE_30"), label ("30+"), or common variants ("above 30", "30 plus").
     * Returns null if no match found.
     */
    public static PlayerCategory fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();

        // Try exact enum name first (case-insensitive)
        for (PlayerCategory cat : values()) {
            if (cat.name().equalsIgnoreCase(v)) return cat;
        }

        // Try matching by label (e.g. "30+", "Open")
        for (PlayerCategory cat : values()) {
            if (cat.label.equalsIgnoreCase(v)) return cat;
        }

        // Try common patterns: "above 30", "30 plus", "30+", "above30", etc.
        String normalized = v.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (normalized.equals("open")) return OPEN;
        if (normalized.contains("40")) return ABOVE_40;
        if (normalized.contains("35")) return ABOVE_35;
        if (normalized.contains("30")) return ABOVE_30;

        return null;
    }
}
