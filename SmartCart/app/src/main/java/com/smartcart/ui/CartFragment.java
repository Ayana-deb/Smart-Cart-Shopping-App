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
import com.smartcart.model.CartItem;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.ArrayList;
import java.util.List;

/** Cart with quantity updates, removal, stock validation and automatic totals. */
public class CartFragment extends Fragment {
    private DBHelper db;
    private long uid;
    private SimpleAdapter<CartItem> adapter;
    private TextView tvEmpty, tvTotal;
    private View cardSummary;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_cart, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        db = DBHelper.get(requireContext());
        uid = Session.of(requireContext()).userId();
        tvEmpty = v.findViewById(R.id.tvEmpty);
        tvEmpty.setText("Your cart is empty \uD83D\uDED2\nBrowse products and add something you like.");
        tvTotal = v.findViewById(R.id.tvTotal);
        cardSummary = v.findViewById(R.id.cardSummary);
        RecyclerView rv = v.findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SimpleAdapter<>(new ArrayList<>(), R.layout.item_cart, this::bind);
        rv.setAdapter(adapter);
        v.findViewById(R.id.btnCheckout).setOnClickListener(x -> startActivity(new Intent(getContext(), CheckoutActivity.class)));
    }

    @Override public void onResume() { super.onResume(); load(); }

    private void load() {
        List<CartItem> items = db.getCart(uid);
        adapter.setItems(items);
        double total = 0;
        for (CartItem ci : items) total += ci.product.price * ci.quantity;
        tvTotal.setText(Util.money(total));
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        cardSummary.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).refreshBadge();
    }

    private void bind(View v, CartItem ci) {
        Util.emoji((TextView) v.findViewById(R.id.tvEmoji), ci.product.emoji, ci.product.category);
        ((TextView) v.findViewById(R.id.tvName)).setText(ci.product.name);
        ((TextView) v.findViewById(R.id.tvPrice)).setText(Util.money(ci.product.price) + " each");
        ((TextView) v.findViewById(R.id.tvQty)).setText(String.valueOf(ci.quantity));
        ((TextView) v.findViewById(R.id.tvLineTotal)).setText(Util.money(ci.product.price * ci.quantity));
        v.findViewById(R.id.btnMinus).setOnClickListener(x -> {
            if (ci.quantity > 1) { db.setCartQty(ci.id, ci.quantity - 1); load(); }
        });
        v.findViewById(R.id.btnPlus).setOnClickListener(x -> {
            if (ci.quantity + 1 > ci.product.stock) Util.toast(requireContext(), "Only " + ci.product.stock + " in stock");
            else { db.setCartQty(ci.id, ci.quantity + 1); load(); }
        });
        v.findViewById(R.id.btnRemove).setOnClickListener(x -> { db.removeCart(ci.id); load(); });
    }
}
