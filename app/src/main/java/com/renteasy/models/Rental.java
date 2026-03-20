package com.renteasy.models;

import android.content.Context;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.renteasy.utils.BillingPolicy;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Rental model.
 * Use getTotalCost(context) for policy-aware billing.
 * Use getTotalCost() for Firestore-safe calls (falls back to EXACT_HOURS).
 */
public class Rental {

    public static final String TYPE_MONTHLY = "MONTHLY";
    public static final String TYPE_DAILY   = "DAILY";
    public static final String TYPE_HOURLY  = "HOURLY";

    @DocumentId
    private String  id;
    private String  customerId;
    private String  itemName;
    private String  notes;
    private String  rateType;
    private double  monthlyRate;
    private double  dailyRate;
    private double  hourlyRate;
    private double  advancePayment;
    private double  securityAmount;
    private String  status;
    private Date    startDate;
    private Date    endDate;

    public Rental() {}

    // ── Static factories ─────────────────────────────────────────────────

    public static Rental monthly(String cId, String item, double rate,
                                  double adv, double sec, String notes) {
        Rental r = new Rental();
        r.customerId = cId; r.itemName = item; r.notes = notes;
        r.rateType = TYPE_MONTHLY; r.monthlyRate = rate;
        r.dailyRate = rate / 30.0; r.hourlyRate = r.dailyRate / 24.0;
        r.advancePayment = adv; r.securityAmount = sec;
        r.status = "active"; r.startDate = new Date();
        return r;
    }

    public static Rental daily(String cId, String item, double rate,
                                double adv, double sec, String notes) {
        Rental r = new Rental();
        r.customerId = cId; r.itemName = item; r.notes = notes;
        r.rateType = TYPE_DAILY; r.dailyRate = rate;
        r.hourlyRate = rate / 24.0;
        r.advancePayment = adv; r.securityAmount = sec;
        r.status = "active"; r.startDate = new Date();
        return r;
    }

    public static Rental hourly(String cId, String item, double rate,
                                 double adv, double sec, String notes) {
        Rental r = new Rental();
        r.customerId = cId; r.itemName = item; r.notes = notes;
        r.rateType = TYPE_HOURLY; r.hourlyRate = rate;
        r.advancePayment = adv; r.securityAmount = sec;
        r.status = "active"; r.startDate = new Date();
        return r;
    }

    // ── Duration ─────────────────────────────────────────────────────────

    @Exclude
    public long elapsedMillis() {
        Date from = (startDate != null) ? startDate : new Date();
        Date to   = (endDate   != null) ? endDate   : new Date();
        return Math.max(0, to.getTime() - from.getTime());
    }

    // ── Cost (policy-aware, requires Context) ────────────────────────────

    @Exclude
    public double getTotalCost(Context ctx) {
        long ms = elapsedMillis();
        if (ms <= 0) return 0;
        String type = (rateType != null) ? rateType : TYPE_DAILY;

        if (TYPE_MONTHLY.equals(type)) {
            return BillingPolicy.calcMonthlyCost(ctx, monthlyRate, ms);
        } else if (TYPE_DAILY.equals(type)) {
            return BillingPolicy.calcDailyCost(ctx, dailyRate, ms);
        } else {
            // HOURLY always rounds up — no policy
            long ceilHours = (ms + TimeUnit.HOURS.toMillis(1) - 1) / TimeUnit.HOURS.toMillis(1);
            return ceilHours * hourlyRate;
        }
    }

    /** Fallback (no Context) — uses EXACT_HOURS for both DAILY and MONTHLY */
    @Exclude
    public double getTotalCost() {
        long ms = elapsedMillis();
        if (ms <= 0) return 0;
        String type = (rateType != null) ? rateType : TYPE_DAILY;
        if (TYPE_MONTHLY.equals(type)) {
            long fullMonths  = ms / TimeUnit.DAYS.toMillis(30);
            long remMo       = ms % TimeUnit.DAYS.toMillis(30);
            long fullDays    = remMo / TimeUnit.DAYS.toMillis(1);
            long remDay      = remMo % TimeUnit.DAYS.toMillis(1);
            long ceilH       = (remDay + TimeUnit.HOURS.toMillis(1) - 1) / TimeUnit.HOURS.toMillis(1);
            return (fullMonths * monthlyRate) + (fullDays * dailyRate) + (ceilH * hourlyRate);
        } else if (TYPE_DAILY.equals(type)) {
            long fullDays  = ms / TimeUnit.DAYS.toMillis(1);
            long remDay    = ms % TimeUnit.DAYS.toMillis(1);
            long ceilH     = (remDay + TimeUnit.HOURS.toMillis(1) - 1) / TimeUnit.HOURS.toMillis(1);
            return (fullDays * dailyRate) + (ceilH * hourlyRate);
        } else {
            long ceilH = (ms + TimeUnit.HOURS.toMillis(1) - 1) / TimeUnit.HOURS.toMillis(1);
            return ceilH * hourlyRate;
        }
    }

    @Exclude
    public double getNetDue(Context ctx) {
        return Math.max(0, getTotalCost(ctx) - advancePayment - securityAmount);
    }

    @Exclude
    public String getDurationLabel() {
        long ms = elapsedMillis();
        if (ms <= 0) return "0 hr";
        long ceilH  = (ms + TimeUnit.HOURS.toMillis(1) - 1) / TimeUnit.HOURS.toMillis(1);
        long months = ceilH / (30 * 24);
        long rem1   = ceilH % (30 * 24);
        long days   = rem1  / 24;
        long hours  = rem1  % 24;
        StringBuilder sb = new StringBuilder();
        if (months > 0) sb.append(months).append(months == 1 ? " mo "  : " mos ");
        if (days   > 0) sb.append(days).append(days   == 1 ? " day " : " days ");
        if (hours  > 0 || sb.length() == 0) sb.append(hours).append(" hr");
        return sb.toString().trim();
    }

    @Exclude
    public String getRateLabel() {
        String type = (rateType != null) ? rateType : TYPE_DAILY;
        if (TYPE_MONTHLY.equals(type)) return String.format("%.2f/mo",  monthlyRate);
        if (TYPE_DAILY.equals(type))   return String.format("%.2f/day", dailyRate);
        return String.format("%.2f/hr", hourlyRate);
    }

    @Exclude public boolean isActive() { return "active".equals(status); }

    // getters/setters
    public String getId()                    { return id; }
    public void   setId(String v)            { id = v; }
    public String getCustomerId()            { return customerId; }
    public void   setCustomerId(String v)    { customerId = v; }
    public String getItemName()              { return itemName; }
    public void   setItemName(String v)      { itemName = v; }
    public String getNotes()                 { return notes; }
    public void   setNotes(String v)         { notes = v; }
    public String getRateType()              { return rateType; }
    public void   setRateType(String v)      { rateType = v; }
    public double getMonthlyRate()           { return monthlyRate; }
    public void   setMonthlyRate(double v)   { monthlyRate = v; }
    public double getDailyRate()             { return dailyRate; }
    public void   setDailyRate(double v)     { dailyRate = v; }
    public double getHourlyRate()            { return hourlyRate; }
    public void   setHourlyRate(double v)    { hourlyRate = v; }
    public double getAdvancePayment()        { return advancePayment; }
    public void   setAdvancePayment(double v){ advancePayment = v; }
    public double getSecurityAmount()        { return securityAmount; }
    public void   setSecurityAmount(double v){ securityAmount = v; }
    public String getStatus()                { return status; }
    public void   setStatus(String v)        { status = v; }
    public Date   getStartDate()             { return startDate; }
    public void   setStartDate(Date v)       { startDate = v; }
    public Date   getEndDate()               { return endDate; }
    public void   setEndDate(Date v)         { endDate = v; }
}
