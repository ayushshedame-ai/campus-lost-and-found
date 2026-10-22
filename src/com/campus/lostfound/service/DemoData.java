package com.campus.lostfound.service;

import java.time.LocalDate;

import com.campus.lostfound.exception.InvalidInputException;
import com.campus.lostfound.exception.StorageException;
import com.campus.lostfound.model.ItemCategory;
import com.campus.lostfound.model.Person;

/**
 * Adds a few sample reports so the system can be demonstrated quickly.
 */
public final class DemoData {

    private DemoData() {
        // utility class - no instances
    }

    public static void load(LostFoundService service) throws InvalidInputException, StorageException {
        LocalDate today = LocalDate.now();

        Person rahul = new Person("Rahul Sharma", "S1001", "9876543210", "rahul@example.com");
        Person priya = new Person("Priya Patil", "S1002", "9123456780", "priya@example.com");
        Person amit = new Person("Amit Kulkarni", "S1003", "9988776655", "amit@example.com");
        Person neha = new Person("Neha Joshi", "S2001", "9090909090", "neha@example.com");
        Person guard = new Person("Campus Security", "SEC001", "9000000001", "security@example.com");

        service.reportLost("Black Leather Wallet", "Contains college ID card and some cash",
                ItemCategory.ACCESSORIES, "Library", today.minusDays(3), rahul);
        service.reportLost("Samsung Galaxy Earbuds", "White charging case with a small scratch",
                ItemCategory.ELECTRONICS, "Canteen", today.minusDays(2), priya);
        service.reportLost("Engineering Mathematics Textbook", "Blue cover, name written on first page",
                ItemCategory.BOOKS, "Block B Classroom 204", today.minusDays(5), amit);

        service.reportFound("Black Wallet", "Leather wallet with an ID card inside",
                ItemCategory.ACCESSORIES, "Library Entrance", today.minusDays(2), neha, "Security Office");
        service.reportFound("Wireless Earbuds", "Samsung earbuds in a white case",
                ItemCategory.ELECTRONICS, "Canteen", today.minusDays(1), guard, "Security Office");
        service.reportFound("Blue Water Bottle", "Steel bottle with a sticker",
                ItemCategory.OTHER, "Playground", today.minusDays(1), guard, "Sports Office");
    }
}
