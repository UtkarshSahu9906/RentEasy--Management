package com.renteasy.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.renteasy.activities.databinding.ActivityMainBinding;
import com.renteasy.models.MissingItem;
import com.renteasy.models.Rental;
import com.renteasy.utils.CurrencyManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding  binding;
    private FirebaseAuth         mAuth;
    private FirebaseFirestore    db;
    private String               uid;

    private ListenerRegistration customersListener;
    private ListenerRegistration rentalsListener;
    private ListenerRegistration missingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { goToLogin(); return; }
        uid = user.getUid();

        String firstName = user.getDisplayName() != null
                ? user.getDisplayName().split(" ")[0] : "there";
        String hour = new SimpleDateFormat("HH", Locale.getDefault()).format(new Date());
        int h = Integer.parseInt(hour);
        String greeting = h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
        binding.tvGreeting.setText(greeting);
        binding.tvWelcome.setText(firstName);

        // Currency badge
        refreshCurrencyBadge();
        binding.cardCurrency.setOnClickListener(v -> showCurrencyPicker());

        // Quick actions
        binding.btnQuickAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddCustomerActivity.class)));
        binding.cardCustomers.setOnClickListener(v ->
                startActivity(new Intent(this, CustomerListActivity.class)));
        binding.btnGoCustomers.setOnClickListener(v ->
                startActivity(new Intent(this, CustomerListActivity.class)));

        loadDashboard();
    }

    private void refreshCurrencyBadge() {
        binding.tvCurrencySymbol.setText(CurrencyManager.getSymbol(this));
        binding.tvCurrencyCode.setText(CurrencyManager.getCode(this));
    }

    private void showCurrencyPicker() {
        String[][] currencies = CurrencyManager.CURRENCIES;
        String[] labels = new String[currencies.length];
        for (int i = 0; i < currencies.length; i++) {
            labels[i] = currencies[i][0] + "  " + currencies[i][1] + "  —  " + currencies[i][2];
        }
        new AlertDialog.Builder(this)
                .setTitle("Select Currency")
                .setItems(labels, (d, which) -> {
                    CurrencyManager.save(this, currencies[which][0], currencies[which][1]);
                    refreshCurrencyBadge();
                    loadDashboard(); // refresh amounts
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadDashboard() {
        String sym = CurrencyManager.getSymbol(this);

        customersListener = db.collection("customers")
                .whereEqualTo("ownerId", uid)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null)
                        binding.tvCustomerCount.setText(String.valueOf(snap.size()));
                });

        rentalsListener = db.collectionGroup("rentals")
                .whereEqualTo("status", "active")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    List<Rental> rentals = snap.toObjects(Rental.class);
                    double revenue = 0, security = 0, advance = 0;
                    for (Rental r : rentals) {
                        revenue  += r.getTotalCost();
                        security += r.getSecurityAmount();
                        advance  += r.getAdvancePayment();
                    }
                    binding.tvActiveRentals.setText(String.valueOf(rentals.size()));
                    binding.tvRevenue.setText(sym + String.format("%.2f", revenue));
                    binding.tvSecurityHeld.setText(sym + String.format("%.2f", security));
                    binding.tvAdvancePaid.setText(sym + String.format("%.2f", advance));

                    // Show advance card only if > 0
                    binding.cardAdvance.setVisibility(advance > 0 ? View.VISIBLE : View.GONE);
                });

        missingListener = db.collectionGroup("missingItems")
                .whereEqualTo("paid", false)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    List<MissingItem> items = snap.toObjects(MissingItem.class);
                    double total = 0;
                    for (MissingItem m : items) total += m.getPrice();
                    binding.tvUnpaidCount.setText(String.valueOf(items.size()));
                    binding.tvUnpaidAmount.setText(sym + String.format("%.2f", total));
                    binding.cardUnpaid.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (customersListener != null) customersListener.remove();
        if (rentalsListener   != null) rentalsListener.remove();
        if (missingListener   != null) missingListener.remove();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            signOut(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        mAuth.signOut();
        GoogleSignInClient gsc = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);
        gsc.signOut().addOnCompleteListener(t -> goToLogin());
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
