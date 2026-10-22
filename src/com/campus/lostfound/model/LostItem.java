package com.campus.lostfound.model;

import java.time.LocalDate;

/**
 * An item a student has lost. The contact person is the owner.
 */
public class LostItem extends Item {

    public LostItem(String id, String name, String description, ItemCategory category,
                    String location, LocalDate dateLost, Person owner) {
        super(id, name, description, category, location, dateLost, owner);
    }

    @Override
    public String getType() {
        return "LOST";
    }

    @Override
    public String getContactRole() {
        return "Owner";
    }
}
