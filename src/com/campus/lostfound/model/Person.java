package com.campus.lostfound.model;

import java.util.Objects;

/**
 * Contact / owner information. Immutable, so it is safe to share.
 * For a lost item this is the owner; for a found item it is the finder.
 */
public class Person {

    private final String name;
    private final String studentId;
    private final String phone;
    private final String email;

    public Person(String name, String studentId, String phone, String email) {
        this.name = Objects.requireNonNull(name, "name");
        this.studentId = Objects.requireNonNull(studentId, "studentId");
        this.phone = Objects.requireNonNull(phone, "phone");
        this.email = Objects.requireNonNull(email, "email");
    }

    public String getName() {
        return name;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return name + " (ID: " + studentId + ", Ph: " + phone + ", Email: " + email + ")";
    }
}
