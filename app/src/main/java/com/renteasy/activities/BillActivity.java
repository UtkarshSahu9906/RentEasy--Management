package com.renteasy.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.*;
import com.renteasy.activities.databinding.ActivityBillBinding;
import com.renteasy.adapters.DamageAdapter;
import com.renteasy.adapters.MissingItemAdapter;
import com.renteasy.adapters.RentalAdapter;
import com.renteasy.models.*;
import com.renteasy.utils.BillGenerator;
import com.renteasy.utils.CurrencyManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Full-screen billing activity for one customer.
 * Handles active-item policy, PDF generation, share, and saving bill to history.
 */
public class BillActivity extends AppCompatActivity {

    // Policy constants for active items
    public static final String POLICY_CALCULATE_NOW = "CALCULATE_NOW";
    public static final String POLICY_MARK_PENDING  = "MARK_PENDING";

    private ActivityBillBinding binding;
    private FirebaseFirestore   db;
    private Customer            customer;
    private String              customerId;
    private String              activeItemPolicy = POLICY_CALCULATE_NOW;

    private List<Rental>      rentals  = new ArrayList<>();
    private List<MissingItem> missing  = new ArrayList<>();
    private List<DamageItem>  damages  = new ArrayList<>();

    private RentalAdapter      rentalAdapter;
    private MissingItemAdapter missingAdapter;
    private DamageAdapter      damageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Generate Bill");

        customerId = getIntent().getStringExtra("customerId");
        customer   = new Customer();
        customer.setId(customerId);
        customer.setName(getIntent().getStringExtra("customerName"));
        customer.setEmail(getIntent().getStringExtra("customerEmail"));
        customer.setPhone(getIntent().getStringExtra("customerPhone"));

        db = FirebaseFirestore.getInstance();

        setupAdapters();
        loadData();
        setupButtons();
    }

    private void setupAdapters() {
        rentalAdapter  = new RentalAdapter(new ArrayList<>(), r -> {});
        missingAdapter = new MissingItemAdapter(new ArrayList<>(), m -> {});
        damageAdapter  = new DamageAdapter(new ArrayList<>(), d -> {});

        binding.recyclerRentals.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerRentals.setAdapter(rentalAdapter);
        binding.recyclerMissing.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMissing.setAdapter(missingAdapter);
        binding.recyclerDamages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerDamages.setAdapter(damageAdapter);
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Load rentals
        db.collection("customers").document(customerId).collection("rentals")
                .get().addOnSuccessListener(snap -> {
                    rentals = snap.toObjects(Rental.class);
                    rentalAdapter.updateList(rentals);

                    // Check for active rentals → show policy card
                    boolean hasActive = false;
                    for (Rental r : rentals) if (r.isActive()) { hasActive = true; break; }
                    binding.cardActivePolicy.setVisibility(hasActive ? View.VISIBLE : View.GONE);

                    loadMissing();
                });
    }

    private void loadMissing() {
        db.collection("customers").document(customerId).collection("missingItems")
                .get().addOnSuccessListener(snap -> {
                    missing = snap.toObjects(MissingItem.class);
                    missingAdapter.updateList(missing);
                    loadDamages();
                });
    }

    private void loadDamages() {
        db.collection("customers").document(customerId).collection("damages")
                .get().addOnSuccessListener(snap -> {
                    damages = snap.toObjects(DamageItem.class);
                    damageAdapter.updateList(damages);
                    binding.progressBar.setVisibility(View.GONE);
                    refreshTotals();
                });
    }

    private void setupButtons() {
        // Active item policy toggle
        binding.btnPolicyNow.setOnClickListener(v -> {
            activeItemPolicy = POLICY_CALCULATE_NOW;
            updatePolicyUI();
            refreshTotals();
        });
        binding.btnPolicyPending.setOnClickListener(v -> {
            activeItemPolicy = POLICY_MARK_PENDING;
            updatePolicyUI();
            refreshTotals();
        });
        updatePolicyUI();

        // Print / PDF
        binding.btnPrintPdf.setOnClickListener(v -> {
            saveBillToHistory();
            BillGenerator.printBill(this, customer,
                    getEffectiveRentals(), missing, damages);
        });

        // Share text
        binding.btnShare.setOnClickListener(v -> {
            saveBillToHistory();
            String text = BillGenerator.buildShareText(this, customer,
                    getEffectiveRentals(), missing, damages);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(share, "Share bill via..."));
        });

        // Save to history only
        binding.btnSaveHistory.setOnClickListener(v -> {
            saveBillToHistory();
            Toast.makeText(this, "Bill saved to history.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updatePolicyUI() {
        boolean now = POLICY_CALCULATE_NOW.equals(activeItemPolicy);
        binding.btnPolicyNow.setSelected(now);
        binding.btnPolicyPending.setSelected(!now);
        int nowBg     = now     ? 0xFF1A237E : 0x00000000;
        int pendingBg = !now    ? 0xFF1A237E : 0x00000000;
        int nowTxt    = now     ? 0xFFFFFFFF : 0xFF1A237E;
        int pendingTxt= !now    ? 0xFFFFFFFF : 0xFF1A237E;
        binding.btnPolicyNow.setBackgroundColor(nowBg);
        binding.btnPolicyNow.setTextColor(nowTxt);
        binding.btnPolicyPending.setBackgroundColor(pendingBg);
        binding.btnPolicyPending.setTextColor(pendingTxt);

        if (now) {
            binding.tvPolicyNote.setText(
                "Active rentals are calculated using today's date and time as the end point.");
        } else {
            binding.tvPolicyNote.setText(
                "Active rentals will be marked as PENDING. They are excluded from the total and must be settled separately when returned.");
        }
        refreshTotals();
    }

    /**
     * Returns rentals adjusted for the current policy.
     * POLICY_CALCULATE_NOW  → all rentals as-is (active ones use current time)
     * POLICY_MARK_PENDING   → only returned rentals included in totals
     */
    private List<Rental> getEffectiveRentals() {
        if (POLICY_CALCULATE_NOW.equals(activeItemPolicy)) return rentals;
        List<Rental> returned = new ArrayList<>();
        for (Rental r : rentals) if (!r.isActive()) returned.add(r);
        return returned;
    }

    private void refreshTotals() {
        String sym = CurrencyManager.getSymbol(this);
        List<Rental> effective = getEffectiveRentals();

        double rentalGross = 0, totalAdv = 0, totalSec = 0, damageExt = 0, missingU = 0;
        int    activeCount = 0;

        for (Rental r : rentals) if (r.isActive()) activeCount++;
        for (Rental r : effective) {
            rentalGross += r.getTotalCost(this);
            totalAdv    += r.getAdvancePayment();
            totalSec    += r.getSecurityAmount();
        }
        for (DamageItem d  : damages) if (!d.isDeductFromSecurity()) damageExt += d.getAmount();
        for (MissingItem m : missing)  if (!m.isPaid())              missingU  += m.getPrice();

        double grand = rentalGross - totalAdv - totalSec + damageExt + missingU;

        binding.tvRentalGross.setText(sym + String.format("%.2f", rentalGross));
        binding.tvAdvance.setText("-" + sym + String.format("%.2f", totalAdv));
        binding.tvSecurity.setText("-" + sym + String.format("%.2f", totalSec));
        binding.tvDamage.setText("+" + sym + String.format("%.2f", damageExt));
        binding.tvMissing.setText("+" + sym + String.format("%.2f", missingU));
        binding.tvGrandTotal.setText(sym + String.format("%.2f", Math.max(0, grand)));

        if (POLICY_MARK_PENDING.equals(activeItemPolicy) && activeCount > 0) {
            binding.tvPendingWarning.setVisibility(View.VISIBLE);
            binding.tvPendingWarning.setText(
                    activeCount + " active rental(s) are PENDING and excluded from this total.");
        } else {
            binding.tvPendingWarning.setVisibility(View.GONE);
        }
    }

    private void saveBillToHistory() {
        String sym = CurrencyManager.getSymbol(this);
        List<Rental> effective = getEffectiveRentals();

        double rentalGross = 0, totalAdv = 0, totalSec = 0, damageExt = 0, missingU = 0;
        for (Rental r : effective) {
            rentalGross += r.getTotalCost(this);
            totalAdv    += r.getAdvancePayment();
            totalSec    += r.getSecurityAmount();
        }
        for (DamageItem d  : damages) if (!d.isDeductFromSecurity()) damageExt += d.getAmount();
        for (MissingItem m : missing)  if (!m.isPaid())              missingU  += m.getPrice();

        Bill bill = new Bill();
        bill.setCustomerId(customerId);
        bill.setCustomerName(customer.getName());
        bill.setCustomerPhone(customer.getPhone());
        bill.setCustomerEmail(customer.getEmail());
        bill.setGeneratedAt(new Date());
        bill.setStatus(POLICY_MARK_PENDING.equals(activeItemPolicy) ? Bill.STATUS_PENDING : Bill.STATUS_FINAL);
        bill.setCurrencySymbol(sym);
        bill.setCurrencyCode(CurrencyManager.getCode(this));
        bill.setActiveItemPolicy(activeItemPolicy);
        bill.setRentalGross(rentalGross);
        bill.setTotalAdvance(totalAdv);
        bill.setTotalSecurity(totalSec);
        bill.setDamageCharges(damageExt);
        bill.setMissingCharges(missingU);
        bill.setGrandTotal(Math.max(0, rentalGross - totalAdv - totalSec + damageExt + missingU));
        bill.setTotalItems(rentals.size() + missing.size() + damages.size());
        bill.setBillingPolicyNote(
                com.renteasy.utils.BillingPolicy.getDailyPolicyLabel(this) + " | " +
                com.renteasy.utils.BillingPolicy.getMonthlyPolicyLabel(this));

        db.collection("customers").document(customerId)
                .collection("bills").add(bill)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not save to history: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
