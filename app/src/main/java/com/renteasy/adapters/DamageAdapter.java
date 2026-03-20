package com.renteasy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.renteasy.models.DamageItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DamageAdapter extends RecyclerView.Adapter<DamageAdapter.ViewHolder> {

    public interface OnMarkPaid { void onMark(DamageItem item); }

    private List<DamageItem>    list;
    private OnMarkPaid          listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public DamageAdapter(List<DamageItem> list, OnMarkPaid listener) {
        this.list     = list;
        this.listener = listener;
    }

    public void updateList(List<DamageItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(com.renteasy.activities.R.layout.item_damage, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        DamageItem d = list.get(pos);

        h.tvDescription.setText(d.getDescription());
        h.tvAmount.setText(String.format("\u20b1%.2f", d.getAmount()));
        h.tvDate.setText(d.getReportedAt() != null ? sdf.format(d.getReportedAt()) : "");

        if (d.getRentalItemName() != null && !d.getRentalItemName().isEmpty()) {
            h.tvRentalItem.setVisibility(View.VISIBLE);
            h.tvRentalItem.setText("Item: " + d.getRentalItemName());
        } else {
            h.tvRentalItem.setVisibility(View.GONE);
        }

        h.tvBillingType.setText(d.isDeductFromSecurity() ? "From security deposit" : "Extra charge");
        h.tvBillingType.setTextColor(d.isDeductFromSecurity() ? 0xFF4527A0 : 0xFFB71C1C);

        if (d.isPaid() || d.isDeductFromSecurity()) {
            h.btnMarkPaid.setVisibility(View.GONE);
        } else {
            h.btnMarkPaid.setVisibility(View.VISIBLE);
            h.btnMarkPaid.setOnClickListener(v -> listener.onMark(d));
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount, tvDate, tvRentalItem, tvBillingType;
        Button   btnMarkPaid;
        ViewHolder(View v) {
            super(v);
            tvDescription = v.findViewById(com.renteasy.activities.R.id.tvDamageDescription);
            tvAmount      = v.findViewById(com.renteasy.activities.R.id.tvDamageAmount);
            tvDate        = v.findViewById(com.renteasy.activities.R.id.tvDamageDate);
            tvRentalItem  = v.findViewById(com.renteasy.activities.R.id.tvDamageRentalItem);
            tvBillingType = v.findViewById(com.renteasy.activities.R.id.tvDamageBillingType);
            btnMarkPaid   = v.findViewById(com.renteasy.activities.R.id.btnDamageMarkPaid);
        }
    }
}
