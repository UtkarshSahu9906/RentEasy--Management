package com.renteasy.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.renteasy.activities.databinding.ItemMissingBinding;
import com.renteasy.models.MissingItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MissingItemAdapter extends RecyclerView.Adapter<MissingItemAdapter.ViewHolder> {

    public interface OnMarkPaid { void onMark(MissingItem item); }

    private List<MissingItem>       list;
    private OnMarkPaid              markPaidListener;
    private final SimpleDateFormat  sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public MissingItemAdapter(List<MissingItem> list, OnMarkPaid listener) {
        this.list             = list;
        this.markPaidListener = listener;
    }

    public void updateList(List<MissingItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemMissingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        MissingItem m = list.get(pos);
        h.b.tvItemName.setText(m.getItemName());
        h.b.tvPrice.setText(String.format("₱%.2f", m.getPrice()));
        h.b.tvDate.setText(m.getReportedAt() != null
                ? "Reported: " + sdf.format(m.getReportedAt()) : "—");

        // Notes (show only if present)
        if (!TextUtils.isEmpty(m.getNotes())) {
            h.b.tvNotes.setVisibility(View.VISIBLE);
            h.b.tvNotes.setText(m.getNotes());
        } else {
            h.b.tvNotes.setVisibility(View.GONE);
        }

        // Status chip
        h.b.chipPaid.setText(m.isPaid() ? "Paid" : "Unpaid");
        h.b.chipPaid.setChipBackgroundColorResource(m.isPaid()
                ? com.google.android.material.R.color.design_default_color_secondary
                : com.google.android.material.R.color.design_default_color_error);

        // Mark paid button
        if (!m.isPaid()) {
            h.b.btnMarkPaid.setVisibility(View.VISIBLE);
            h.b.btnMarkPaid.setOnClickListener(v -> markPaidListener.onMark(m));
        } else {
            h.b.btnMarkPaid.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemMissingBinding b;
        ViewHolder(ItemMissingBinding binding) { super(binding.getRoot()); this.b = binding; }
    }
}
