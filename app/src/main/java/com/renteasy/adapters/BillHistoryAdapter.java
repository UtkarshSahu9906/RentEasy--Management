package com.renteasy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.renteasy.activities.BillActivity;
import com.renteasy.models.Bill;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BillHistoryAdapter extends RecyclerView.Adapter<BillHistoryAdapter.ViewHolder> {

    private List<Bill> list;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault());

    public BillHistoryAdapter(List<Bill> list) { this.list = list; }

    public void updateList(List<Bill> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(com.renteasy.activities.R.layout.item_bill_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Bill b   = list.get(pos);
        String sym = b.getCurrencySymbol() != null ? b.getCurrencySymbol() : "";

        h.tvDate.setText(b.getGeneratedAt() != null ? sdf.format(b.getGeneratedAt()) : "—");
        h.tvAmount.setText(sym + String.format("%.2f", b.getGrandTotal()));
        h.tvItems.setText(b.getTotalItems() + " item(s)");

        // Status badge
        switch (b.getStatus() != null ? b.getStatus() : "") {
            case Bill.STATUS_PENDING:
                h.tvStatus.setText("Pending");
                h.tvStatus.setBackgroundColor(0xFFFFF8E1);
                h.tvStatus.setTextColor(0xFFE65100);
                break;
            case Bill.STATUS_FINAL:
                h.tvStatus.setText("Final");
                h.tvStatus.setBackgroundColor(0xFFE8F5E9);
                h.tvStatus.setTextColor(0xFF2E7D32);
                break;
            default:
                h.tvStatus.setText("Draft");
                h.tvStatus.setBackgroundColor(0xFFE8EAF6);
                h.tvStatus.setTextColor(0xFF3949AB);
        }

        // Policy note
        if (BillActivity.POLICY_MARK_PENDING.equals(b.getActiveItemPolicy())) {
            h.tvPolicyNote.setVisibility(View.VISIBLE);
            h.tvPolicyNote.setText("Active items excluded (pending)");
        } else {
            h.tvPolicyNote.setVisibility(View.GONE);
        }

        // Breakdown
        h.tvBreakdown.setText(
                "Rentals: " + sym + String.format("%.2f", b.getRentalGross())
                + "  Adv: -" + sym + String.format("%.2f", b.getTotalAdvance())
                + "  Sec: -" + sym + String.format("%.2f", b.getTotalSecurity())
                + (b.getDamageCharges()  > 0 ? "  Dmg: +" + sym + String.format("%.2f", b.getDamageCharges())  : "")
                + (b.getMissingCharges() > 0 ? "  Miss: +" + sym + String.format("%.2f", b.getMissingCharges()) : "")
        );
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvAmount, tvStatus, tvItems, tvBreakdown, tvPolicyNote;
        ViewHolder(View v) {
            super(v);
            tvDate       = v.findViewById(com.renteasy.activities.R.id.tvBillDate);
            tvAmount     = v.findViewById(com.renteasy.activities.R.id.tvBillAmount);
            tvStatus     = v.findViewById(com.renteasy.activities.R.id.tvBillStatus);
            tvItems      = v.findViewById(com.renteasy.activities.R.id.tvBillItems);
            tvBreakdown  = v.findViewById(com.renteasy.activities.R.id.tvBillBreakdown);
            tvPolicyNote = v.findViewById(com.renteasy.activities.R.id.tvBillPolicyNote);
        }
    }
}
