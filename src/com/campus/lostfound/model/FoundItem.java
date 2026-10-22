package com.campus.lostfound.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * An item somebody found on campus. The contact person is the finder, and the
 * item additionally records where it is currently stored.
 */
public class FoundItem extends Item {

    private String storedAt;

    public FoundItem(String id, String name, String description, ItemCategory category,
                     String location, LocalDate dateFound, Person finder, String storedAt) {
        super(id, name, description, category, location, dateFound, finder);
        this.storedAt = Objects.requireNonNull(storedAt, "storedAt");
    }

    public String getStoredAt() {
        return storedAt;
    }

    public void setStoredAt(String storedAt) {
        this.storedAt = Objects.requireNonNull(storedAt, "storedAt");
    }

    @Override
    public String getType() {
        return "FOUND";
    }

    @Override
    public String getContactRole() {
        return "Finder";
    }

    @Override
    protected String extraDetails() {
        return String.format("  Stored At    : %s%n", storedAt);
    }
}
