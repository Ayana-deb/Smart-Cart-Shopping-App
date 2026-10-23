package com.smartcart.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** One reusable RecyclerView adapter: give it a layout and a binder lambda. */
public class SimpleAdapter<T> extends RecyclerView.Adapter<SimpleAdapter.VH> {
    public interface Binder<T> { void bind(View v, T item); }

    private List<T> items;
    private final int layout;
    private final Binder<T> binder;

    public SimpleAdapter(List<T> items, int layout, Binder<T> binder) {
        this.items = items; this.layout = layout; this.binder = binder;
    }

    public void setItems(List<T> l) { items = l; notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int type) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(layout, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) { binder.bind(h.itemView, items.get(i)); }
    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }
}
