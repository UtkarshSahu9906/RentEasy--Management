package com.renteasy.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.renteasy.activities.databinding.ItemCustomerBinding;
import com.renteasy.models.Customer;

import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {

    public interface OnCustomerClick { void onClick(Customer customer); }

    // Distinct colors for avatar backgrounds
    private static final int[] AVATAR_COLORS = {
            0xFF1565C0, // deep blue
            0xFF2E7D32, // deep green
            0xFF6A1B9A, // deep purple
            0xFFAD1457, // deep pink
            0xFF00695C, // teal
            0xFFE65100, // deep orange
            0xFF37474F, // blue grey
            0xFF4527A0, // deep indigo
    };

    private List<Customer>  list;
    private OnCustomerClick clickListener;

    public CustomerAdapter(List<Customer> list, OnCustomerClick listener) {
        this.list          = list;
        this.clickListener = listener;
    }

    public void updateList(List<Customer> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCustomerBinding b = ItemCustomerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position), position);
    }

    @Override
    public int getItemCount() { return list.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        ItemCustomerBinding b;
        ViewHolder(ItemCustomerBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
        void bind(Customer c, int position) {
            b.tvInitials.setText(c.getInitials());

            // Pick a consistent color per position
            int color = AVATAR_COLORS[position % AVATAR_COLORS.length];
            b.tvInitials.getBackground().setTint(color);

            b.tvName.setText(c.getName());
            b.tvEmail.setText(c.getEmail() != null ? c.getEmail() : "");
            b.tvPhone.setText(c.getPhone() != null ? c.getPhone() : "");

            b.getRoot().setOnClickListener(v -> clickListener.onClick(c));
        }
    }
}
