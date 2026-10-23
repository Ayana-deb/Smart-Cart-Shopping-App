package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.model.Product;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.ArrayList;
import java.util.List;

/** Persistent per-user wishlist with remove and move-to-cart. */
public class WishlistFragment extends Fragment {
    private DBHelper db;
    private long uid;
    private SimpleAdapter<Product> adapter;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_wishlist, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        db = DBHelper.get(requireContext());
        uid = Session.of(requireContext()).userId();
        tvEmpty = v.findViewById(R.id.tvEmpty);
        tvEmpty.setText("Your wishlist is empty \u2764\uFE0F\nTap the heart on any product to save it here.");
        RecyclerView rv = v.findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SimpleAdapter<>(new ArrayList<>(), R.layout.item_wishlist, this::bind);
        rv.setAdapter(adapter);
    }

    @Override public void onResume() { super.onResume(); load(); }

    private void load() {
        List<Product> list = db.getWishlist(uid);
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bind(View v, Product p) {
        Util.emoji((TextView) v.findViewById(R.id.tvEmoji), p.emoji, p.category);
        ((TextView) v.findViewById(R.id.tvName)).setText(p.name);
        ((TextView) v.findViewById(R.id.tvPrice)).setText(Util.money(p.price));
        ((TextView) v.findViewById(R.id.tvStock)).setText(p.stock > 0 ? "In stock" : "Out of stock");
        v.findViewById(R.id.btnRemove).setOnClickListener(x -> { db.toggleWishlist(uid, p.id); load(); });
        v.findViewById(R.id.btnMove).setOnClickListener(x -> {
            String err = db.addToCart(uid, p.id, 1);
            if (err != null) { Util.toast(requireContext(), err); return; }
            db.toggleWishlist(uid, p.id);
            Util.toast(requireContext(), "Moved to cart");
            load();
        });
        v.setOnClickListener(x -> {
            Intent i = new Intent(getContext(), ProductDetailActivity.class);
            i.putExtra("pid", p.id);
            startActivity(i);
        });
    }
}
