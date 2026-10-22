package com.campus.lostfound.model;

/**
 * Life-cycle of a lost/found report.
 * OPEN -> MATCHED (staff linked a lost and a found report) -> CLAIMED (item returned).
 */
public enum ItemStatus {

    OPEN("Open"),
    MATCHED("Matched"),
    CLAIMED("Claimed");

    private final String label;

    ItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
