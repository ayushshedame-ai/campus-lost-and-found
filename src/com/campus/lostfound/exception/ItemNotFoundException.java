package com.campus.lostfound.exception;

/**
 * Checked exception thrown when a lost/found item ID does not exist.
 */
public class ItemNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String itemId;

    public ItemNotFoundException(String itemId) {
        super("No item found with ID '" + itemId + "'.");
        this.itemId = itemId;
    }

    public String getItemId() {
        return itemId;
    }
}
