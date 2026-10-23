package com.smartcart.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.model.Product;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {
    private int qty = 1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_product_detail);
        Util.toolbar(this, "Product details", true);
        DBHelper db = DBHelper.get(this);
        long uid = Session.of(this).userId();
        Product p = db.getProduct(getIntent().getLongExtra("pid", 0));
        if (p == null) { finish(); return; }

        Util.emoji(findViewById(R.id.tvEmoji), p.emoji, p.category);
        ((TextView) findViewById(R.id.tvName)).setText(p.name);
        Util.badge(findViewById(R.id.tvCategory), p.category, Util.color("Other"));
        ((TextView) findViewById(R.id.tvRating)).setText(String.format(Locale.US, "\u2605 %.1f rating", p.rating));
        ((TextView) findViewById(R.id.tvPrice)).setText(Util.money(p.price));
        ((TextView) findViewById(R.id.tvStock)).setText(p.stock <= 0 ? "Out of stock" : p.stock + " units available");
        ((TextView) findViewById(R.id.tvDesc)).setText(p.description);

        TextView tvQty = findViewById(R.id.tvQty);
        findViewById(R.id.btnMinus).setOnClickListener(v -> { if (qty > 1) { qty--; tvQty.setText(String.valueOf(qty)); } });
        findViewById(R.id.btnPlus).setOnClickListener(v -> {
            if (qty < p.stock) { qty++; tvQty.setText(String.valueOf(qty)); }
            else Util.toast(this, "Only " + p.stock + " in stock");
        });
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            String err = db.addToCart(uid, p.id, qty);
            Util.toast(this, err == null ? qty + " x " + p.name + " added to cart" : err);
        });
        TextView wish = findViewById(R.id.btnWish);
        wish.setText(db.isWishlisted(uid, p.id) ? "\u2665  In your wishlist (tap to remove)" : "\u2661  Add to wishlist");
        wish.setOnClickListener(v -> {
            boolean now = db.toggleWishlist(uid, p.id);
            wish.setText(now ? "\u2665  In your wishlist (tap to remove)" : "\u2661  Add to wishlist");
        });
    }
}
