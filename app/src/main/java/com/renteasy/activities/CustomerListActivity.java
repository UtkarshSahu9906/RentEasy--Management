package com.renteasy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.renteasy.activities.databinding.ActivityCustomerListBinding;
import com.renteasy.adapters.CustomerAdapter;
import com.renteasy.models.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerListActivity extends AppCompatActivity {

    private ActivityCustomerListBinding binding;
    private CustomerAdapter             adapter;
    private final List<Customer>        allCustomers = new ArrayList<>();
    private FirebaseFirestore           db;
    private ListenerRegistration        listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Customers");

        db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new CustomerAdapter(new ArrayList<>(), customer -> {
            Intent i = new Intent(this, CustomerDetailActivity.class);
            i.putExtra("customerId",    customer.getId());
            i.putExtra("customerName",  customer.getName());
            i.putExtra("customerEmail", customer.getEmail());   // needed for bill
            i.putExtra("customerPhone", customer.getPhone());   // needed for bill
            startActivity(i);
        });
        binding.recyclerCustomers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerCustomers.setAdapter(adapter);

        binding.fabAddCustomer.setOnClickListener(v ->
                startActivity(new Intent(this, AddCustomerActivity.class)));

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { filterList(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        listener = db.collection("customers")
                .whereEqualTo("ownerId", uid)
                .orderBy("name")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    allCustomers.clear();
                    allCustomers.addAll(snap.toObjects(Customer.class));
                    filterList(binding.etSearch.getText().toString());
                    updateCountLabel();
                });
    }

    private void filterList(String query) {
        List<Customer> filtered = new ArrayList<>();
        String q = query.toLowerCase().trim();
        for (Customer c : allCustomers) {
            boolean nameMatch  = c.getName()  != null && c.getName().toLowerCase().contains(q);
            boolean emailMatch = c.getEmail() != null && c.getEmail().toLowerCase().contains(q);
            boolean phoneMatch = c.getPhone() != null && c.getPhone().contains(q);
            if (nameMatch || emailMatch || phoneMatch) filtered.add(c);
        }
        adapter.updateList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateCountLabel() {
        int count = allCustomers.size();
        binding.tvCustomerCount.setText(count + (count == 1 ? " customer" : " customers"));
    }



    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
