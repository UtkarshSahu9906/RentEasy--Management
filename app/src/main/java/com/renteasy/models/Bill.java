package com.renteasy.models;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;
import java.util.List;

/**
 * A saved bill snapshot stored in Firestore under:
 *   customers/{customerId}/bills/{billId}
 *
 * Stores totals + line items at the moment of generation.
 * Active-item policy is recorded so the bill is reproducible.
 */
public class Bill {

    public static final String STATUS_DRAFT   = "draft";
    public static final String STATUS_FINAL   = "final";
    public static final String STATUS_PENDING = "pending"; // has active rentals, not finalized

    @DocumentId
    private String id;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private Date   generatedAt;
    private String status;          // draft | final | pending
    private String currencySymbol;
    private String currencyCode;
    private String activeItemPolicy; // "CALCULATE_NOW" | "MARK_PENDING"
    private double rentalGross;
    private double totalAdvance;
    private double totalSecurity;
    private double damageCharges;
    private double missingCharges;
    private double grandTotal;
    private int    totalItems;
    private String billingPolicyNote;

    public Bill() {}

    // getters / setters
    public String getId()                       { return id; }
    public void   setId(String v)               { id = v; }
    public String getCustomerId()               { return customerId; }
    public void   setCustomerId(String v)       { customerId = v; }
    public String getCustomerName()             { return customerName; }
    public void   setCustomerName(String v)     { customerName = v; }
    public String getCustomerPhone()            { return customerPhone; }
    public void   setCustomerPhone(String v)    { customerPhone = v; }
    public String getCustomerEmail()            { return customerEmail; }
    public void   setCustomerEmail(String v)    { customerEmail = v; }
    public Date   getGeneratedAt()              { return generatedAt; }
    public void   setGeneratedAt(Date v)        { generatedAt = v; }
    public String getStatus()                   { return status; }
    public void   setStatus(String v)           { status = v; }
    public String getCurrencySymbol()           { return currencySymbol; }
    public void   setCurrencySymbol(String v)   { currencySymbol = v; }
    public String getCurrencyCode()             { return currencyCode; }
    public void   setCurrencyCode(String v)     { currencyCode = v; }
    public String getActiveItemPolicy()         { return activeItemPolicy; }
    public void   setActiveItemPolicy(String v) { activeItemPolicy = v; }
    public double getRentalGross()              { return rentalGross; }
    public void   setRentalGross(double v)      { rentalGross = v; }
    public double getTotalAdvance()             { return totalAdvance; }
    public void   setTotalAdvance(double v)     { totalAdvance = v; }
    public double getTotalSecurity()            { return totalSecurity; }
    public void   setTotalSecurity(double v)    { totalSecurity = v; }
    public double getDamageCharges()            { return damageCharges; }
    public void   setDamageCharges(double v)    { damageCharges = v; }
    public double getMissingCharges()           { return missingCharges; }
    public void   setMissingCharges(double v)   { missingCharges = v; }
    public double getGrandTotal()               { return grandTotal; }
    public void   setGrandTotal(double v)       { grandTotal = v; }
    public int    getTotalItems()               { return totalItems; }
    public void   setTotalItems(int v)          { totalItems = v; }
    public String getBillingPolicyNote()        { return billingPolicyNote; }
    public void   setBillingPolicyNote(String v){ billingPolicyNote = v; }
}
