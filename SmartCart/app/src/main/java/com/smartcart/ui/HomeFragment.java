package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.model.Product;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Product browsing with live search and category chips. */
public class HomeFragment extends Fragment {
    private DBHelper db;
    private long uid;
    private String category = "";
    private String query = "";
    private SimpleAdapter<Product> adapter;
    private ChipGroup chips;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_home, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        db = DBHelper.get(requireContext());
        Session s = Session.of(requireContext());
        uid = s.userId();
        ((TextView) v.findViewById(R.id.tvGreeting)).setText("Hi, " + s.name().split(" ")[0] + " \uD83D\uDC4B");
        tvEmpty = v.findViewById(R.id.tvEmpty);

        RecyclerView rv = v.findViewById(R.id.rvProducts);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new SimpleAdapter<>(new ArrayList<>(), R.layout.item_product, this::bindProduct);
        rv.setAdapter(adapter);

        ((EditText) v.findViewById(R.id.etSearch)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { query = s.toString(); load(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        chips = v.findViewById(R.id.chipGroup);
        List<String> cats = new ArrayList<>();
        cats.add("All");
        cats.addAll(db.getCategories());
        for (String name : cats) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(name);
            chip.setCheckable(true);
            chip.setChecked(name.equals("All"));
            chip.setOnClickListener(x -> { category = name.equals("All") ? "" : name; load(); });
            chips.addView(chip);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity m = (MainActivity) getActivity();
            if (m.pendingCategory != null) { // opened from the Categories tab
                category = m.pendingCategory;
                m.pendingCategory = null;
                for (int i = 0; i < chips.getChildCount(); i++) {
                    Chip c = (Chip) chips.getChildAt(i);
                    if (c.getText().toString().equals(category)) c.setChecked(true);
                }
            }
            m.refreshBadge();
        }
        load();
    }

    private void load() {
        List<Product> list = db.getProducts(category, query);
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bindProduct(View v, Product p) {
        Util.emoji((TextView) v.findViewById(R.id.tvEmoji), p.emoji, p.category);
        ((TextView) v.findViewById(R.id.tvName)).setText(p.name);
        ((TextView) v.findViewById(R.id.tvPrice)).setText(Util.money(p.price));
        ((TextView) v.findViewById(R.id.tvRating)).setText(String.format(Locale.US, "\u2605 %.1f", p.rating));
        ((TextView) v.findViewById(R.id.tvStock)).setText(p.stock <= 0 ? "Out of stock" : p.stock <= 5 ? "Only " + p.stock + " left" : "In stock");
        ImageButton wish = v.findViewById(R.id.btnWish);
        wish.setImageResource(db.isWishlisted(uid, p.id) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        wish.setOnClickListener(x -> {
            boolean now = db.toggleWishlist(uid, p.id);
            wish.setImageResource(now ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            Util.toast(requireContext(), now ? "Added to wishlist" : "Removed from wishlist");
        });
        v.findViewById(R.id.btnAdd).setOnClickListener(x -> {
            String err = db.addToCart(uid, p.id, 1);
            Util.toast(requireContext(), err == null ? p.name + " added to cart" : err);
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).refreshBadge();
        });
        v.setOnClickListener(x -> {
            Intent i = new Intent(getContext(), ProductDetailActivity.class);
            i.putExtra("pid", p.id);
            startActivity(i);
        });
    }
}
