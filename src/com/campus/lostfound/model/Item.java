package com.campus.lostfound.model;

import java.time.LocalDate;
import java.util.Objects;

import com.campus.lostfound.util.Validator;

/**
 * Abstract base class for every report in the system (OOP: abstraction,
 * inheritance). All fields are private and exposed through getters/setters
 * (encapsulation); fields that must never change after creation are final.
 */
public abstract class Item implements Comparable<Item> {

    public static final String SUMMARY_HEADER = String.format("%-5s %-26s %-12s %-22s %-11s %-8s",
            "ID", "Item", "Category", "Location", "Date", "Status");

    private final String id;
    private final String name;
    private final ItemCategory category;
    private final LocalDate date;
    private final Person contact;

    private String description;
    private String location;
    private ItemStatus status;
    private String matchedWithId; // ID of the linked report, or null

    protected Item(String id, String name, String description, ItemCategory category,
                   String location, LocalDate date, Person contact) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
        this.location = Objects.requireNonNull(location, "location");
        this.date = Objects.requireNonNull(date, "date");
        this.contact = Objects.requireNonNull(contact, "contact");
        this.status = ItemStatus.OPEN;
    }

    // ----- abstract behaviour implemented by subclasses (polymorphism) -----

    /** "LOST" or "FOUND". */
    public abstract String getType();

    /** Role of the contact person: "Owner" for lost items, "Finder" for found items. */
    public abstract String getContactRole();

    /** Extra detail lines specific to the subclass (may be empty). */
    protected String extraDetails() {
        return "";
    }

    // ----- getters / setters (encapsulation) -----

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }

    public ItemCategory getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = Objects.requireNonNull(location, "location");
    }

    public LocalDate getDate() {
        return date;
    }

    public Person getContact() {
        return contact;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public String getMatchedWithId() {
        return matchedWithId;
    }

    public void setMatchedWithId(String matchedWithId) {
        this.matchedWithId = matchedWithId;
    }

    public boolean isOpen() {
        return status == ItemStatus.OPEN;
    }

    // ----- display helpers -----

    /** One-line row for list views. */
    public String getSummaryLine() {
        return String.format("%-5s %-26s %-12s %-22s %-11s %-8s",
                id, truncate(name, 26), category.getLabel(), truncate(location, 22),
                Validator.formatDate(date), status.getLabel());
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    /** Multi-line detail view. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  ID           : %s (%s)%n", id, getType()));
        sb.append(String.format("  Item         : %s%n", name));
        sb.append(String.format("  Category     : %s%n", category.getLabel()));
        sb.append(String.format("  Description  : %s%n", description));
        sb.append(String.format("  Location     : %s%n", location));
        sb.append(String.format("  Date         : %s%n", Validator.formatDate(date)));
        sb.append(String.format("  Status       : %s%n", status.getLabel()));
        sb.append(String.format("  %-12s : %s%n", getContactRole(), contact));
        sb.append(extraDetails());
        if (matchedWithId != null) {
            sb.append(String.format("  Matched With : %s%n", matchedWithId));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return id.equals(((Item) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Natural order: oldest date first, then by ID. */
    @Override
    public int compareTo(Item other) {
        int byDate = date.compareTo(other.date);
        return byDate != 0 ? byDate : id.compareTo(other.id);
    }
}
