package com.renteasy.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.renteasy.activities.databinding.ActivityAddMissingItemBinding;
import com.renteasy.models.MissingItem;

public class AddMissingItemActivity extends AppCompatActivity {

    private ActivityAddMissingItemBinding binding;
    private FirebaseFirestore             db;
    private String                        customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddMissingItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add Missing Item");

        customerId = getIntent().getStringExtra("customerId");
        db = FirebaseFirestore.getInstance();

        binding.btnSave.setOnClickListener(v -> saveMissingItem());
    }

    private void saveMissingItem() {
        String itemName = binding.etItemName.getText().toString().trim();
        String priceStr = binding.etPrice.getText().toString().trim();
        String notes    = binding.etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(itemName)) {
            binding.etItemName.setError("Item name is required");
            binding.etItemName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            binding.etPrice.setError("Price is required");
            binding.etPrice.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            binding.etPrice.setError("Enter a valid price");
            return;
        }
        if (price <= 0) {
            binding.etPrice.setError("Price must be greater than 0");
            return;
        }

        MissingItem item = new MissingItem(customerId, itemName, price);
        if (!notes.isEmpty()) item.setNotes(notes);

        binding.btnSave.setEnabled(false);
        db.collection("customers").document(customerId)
                .collection("missingItems").add(item)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Missing item recorded!", Toast.LENGTH_SHORT).show();
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
