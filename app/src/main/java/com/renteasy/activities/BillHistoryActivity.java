package com.renteasy.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.*;
import com.renteasy.activities.databinding.ActivityBillHistoryBinding;
import com.renteasy.adapters.BillHistoryAdapter;
import com.renteasy.models.Bill;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows all saved bills for a single customer, newest first.
 * Accessible from CustomerDetailActivity toolbar menu.
 */
public class BillHistoryActivity extends AppCompatActivity {

    private ActivityBillHistoryBinding binding;
    private FirebaseFirestore          db;
    private String                     customerId;
    private BillHistoryAdapter         adapter;
    private ListenerRegistration       listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        customerId = getIntent().getStringExtra("customerId");
        String name = getIntent().getStringExtra("customerName");
        getSupportActionBar().setTitle(name + " — Bill History");

        db = FirebaseFirestore.getInstance();

        adapter = new BillHistoryAdapter(new ArrayList<>());
        binding.recyclerBills.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerBills.setAdapter(adapter);

        listener = db.collection("customers").document(customerId)
                .collection("bills")
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    List<Bill> bills = snap.toObjects(Bill.class);
                    adapter.updateList(bills);
                    binding.tvEmpty.setVisibility(bills.isEmpty() ? View.VISIBLE : View.GONE);
                    updateStats(bills);
                });
    }

    private void updateStats(List<Bill> bills) {
        int total = bills.size();
        int pending = 0;
        double maxBill = 0;
        double totalRevenue = 0;
        for (Bill b : bills) {
            if (Bill.STATUS_PENDING.equals(b.getStatus())) pending++;
            if (b.getGrandTotal() > maxBill) maxBill = b.getGrandTotal();
            totalRevenue += b.getGrandTotal();
        }
        String sym = total > 0 ? bills.get(0).getCurrencySymbol() : "";
        binding.tvStatTotal.setText(String.valueOf(total));
        binding.tvStatPending.setText(String.valueOf(pending));
        binding.tvStatRevenue.setText(sym + String.format("%.2f", totalRevenue));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
