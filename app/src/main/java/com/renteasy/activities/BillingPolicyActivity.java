package com.renteasy.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.renteasy.activities.databinding.ActivityBillingPolicyBinding;
import com.renteasy.utils.BillingPolicy;

public class BillingPolicyActivity extends AppCompatActivity {

    private ActivityBillingPolicyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillingPolicyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Billing Policy");

        loadCurrentSettings();

        binding.btnSave.setOnClickListener(v -> saveSettings());

        // Show/hide grace hours input based on selection
        binding.rgDaily.setOnCheckedChangeListener((group, id) -> {
            binding.layoutGrace.setVisibility(
                    id == binding.rbDailyGrace.getId()
                    ? android.view.View.VISIBLE
                    : android.view.View.GONE);
        });
    }

    private void loadCurrentSettings() {
        // Daily policy
        BillingPolicy.OveragePolicy daily = BillingPolicy.getDailyPolicy(this);
        switch (daily) {
            case ROUND_UP_DAY:
                binding.rbDailyRound.setChecked(true);
                binding.layoutGrace.setVisibility(android.view.View.GONE);
                break;
            case GRACE_PERIOD:
                binding.rbDailyGrace.setChecked(true);
                binding.layoutGrace.setVisibility(android.view.View.VISIBLE);
                binding.etGraceHours.setText(String.valueOf(BillingPolicy.getGraceHours(this)));
                break;
            default:
                binding.rbDailyExact.setChecked(true);
                binding.layoutGrace.setVisibility(android.view.View.GONE);
                break;
        }

        // Monthly policy
        BillingPolicy.OveragePolicy monthly = BillingPolicy.getMonthlyPolicy(this);
        if (monthly == BillingPolicy.OveragePolicy.ROUND_UP_DAY) {
            binding.rbMonthlyRound.setChecked(true);
        } else {
            binding.rbMonthlyExact.setChecked(true);
        }
    }

    private void saveSettings() {
        // Daily
        int dailyId = binding.rgDaily.getCheckedRadioButtonId();
        BillingPolicy.OveragePolicy dailyPol;
        if      (dailyId == binding.rbDailyRound.getId()) dailyPol = BillingPolicy.OveragePolicy.ROUND_UP_DAY;
        else if (dailyId == binding.rbDailyGrace.getId()) dailyPol = BillingPolicy.OveragePolicy.GRACE_PERIOD;
        else                                               dailyPol = BillingPolicy.OveragePolicy.EXACT_HOURS;
        BillingPolicy.saveDailyPolicy(this, dailyPol);

        // Grace hours
        if (dailyPol == BillingPolicy.OveragePolicy.GRACE_PERIOD) {
            String g = binding.etGraceHours.getText().toString().trim();
            if (TextUtils.isEmpty(g)) { binding.etGraceHours.setError("Required"); return; }
            int grace = Integer.parseInt(g);
            if (grace < 1 || grace > 23) { binding.etGraceHours.setError("Enter 1–23 hours"); return; }
            BillingPolicy.saveGraceHours(this, grace);
        }

        // Monthly
        int monthlyId = binding.rgMonthly.getCheckedRadioButtonId();
        BillingPolicy.OveragePolicy monthlyPol = (monthlyId == binding.rbMonthlyRound.getId())
                ? BillingPolicy.OveragePolicy.ROUND_UP_DAY
                : BillingPolicy.OveragePolicy.EXACT_HOURS;
        BillingPolicy.saveMonthlyPolicy(this, monthlyPol);

        Toast.makeText(this, "Billing policy saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
