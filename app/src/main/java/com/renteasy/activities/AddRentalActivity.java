package com.renteasy.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.renteasy.activities.databinding.ActivityAddRentalBinding;
import com.renteasy.models.Rental;
import com.renteasy.utils.CurrencyManager;

public class AddRentalActivity extends AppCompatActivity {

    private ActivityAddRentalBinding binding;
    private FirebaseFirestore        db;
    private String                   customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddRentalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add Rental");

        customerId = getIntent().getStringExtra("customerId");
        db = FirebaseFirestore.getInstance();

        // Show current currency symbol in hints
        String sym = CurrencyManager.getSymbol(this);
        binding.tilRate.setHint("Monthly Rate (" + sym + "/month)");
        binding.tilAdvance.setHint("Advance Payment (" + sym + ")");
        binding.tilSecurity.setHint("Security / Deposit (" + sym + ")");

        // Default: Monthly selected
        binding.btnMonthly.setChecked(true);
        updateRateHint("MONTHLY");

        binding.toggleRateType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if      (checkedId == binding.btnMonthly.getId()) updateRateHint("MONTHLY");
            else if (checkedId == binding.btnDaily.getId())   updateRateHint("DAILY");
            else if (checkedId == binding.btnHourly.getId())  updateRateHint("HOURLY");
        });

        binding.btnSave.setOnClickListener(v -> saveRental());
    }

    private void updateRateHint(String type) {
        String sym = CurrencyManager.getSymbol(this);
        switch (type) {
            case "MONTHLY":
                binding.tilRate.setHint("Monthly Rate (" + sym + "/month)");
                binding.tvRateNote.setText("Months first, then leftover days, then leftover hours");
                break;
            case "DAILY":
                binding.tilRate.setHint("Daily Rate (" + sym + "/day)");
                binding.tvRateNote.setText("Days first, then leftover hours");
                break;
            case "HOURLY":
                binding.tilRate.setHint("Hourly Rate (" + sym + "/hour)");
                binding.tvRateNote.setText("Total hours, rounded up to next full hour");
                break;
        }
    }

    private String getSelectedType() {
        int id = binding.toggleRateType.getCheckedButtonId();
        if (id == binding.btnMonthly.getId()) return "MONTHLY";
        if (id == binding.btnHourly.getId())  return "HOURLY";
        return "DAILY";
    }

    private double parseOrZero(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private void saveRental() {
        String itemName   = binding.etItemName.getText().toString().trim();
        String rateStr    = binding.etRate.getText().toString().trim();
        String advanceStr = binding.etAdvance.getText().toString().trim();
        String secureStr  = binding.etSecurity.getText().toString().trim();
        String notes      = binding.etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(itemName)) {
            binding.etItemName.setError("Item name is required");
            binding.etItemName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(rateStr)) {
            binding.etRate.setError("Rate is required");
            binding.etRate.requestFocus();
            return;
        }
        double rate = parseOrZero(rateStr);
        if (rate <= 0) {
            binding.etRate.setError("Rate must be greater than 0");
            return;
        }

        double advance  = parseOrZero(advanceStr);
        double security = parseOrZero(secureStr);
        String type     = getSelectedType();

        Rental rental;
        if ("MONTHLY".equals(type)) {
            rental = Rental.monthly(customerId, itemName, rate, advance, security, notes);
        } else if ("HOURLY".equals(type)) {
            rental = Rental.hourly(customerId, itemName, rate, advance, security, notes);
        } else {
            rental = Rental.daily(customerId, itemName, rate, advance, security, notes);
        }

        binding.btnSave.setEnabled(false);
        db.collection("customers").document(customerId)
                .collection("rentals").add(rental)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Rental added!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
