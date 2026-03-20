package com.renteasy.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

public class Customer {

    @DocumentId
    private String id;
    private String ownerId;
    private String name;
    private String email;
    private String phone;

    public Customer() {}

    public Customer(String ownerId, String name, String email, String phone) {
        this.ownerId = ownerId;
        this.name    = name;
        this.email   = email;
        this.phone   = phone;
    }

    /** Returns up to 2 uppercase initials from the name, e.g. "Juan Dela Cruz" → "JD" */
    @Exclude
    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    public String getId()              { return id; }
    public void   setId(String v)      { id = v; }

    public String getOwnerId()         { return ownerId; }
    public void   setOwnerId(String v) { ownerId = v; }

    public String getName()            { return name; }
    public void   setName(String v)    { name = v; }

    public String getEmail()           { return email; }
    public void   setEmail(String v)   { email = v; }

    public String getPhone()           { return phone; }
    public void   setPhone(String v)   { phone = v; }
}
