package com.renteasy.models;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;

public class MissingItem {

    @DocumentId
    private String  id;
    private String  customerId;
    private String  itemName;
    private String  notes;
    private double  price;
    private boolean paid;
    private Date    reportedAt;

    public MissingItem() {}

    public MissingItem(String customerId, String itemName, double price) {
        this.customerId = customerId;
        this.itemName   = itemName;
        this.price      = price;
        this.paid       = false;
        this.reportedAt = new Date();
    }

    public String  getId()               { return id; }
    public void    setId(String v)       { id = v; }

    public String  getCustomerId()           { return customerId; }
    public void    setCustomerId(String v)   { customerId = v; }

    public String  getItemName()             { return itemName; }
    public void    setItemName(String v)     { itemName = v; }

    public String  getNotes()                { return notes; }
    public void    setNotes(String v)        { notes = v; }

    public double  getPrice()                { return price; }
    public void    setPrice(double v)        { price = v; }

    public boolean isPaid()                  { return paid; }
    public void    setPaid(boolean v)        { paid = v; }

    public Date    getReportedAt()           { return reportedAt; }
    public void    setReportedAt(Date v)     { reportedAt = v; }
}
