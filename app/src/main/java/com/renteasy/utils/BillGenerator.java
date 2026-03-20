package com.renteasy.utils;

import android.content.Context;
import android.content.Intent;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.renteasy.models.Customer;
import com.renteasy.models.DamageItem;
import com.renteasy.models.MissingItem;
import com.renteasy.models.Rental;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BillGenerator {

    private static final SimpleDateFormat dateFmt = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat dtFmt   = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat billNo  = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());

    // ── Print / PDF ───────────────────────────────────────────────────────
    public static void printBill(Context ctx, Customer customer,
                                 List<Rental> rentals,
                                 List<MissingItem> missing,
                                 List<DamageItem> damages) {
        WebView wv = new WebView(ctx);
        wv.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                PrintManager pm = (PrintManager) ctx.getSystemService(Context.PRINT_SERVICE);
                String job = "RentEasy_Bill_" + customer.getName().replace(" ", "_")
                        + "_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
                pm.print(job, view.createPrintDocumentAdapter(job),
                        new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .build());
            }
        });
        wv.loadDataWithBaseURL(null,
                buildHtml(ctx, customer, rentals, missing, damages),
                "text/HTML", "UTF-8", null);
    }

    // ── Share as text ─────────────────────────────────────────────────────
    public static String buildShareText(Context ctx, Customer customer,
                                        List<Rental> rentals,
                                        List<MissingItem> missing,
                                        List<DamageItem> damages) {
        String sym = CurrencyManager.getSymbol(ctx);
        Totals t   = calcTotals(ctx, rentals, missing, damages);

        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════╗\n");
        sb.append("║      BILLING STATEMENT     ║\n");
        sb.append("╚══════════════════════════╝\n\n");
        sb.append("Business : RentEasy\n");
        sb.append("Bill No  : #").append(billNo.format(new Date())).append("\n");
        sb.append("Date     : ").append(dateFmt.format(new Date())).append("\n");
        sb.append("Customer : ").append(customer.getName()).append("\n");
        if (customer.getPhone() != null)
            sb.append("Phone    : ").append(customer.getPhone()).append("\n");
        sb.append("\n─── RENTALS ────────────────\n");
        for (Rental r : rentals) {
            double cost = r.getTotalCost(ctx);
            sb.append(r.getItemName()).append("\n");
            sb.append("  Rate: ").append(sym).append(r.getRateLabel())
                    .append("  Duration: ").append(r.getDurationLabel()).append("\n");
            sb.append("  Status: ").append(r.isActive() ? "Active (running)" : "Returned").append("\n");
            sb.append("  Cost: ").append(sym).append(String.format("%.2f", cost)).append("\n");
            if (r.getAdvancePayment() > 0)
                sb.append("  Advance: -").append(sym).append(String.format("%.2f", r.getAdvancePayment())).append("\n");
            if (r.getSecurityAmount() > 0)
                sb.append("  Security: -").append(sym).append(String.format("%.2f", r.getSecurityAmount())).append("\n");
        }
        if (!damages.isEmpty()) {
            sb.append("\n─── DAMAGE CHARGES ─────────\n");
            for (DamageItem d : damages) {
                sb.append(d.getDescription());
                if (d.getRentalItemName() != null)
                    sb.append(" (").append(d.getRentalItemName()).append(")");
                sb.append("\n  ").append(sym).append(String.format("%.2f", d.getAmount()))
                        .append("  ").append(d.isDeductFromSecurity() ? "[From deposit]" : "[Extra charge]").append("\n");
            }
        }
        if (!missing.isEmpty()) {
            sb.append("\n─── MISSING ITEMS ──────────\n");
            for (MissingItem m : missing) {
                sb.append(m.getItemName()).append("\n");
                sb.append("  ").append(sym).append(String.format("%.2f", m.getPrice()))
                        .append("  [").append(m.isPaid() ? "Paid" : "UNPAID").append("]\n");
            }
        }
        sb.append("\n═══════════════════════════\n");
        sb.append("Rentals gross  : ").append(sym).append(String.format("%.2f", t.rentalGross)).append("\n");
        if (t.totalAdv  > 0) sb.append("(-) Advance    : ").append(sym).append(String.format("%.2f", t.totalAdv)).append("\n");
        if (t.totalSec  > 0) sb.append("(-) Security   : ").append(sym).append(String.format("%.2f", t.totalSec)).append("\n");
        if (t.damageExt > 0) sb.append("(+) Damage     : ").append(sym).append(String.format("%.2f", t.damageExt)).append("\n");
        if (t.missingU  > 0) sb.append("(+) Missing    : ").append(sym).append(String.format("%.2f", t.missingU)).append("\n");
        sb.append("═══════════════════════════\n");
        sb.append("TOTAL DUE : ").append(sym).append(String.format("%.2f", t.grand)).append("\n");
        sb.append("═══════════════════════════\n");
        sb.append("\nGenerated by RentEasy · ").append(dateFmt.format(new Date()));
        return sb.toString();
    }

    // ── HTML Bill ─────────────────────────────────────────────────────────
    private static String buildHtml(Context ctx, Customer customer,
                                    List<Rental> rentals,
                                    List<MissingItem> missing,
                                    List<DamageItem> damages) {
        String sym  = CurrencyManager.getSymbol(ctx);
        String code = CurrencyManager.getCode(ctx);
        String bn   = "#" + billNo.format(new Date());
        Totals t    = calcTotals(ctx, rentals, missing, damages);

        // ── Rental rows ──
        StringBuilder rr = new StringBuilder();
        int idx = 1;
        for (Rental r : rentals) {
            double cost = r.getTotalCost(ctx);
            String statusBadge = r.isActive()
                    ? "<span class='badge active'>Active</span>"
                    : "<span class='badge returned'>Returned</span>";
            rr.append("<tr>")
                    .append("<td class='num'>").append(idx++).append("</td>")
                    .append("<td><strong>").append(esc(r.getItemName())).append("</strong>")
                    .append(r.getNotes() != null && !r.getNotes().isEmpty()
                            ? "<br><small class='note'>" + esc(r.getNotes()) + "</small>" : "")
                    .append("</td>")
                    .append("<td>").append(sym).append(esc(r.getRateLabel())).append("</td>")
                    .append("<td>").append(esc(r.getDurationLabel())).append("</td>")
                    .append("<td class='center'>").append(statusBadge).append("</td>")
                    .append("<td class='right amount'>").append(sym).append(String.format("%.2f", cost)).append("</td>")
                    .append("</tr>");
            if (r.getAdvancePayment() > 0 || r.getSecurityAmount() > 0) {
                rr.append("<tr class='sub-row'>");
                rr.append("<td></td><td colspan='4' class='sub-label'>");
                if (r.getAdvancePayment() > 0)
                    rr.append("Advance paid: <span class='green'>-" + sym + String.format("%.2f", r.getAdvancePayment()) + "</span>&nbsp;&nbsp;");
                if (r.getSecurityAmount() > 0)
                    rr.append("Security deposit: <span class='purple'>-" + sym + String.format("%.2f", r.getSecurityAmount()) + "</span>");
                rr.append("</td><td></td></tr>");
            }
        }

        // ── Damage rows ──
        StringBuilder dr = new StringBuilder();
        if (!damages.isEmpty()) {
            dr.append("<tr class='section-row'><td colspan='5'><strong>Damage Charges</strong></td><td></td></tr>");
            for (DamageItem d : damages) {
                String type = d.isDeductFromSecurity()
                        ? "<span class='badge deposit'>From deposit</span>"
                        : "<span class='badge damage'>Extra charge</span>";
                dr.append("<tr>")
                        .append("<td class='num'>").append(idx++).append("</td>")
                        .append("<td>").append(esc(d.getDescription()))
                        .append(d.getRentalItemName() != null
                                ? " <small class='note'>(" + esc(d.getRentalItemName()) + ")</small>" : "")
                        .append("</td>")
                        .append("<td colspan='2'>").append(type).append("</td>")
                        .append("<td></td>")
                        .append("<td class='right amount red'>").append(sym).append(String.format("%.2f", d.getAmount())).append("</td>")
                        .append("</tr>");
            }
        }

        // ── Missing rows ──
        StringBuilder mr = new StringBuilder();
        if (!missing.isEmpty()) {
            mr.append("<tr class='section-row'><td colspan='5'><strong>Missing Items</strong></td><td></td></tr>");
            for (MissingItem m : missing) {
                String paidBadge = m.isPaid()
                        ? "<span class='badge returned'>Paid</span>"
                        : "<span class='badge damage'>Unpaid</span>";
                mr.append("<tr>")
                        .append("<td class='num'>").append(idx++).append("</td>")
                        .append("<td>").append(esc(m.getItemName()))
                        .append(m.getNotes() != null && !m.getNotes().isEmpty()
                                ? " <small class='note'>" + esc(m.getNotes()) + "</small>" : "")
                        .append("</td>")
                        .append("<td colspan='2'><small>" + (m.getReportedAt() != null ? dtFmt.format(m.getReportedAt()) : "") + "</small></td>")
                        .append("<td class='center'>").append(paidBadge).append("</td>")
                        .append("<td class='right amount red'>").append(m.isPaid() ? "" : sym + String.format("%.2f", m.getPrice())).append("</td>")
                        .append("</tr>");
            }
        }

        // ── Active rental note ──
        boolean hasActive = false;
        for (Rental r : rentals) if (r.isActive()) { hasActive = true; break; }
        String activeNote = hasActive
                ? "<p class='active-note'>* Active rentals are calculated up to the current date and time. Final amount may change when items are returned.</p>"
                : "";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "body { font-family: Arial, Helvetica, sans-serif; font-size:13px; color:#222; background:#fff; }"
                + ".page { padding:32px; max-width:800px; margin:0 auto; }"

                // Header
                + ".bill-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:28px; padding-bottom:20px; border-bottom:3px solid #1A237E; }"
                + ".company-name { font-size:28px; font-weight:bold; color:#1A237E; letter-spacing:-0.5px; }"
                + ".company-tagline { font-size:12px; color:#757575; margin-top:2px; }"
                + ".bill-info { text-align:right; }"
                + ".bill-title { font-size:18px; font-weight:bold; color:#1A237E; text-transform:uppercase; letter-spacing:1px; }"
                + ".bill-meta { font-size:12px; color:#555; margin-top:4px; line-height:1.7; }"

                // Customer + Bill details
                + ".info-section { display:flex; justify-content:space-between; margin-bottom:24px; }"
                + ".info-box { background:#F5F6FA; padding:14px 16px; border-radius:6px; min-width:48%; }"
                + ".info-box h3 { font-size:10px; text-transform:uppercase; letter-spacing:0.08em; color:#1A237E; margin-bottom:8px; }"
                + ".info-box p { font-size:13px; color:#333; line-height:1.7; }"
                + ".info-box .name { font-size:15px; font-weight:bold; color:#1A237E; }"

                // Table
                + "table { width:100%; border-collapse:collapse; margin-bottom:20px; }"
                + "thead tr { background:#1A237E; color:#fff; }"
                + "thead th { padding:10px 12px; text-align:left; font-size:11px; font-weight:normal; letter-spacing:0.04em; text-transform:uppercase; }"
                + "thead th.right { text-align:right; }"
                + "thead th.center { text-align:center; }"
                + "tbody tr { border-bottom:1px solid #EEEEEE; }"
                + "tbody tr:nth-child(even) { background:#FAFAFA; }"
                + "tbody td { padding:10px 12px; vertical-align:top; font-size:13px; color:#333; }"
                + "td.num { color:#9E9E9E; font-size:11px; width:28px; }"
                + "td.right { text-align:right; }"
                + "td.center { text-align:center; }"
                + "td.amount { font-weight:bold; font-size:14px; color:#1A237E; }"
                + "td.amount.red { color:#B71C1C; }"
                + ".sub-row td { padding-top:2px; padding-bottom:8px; background:#FAFAFA; border-bottom:1px solid #EEEEEE; }"
                + ".sub-label { font-size:11px; color:#757575; }"
                + ".section-row td { background:#F0F4FF; padding:8px 12px; font-size:12px; color:#1A237E; border-bottom:none; }"
                + "small { font-size:11px; }"
                + ".note { color:#9E9E9E; }"

                // Badges
                + ".badge { display:inline-block; padding:2px 8px; border-radius:20px; font-size:10px; font-weight:bold; }"
                + ".badge.active { background:#E8F5E9; color:#2E7D32; }"
                + ".badge.returned { background:#E8EAF6; color:#3949AB; }"
                + ".badge.damage { background:#FFEBEE; color:#B71C1C; }"
                + ".badge.deposit { background:#EDE7F6; color:#4527A0; }"

                // Colors
                + ".green { color:#2E7D32; font-weight:bold; }"
                + ".purple { color:#4527A0; font-weight:bold; }"

                // Totals
                + ".totals-section { display:flex; justify-content:flex-end; margin-bottom:20px; }"
                + ".totals-box { width:320px; border:1px solid #E0E0E0; border-radius:8px; overflow:hidden; }"
                + ".tot-row { display:flex; justify-content:space-between; padding:9px 14px; border-bottom:1px solid #EEEEEE; font-size:13px; }"
                + ".tot-row .lbl { color:#555; }"
                + ".tot-row .val { font-weight:bold; }"
                + ".tot-row.deduct .val { color:#2E7D32; }"
                + ".tot-row.charge .val { color:#B71C1C; }"
                + ".tot-grand { display:flex; justify-content:space-between; padding:12px 14px; background:#1A237E; }"
                + ".tot-grand .lbl { color:#9FA8DA; font-size:13px; font-weight:bold; }"
                + ".tot-grand .val { color:#fff; font-size:18px; font-weight:bold; }"

                // Footer
                + ".active-note { font-size:11px; color:#E65100; background:#FFF8E1; padding:8px 12px; border-radius:6px; margin-bottom:16px; border-left:3px solid #E65100; }"
                + ".bill-footer { margin-top:24px; padding-top:16px; border-top:1px solid #EEEEEE; display:flex; justify-content:space-between; font-size:11px; color:#9E9E9E; }"
                + ".policy-note { font-size:10px; color:#BDBDBD; margin-top:4px; }"
                + "</style></head><body><div class='page'>"

                // Header
                + "<div class='bill-header'>"
                + "<div><div class='company-name'>RentEasy</div><div class='company-tagline'>Rental Management</div></div>"
                + "<div class='bill-info'><div class='bill-title'>Invoice</div>"
                + "<div class='bill-meta'>Bill No: <strong>" + bn + "</strong><br>Date: <strong>" + dateFmt.format(new Date()) + "</strong><br>Currency: <strong>" + sym + " (" + code + ")</strong></div>"
                + "</div></div>"

                // Info section
                + "<div class='info-section'>"
                + "<div class='info-box'><h3>Bill To</h3>"
                + "<p class='name'>" + esc(customer.getName()) + "</p>"
                + (customer.getEmail() != null ? "<p>" + esc(customer.getEmail()) + "</p>" : "")
                + (customer.getPhone() != null ? "<p>" + esc(customer.getPhone()) + "</p>" : "")
                + "</div>"
                + "<div class='info-box' style='text-align:right'><h3>Bill Details</h3>"
                + "<p>Billing policy</p>"
                + "<p style='font-size:11px;color:#757575'>" + esc(BillingPolicy.getDailyPolicyLabel(ctx)) + "</p>"
                + "<p style='font-size:11px;color:#757575'>" + esc(BillingPolicy.getMonthlyPolicyLabel(ctx)) + "</p>"
                + "</div></div>"

                // Items table
                + "<table><thead><tr>"
                + "<th>#</th><th>Description</th><th>Rate</th><th>Duration</th><th class='center'>Status</th><th class='right'>Amount</th>"
                + "</tr></thead><tbody>"
                + rr + dr + mr
                + "</tbody></table>"

                // Active note
                + activeNote

                // Totals
                + "<div class='totals-section'><div class='totals-box'>"
                + "<div class='tot-row'><span class='lbl'>Rentals gross</span><span class='val'>" + sym + String.format("%.2f", t.rentalGross) + "</span></div>"
                + (t.totalAdv  > 0 ? "<div class='tot-row deduct'><span class='lbl'>(-) Advance paid</span><span class='val'>-" + sym + String.format("%.2f", t.totalAdv) + "</span></div>" : "")
                + (t.totalSec  > 0 ? "<div class='tot-row deduct'><span class='lbl'>(-) Security deposit</span><span class='val'>-" + sym + String.format("%.2f", t.totalSec) + "</span></div>" : "")
                + (t.damageExt > 0 ? "<div class='tot-row charge'><span class='lbl'>(+) Damage charges</span><span class='val'>+" + sym + String.format("%.2f", t.damageExt) + "</span></div>" : "")
                + (t.missingU  > 0 ? "<div class='tot-row charge'><span class='lbl'>(+) Unpaid missing items</span><span class='val'>+" + sym + String.format("%.2f", t.missingU) + "</span></div>" : "")
                + "<div class='tot-grand'><span class='lbl'>TOTAL DUE</span><span class='val'>" + sym + String.format("%.2f", t.grand) + "</span></div>"
                + "</div></div>"

                // Footer
                + "<div class='bill-footer'>"
                + "<div><div>Thank you for your business.</div>"
                + "<div class='policy-note'>Active rentals calculated as of " + dtFmt.format(new Date()) + "</div></div>"
                + "<div style='text-align:right'><div>Generated by RentEasy</div><div class='policy-note'>" + dateFmt.format(new Date()) + "</div></div>"
                + "</div>"
                + "</div></body></html>";
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static class Totals {
        double rentalGross, totalAdv, totalSec, damageExt, missingU, grand;
    }

    private static Totals calcTotals(Context ctx, List<Rental> rentals,
                                     List<MissingItem> missing, List<DamageItem> damages) {
        Totals t = new Totals();
        for (Rental r : rentals) {
            t.rentalGross += r.getTotalCost(ctx);
            t.totalAdv    += r.getAdvancePayment();
            t.totalSec    += r.getSecurityAmount();
        }
        for (DamageItem d : damages)  if (!d.isDeductFromSecurity()) t.damageExt += d.getAmount();
        for (MissingItem m : missing) if (!m.isPaid())               t.missingU  += m.getPrice();
        t.grand = t.rentalGross - t.totalAdv - t.totalSec + t.damageExt + t.missingU;
        return t;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}