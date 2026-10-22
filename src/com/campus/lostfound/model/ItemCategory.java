package com.campus.lostfound.model;

import com.campus.lostfound.exception.InvalidInputException;

/**
 * Categories of items that can be reported. Used by the matching engine
 * (items of the same category score higher).
 */
public enum ItemCategory {

    ELECTRONICS("Electronics"),
    DOCUMENTS("Documents"),
    ACCESSORIES("Accessories"),
    BOOKS("Books"),
    CLOTHING("Clothing"),
    KEYS("Keys"),
    BAGS("Bags"),
    OTHER("Other");

    private final String label;

    ItemCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Converts user input (menu number such as "3", or the category name)
     * into a category.
     */
    public static ItemCategory fromInput(String input) throws InvalidInputException {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidInputException("Category cannot be empty.");
        }
        String text = input.trim();
        ItemCategory[] all = values();
        try {
            int number = Integer.parseInt(text);
            if (number >= 1 && number <= all.length) {
                return all[number - 1];
            }
        } catch (NumberFormatException e) {
            // not a number - fall through and try matching by name
        }
        for (ItemCategory category : all) {
            if (category.name().equalsIgnoreCase(text) || category.label.equalsIgnoreCase(text)) {
                return category;
            }
        }
        throw new InvalidInputException("Unknown category '" + text + "'.");
    }

    @Override
    public String toString() {
        return label;
    }
}
