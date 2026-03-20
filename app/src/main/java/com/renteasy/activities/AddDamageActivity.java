package com.renteasy.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.renteasy.activities.databinding.ActivityAddDamageBinding;
import com.renteasy.models.DamageItem;
import com.renteasy.models.Rental;
import com.renteasy.utils.CurrencyManager;

import java.util.ArrayList;
import java.util.List;

public class AddDamageActivity extends AppCompatActivity {

    private ActivityAddDamageBinding binding;
    private FirebaseFirestore         db;
    private String                    customerId;
    private List<Rental>              rentals     = new ArrayList<>();
    private List<String>              rentalNames = new ArrayList<>();
    private boolean                   deductFromSecurity = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddDamageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Record Damage");

        customerId = getIntent().getStringExtra("customerId");
        db = FirebaseFirestore.getInstance();

        String sym = CurrencyManager.getSymbol(this);
        binding.tilAmount.setHint("Repair / Replacement Cost (" + sym + ")");

        // Load rentals for dropdown
        loadRentals();

        // Deduct toggle
        binding.btnDeductYes.setOnClickListener(v -> setDeductMode(true));
        binding.btnDeductNo.setOnClickListener(v ->  setDeductMode(false));
        setDeductMode(true);

        binding.btnSave.setOnClickListener(v -> saveDamage());
    }

    private void loadRentals() {
        db.collection("customers").document(customerId)
                .collection("rentals")
                .get()
                .addOnSuccessListener(snap -> {
                    rentals.clear();
                    rentalNames.clear();
                    rentalNames.add("(Not linked to a rental)");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Rental r = doc.toObject(Rental.class);
                        if (r != null) {
                            rentals.add(r);
                            rentalNames.add(r.getItemName() + (r.isActive() ? " · Active" : " · Returned"));
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, rentalNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerRental.setAdapter(adapter);
                });
    }

    private void setDeductMode(boolean deduct) {
        deductFromSecurity = deduct;
        binding.btnDeductYes.setSelected(deduct);
        binding.btnDeductNo.setSelected(!deduct);

        if (deduct) {
            binding.btnDeductYes.setBackgroundColor(0xFF1565C0);
            binding.btnDeductYes.setTextColor(0xFFFFFFFF);
            binding.btnDeductNo.setBackgroundColor(0x00000000);
            binding.btnDeductNo.setTextColor(0xFF666666);
            binding.tvSecurityNote.setVisibility(View.VISIBLE);
        } else {
            binding.btnDeductNo.setBackgroundColor(0xFF1565C0);
            binding.btnDeductNo.setTextColor(0xFFFFFFFF);
            binding.btnDeductYes.setBackgroundColor(0x00000000);
            binding.btnDeductYes.setTextColor(0xFF666666);
            binding.tvSecurityNote.setVisibility(View.GONE);
        }
    }

    private void saveDamage() {
        String description = binding.etDescription.getText().toString().trim();
        String amountStr   = binding.etAmount.getText().toString().trim();

        if (TextUtils.isEmpty(description)) {
            binding.etDescription.setError("Description is required");
            binding.etDescription.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(amountStr)) {
            binding.etAmount.setError("Amount is required");
            binding.etAmount.requestFocus();
            return;
        }

        double amount;
        try { amount = Double.parseDouble(amountStr); }
        catch (NumberFormatException e) { binding.etAmount.setError("Invalid amount"); return; }
        if (amount <= 0) { binding.etAmount.setError("Amount must be > 0"); return; }

        // Get selected rental
        int selectedPos = binding.spinnerRental.getSelectedItemPosition();
        String rentalId       = null;
        String rentalItemName = null;
        if (selectedPos > 0 && selectedPos - 1 < rentals.size()) {
            Rental r = rentals.get(selectedPos - 1);
            rentalId       = r.getId();
            rentalItemName = r.getItemName();
        }

        DamageItem damage = new DamageItem(
                customerId, rentalId, rentalItemName,
                description, amount, deductFromSecurity);

        binding.btnSave.setEnabled(false);
        db.collection("customers").document(customerId)
                .collection("damages").add(damage)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Damage recorded.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
