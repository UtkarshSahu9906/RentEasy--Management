package com.renteasy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.renteasy.activities.databinding.ItemRentalBinding;
import com.renteasy.models.Rental;
import com.renteasy.utils.CurrencyManager;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RentalAdapter extends RecyclerView.Adapter<RentalAdapter.ViewHolder> {

    public interface OnReturnClick { void onReturn(Rental rental); }

    private List<Rental>  list;
    private OnReturnClick returnListener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault());

    public RentalAdapter(List<Rental> list, OnReturnClick listener) {
        this.list           = list;
        this.returnListener = listener;
    }

    public void updateList(List<Rental> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemRentalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Rental r   = list.get(pos);
        String sym = CurrencyManager.getSymbol(h.b.getRoot().getContext());

        h.b.tvItemName.setText(r.getItemName());
        h.b.tvRate.setText(sym + r.getRateLabel() + "  \u00b7  " + r.getDurationLabel());
        h.b.tvStartDate.setText("Started: " + (r.getStartDate() != null ? sdf.format(r.getStartDate()) : "\u2014"));
        h.b.tvTotal.setText(sym + String.format("%.2f", r.getTotalCost(h.b.getRoot().getContext())));

        boolean active = r.isActive();
        h.b.chipStatus.setText(active ? "Active" : "Returned");
        h.b.chipStatus.setChipBackgroundColorResource(active
                ? com.google.android.material.R.color.design_default_color_secondary
                : com.google.android.material.R.color.design_default_color_background);

        if (active) {
            h.b.btnReturn.setVisibility(View.VISIBLE);
            h.b.btnReturn.setOnClickListener(v -> returnListener.onReturn(r));
        } else {
            h.b.btnReturn.setVisibility(View.GONE);
            if (r.getEndDate() != null) {
                h.b.tvStartDate.setText(
                        "Started: " + sdf.format(r.getStartDate()) +
                        "\nReturned: " + sdf.format(r.getEndDate()));
            }
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemRentalBinding b;
        ViewHolder(ItemRentalBinding binding) { super(binding.getRoot()); this.b = binding; }
    }
}
