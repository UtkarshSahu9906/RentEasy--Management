package com.renteasy.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.*;
import com.renteasy.activities.databinding.ActivityCustomerDetailBinding;
import com.renteasy.adapters.DamageAdapter;
import com.renteasy.adapters.MissingItemAdapter;
import com.renteasy.adapters.RentalAdapter;
import com.renteasy.models.*;
import com.renteasy.utils.CurrencyManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CustomerDetailActivity extends AppCompatActivity {

    private ActivityCustomerDetailBinding binding;
    private FirebaseFirestore             db;
    private String                        customerId;
    private Customer                      currentCustomer;

    private RentalAdapter      rentalAdapter;
    private MissingItemAdapter missingAdapter;
    private DamageAdapter      damageAdapter;

    private ListenerRegistration rentalsListener;
    private ListenerRegistration missingListener;
    private ListenerRegistration damageListener;

    private final Handler     ticker        = new Handler(Looper.getMainLooper());
    private List<Rental>      cachedRentals = new ArrayList<>();
    private List<MissingItem> cachedMissing = new ArrayList<>();
    private List<DamageItem>  cachedDamages = new ArrayList<>();

    private final Runnable tickRunnable = new Runnable() {
        @Override public void run() {
            refreshRentalTotals();
            ticker.postDelayed(this, 60_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        customerId           = getIntent().getStringExtra("customerId");
        String customerName  = getIntent().getStringExtra("customerName");
        String customerEmail = getIntent().getStringExtra("customerEmail");
        String customerPhone = getIntent().getStringExtra("customerPhone");

        currentCustomer = new Customer();
        currentCustomer.setId(customerId);
        currentCustomer.setName(customerName);
        currentCustomer.setEmail(customerEmail);
        currentCustomer.setPhone(customerPhone);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(customerName);

        db = FirebaseFirestore.getInstance();

        setupRentals();
        setupMissingItems();
        setupDamages();

        binding.btnAddRental.setOnClickListener(v -> startActivity(
                new Intent(this, AddRentalActivity.class).putExtra("customerId", customerId)));
        binding.btnAddMissing.setOnClickListener(v -> startActivity(
                new Intent(this, AddMissingItemActivity.class).putExtra("customerId", customerId)));
        binding.btnAddDamage.setOnClickListener(v -> startActivity(
                new Intent(this, AddDamageActivity.class).putExtra("customerId", customerId)));

        // Generate Bill → open dedicated BillActivity
        binding.btnGenerateBill.setOnClickListener(v -> openBillActivity());
    }

    @Override protected void onResume() { super.onResume(); ticker.post(tickRunnable); }
    @Override protected void onPause()  { super.onPause();  ticker.removeCallbacks(tickRunnable); }

    // ── Rentals ──────────────────────────────────────────────────────────

    private void setupRentals() {
        rentalAdapter = new RentalAdapter(new ArrayList<>(), this::markReturned);
        binding.recyclerRentals.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerRentals.setAdapter(rentalAdapter);

        rentalsListener = db.collection("customers")
                .document(customerId).collection("rentals")
                .orderBy("startDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    cachedRentals = snap.toObjects(Rental.class);
                    rentalAdapter.updateList(cachedRentals);
                    refreshRentalTotals();
                    binding.tvNoRentals.setVisibility(cachedRentals.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void refreshRentalTotals() {
        String sym = CurrencyManager.getSymbol(this);
        double active = 0, all = 0;
        for (Rental r : cachedRentals) {
            double cost = r.getTotalCost(this);
            all += cost;
            if (r.isActive()) active += cost;
        }
        binding.tvRentalTotal.setText(
                String.format("Active: %s%.2f  |  All-time: %s%.2f", sym, active, sym, all));
        rentalAdapter.notifyDataSetChanged();
        refreshPaymentSummary();
    }

    private void refreshPaymentSummary() {
        String sym = CurrencyManager.getSymbol(this);
        double totalDue = 0, totalAdv = 0, totalSec = 0, damageExtra = 0, missingUnpaid = 0;
        for (Rental r : cachedRentals)      { totalDue += r.getTotalCost(this); totalAdv += r.getAdvancePayment(); totalSec += r.getSecurityAmount(); }
        for (DamageItem d : cachedDamages)  if (!d.isDeductFromSecurity()) damageExtra += d.getAmount();
        for (MissingItem m : cachedMissing) if (!m.isPaid()) missingUnpaid += m.getPrice();
        double net = totalDue - totalAdv - totalSec + damageExtra + missingUnpaid;
        binding.tvSummaryDue.setText(sym + String.format("%.2f", totalDue));
        binding.tvSummaryAdvance.setText(sym + String.format("%.2f", totalAdv));
        binding.tvSummarySecurity.setText(sym + String.format("%.2f", totalSec));
        binding.tvSummaryNet.setText(sym + String.format("%.2f", Math.max(0, net)));
    }

    private void markReturned(Rental rental) {
        String sym = CurrencyManager.getSymbol(this);
        new AlertDialog.Builder(this)
                .setTitle("Mark as Returned?")
                .setMessage("\"" + rental.getItemName() + "\"\n"
                        + "Duration: " + rental.getDurationLabel()
                        + "\nTotal: " + sym + String.format("%.2f", rental.getTotalCost(this)))
                .setPositiveButton("Mark Returned", (d, w) ->
                        db.collection("customers").document(customerId)
                                .collection("rentals").document(rental.getId())
                                .update("status", "returned", "endDate", new Date())
                                .addOnFailureListener(ex -> Toast.makeText(this,
                                        "Failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null).show();
    }

    // ── Missing Items ────────────────────────────────────────────────────

    private void setupMissingItems() {
        missingAdapter = new MissingItemAdapter(new ArrayList<>(), this::markMissingPaid);
        binding.recyclerMissing.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMissing.setAdapter(missingAdapter);

        missingListener = db.collection("customers")
                .document(customerId).collection("missingItems")
                .orderBy("reportedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    cachedMissing = snap.toObjects(MissingItem.class);
                    missingAdapter.updateList(cachedMissing);
                    String sym = CurrencyManager.getSymbol(this);
                    double total = 0, unpaid = 0;
                    for (MissingItem m : cachedMissing) {
                        total += m.getPrice();
                        if (!m.isPaid()) unpaid += m.getPrice();
                    }
                    binding.tvMissingTotal.setText(
                            String.format("Total: %s%.2f  |  Unpaid: %s%.2f", sym, total, sym, unpaid));
                    binding.tvNoMissing.setVisibility(cachedMissing.isEmpty() ? View.VISIBLE : View.GONE);
                    refreshPaymentSummary();
                });
    }

    private void markMissingPaid(MissingItem item) {
        if (item.isPaid()) return;
        String sym = CurrencyManager.getSymbol(this);
        new AlertDialog.Builder(this)
                .setTitle("Mark as Paid?")
                .setMessage("\"" + item.getItemName() + "\" — " + sym + String.format("%.2f", item.getPrice()))
                .setPositiveButton("Mark Paid", (d, w) ->
                        db.collection("customers").document(customerId)
                                .collection("missingItems").document(item.getId())
                                .update("paid", true)
                                .addOnFailureListener(ex -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null).show();
    }

    // ── Damages ──────────────────────────────────────────────────────────

    private void setupDamages() {
        damageAdapter = new DamageAdapter(new ArrayList<>(), this::markDamagePaid);
        binding.recyclerDamages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerDamages.setAdapter(damageAdapter);

        damageListener = db.collection("customers")
                .document(customerId).collection("damages")
                .orderBy("reportedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    cachedDamages = snap.toObjects(DamageItem.class);
                    damageAdapter.updateList(cachedDamages);
                    String sym = CurrencyManager.getSymbol(this);
                    double total = 0, extra = 0;
                    for (DamageItem d : cachedDamages) {
                        total += d.getAmount();
                        if (!d.isDeductFromSecurity()) extra += d.getAmount();
                    }
                    binding.tvDamageTotal.setText(
                            String.format("Total: %s%.2f  |  Extra charges: %s%.2f", sym, total, sym, extra));
                    binding.tvNoDamages.setVisibility(cachedDamages.isEmpty() ? View.VISIBLE : View.GONE);
                    refreshPaymentSummary();
                });
    }

    private void markDamagePaid(DamageItem item) {
        if (item.isPaid()) return;
        String sym = CurrencyManager.getSymbol(this);
        new AlertDialog.Builder(this)
                .setTitle("Mark Damage as Paid?")
                .setMessage("\"" + item.getDescription() + "\" — " + sym + String.format("%.2f", item.getAmount()))
                .setPositiveButton("Mark Paid", (d, w) ->
                        db.collection("customers").document(customerId)
                                .collection("damages").document(item.getId())
                                .update("paid", true)
                                .addOnFailureListener(ex -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null).show();
    }

    // ── Bill ─────────────────────────────────────────────────────────────

    private void openBillActivity() {
        Intent i = new Intent(this, BillActivity.class);
        i.putExtra("customerId",    customerId);
        i.putExtra("customerName",  currentCustomer.getName());
        i.putExtra("customerEmail", currentCustomer.getEmail());
        i.putExtra("customerPhone", currentCustomer.getPhone());
        startActivity(i);
    }

    // ── Menu ─────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_customer_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_bill_history) {
            Intent i = new Intent(this, BillHistoryActivity.class);
            i.putExtra("customerId",   customerId);
            i.putExtra("customerName", currentCustomer.getName());
            startActivity(i);
            return true;
        }
        if (id == R.id.action_delete_customer) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Customer?")
                    .setMessage("This will permanently delete all records. Cannot be undone.")
                    .setPositiveButton("Delete", (d, w) ->
                            db.collection("customers").document(customerId).delete()
                                    .addOnSuccessListener(v -> finish())
                                    .addOnFailureListener(ex -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()))
                    .setNegativeButton("Cancel", null).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ticker.removeCallbacks(tickRunnable);
        if (rentalsListener != null) rentalsListener.remove();
        if (missingListener  != null) missingListener.remove();
        if (damageListener   != null) damageListener.remove();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
