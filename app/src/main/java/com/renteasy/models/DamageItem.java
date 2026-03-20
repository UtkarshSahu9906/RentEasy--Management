package com.renteasy.models;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;

/**
 * Damage record linked to a customer and optionally to a rental item.
 * Can be deducted from security deposit or charged separately.
 */
public class DamageItem {

    @DocumentId
    private String  id;
    private String  customerId;
    private String  rentalId;       // optional — link to a specific rental
    private String  rentalItemName; // copy of item name for display
    private String  description;    // what was damaged
    private double  amount;         // repair/replacement cost
    private boolean deductFromSecurity; // true = deduct from deposit, false = charge extra
    private boolean paid;
    private Date    reportedAt;

    public DamageItem() {}

    public DamageItem(String customerId, String rentalId, String rentalItemName,
                      String description, double amount, boolean deductFromSecurity) {
        this.customerId          = customerId;
        this.rentalId            = rentalId;
        this.rentalItemName      = rentalItemName;
        this.description         = description;
        this.amount              = amount;
        this.deductFromSecurity  = deductFromSecurity;
        this.paid                = false;
        this.reportedAt          = new Date();
    }

    public String  getId()                        { return id; }
    public void    setId(String v)                { id = v; }
    public String  getCustomerId()                { return customerId; }
    public void    setCustomerId(String v)        { customerId = v; }
    public String  getRentalId()                  { return rentalId; }
    public void    setRentalId(String v)          { rentalId = v; }
    public String  getRentalItemName()            { return rentalItemName; }
    public void    setRentalItemName(String v)    { rentalItemName = v; }
    public String  getDescription()               { return description; }
    public void    setDescription(String v)       { description = v; }
    public double  getAmount()                    { return amount; }
    public void    setAmount(double v)            { amount = v; }
    public boolean isDeductFromSecurity()         { return deductFromSecurity; }
    public void    setDeductFromSecurity(boolean v){ deductFromSecurity = v; }
    public boolean isPaid()                       { return paid; }
    public void    setPaid(boolean v)             { paid = v; }
    public Date    getReportedAt()                { return reportedAt; }
    public void    setReportedAt(Date v)          { reportedAt = v; }
}
