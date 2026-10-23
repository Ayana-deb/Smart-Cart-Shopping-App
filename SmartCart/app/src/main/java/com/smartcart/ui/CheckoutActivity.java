package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.MembershipManager;
import com.smartcart.model.CartItem;
import com.smartcart.model.Order;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.List;

/** Checkout with simulated Cash on Delivery. */
public class CheckoutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_checkout);
        Util.toolbar(this, "Checkout", true);
        DBHelper db = DBHelper.get(this);
        long uid = Session.of(this).userId();
        List<CartItem> cart = db.getCart(uid);
        if (cart.isEmpty()) { Util.toast(this, "Your cart is empty"); finish(); return; }

        StringBuilder sb = new StringBuilder();
        double total = 0;
        for (CartItem ci : cart) {
            sb.append(ci.product.emoji).append("  ").append(ci.product.name).append("\n      ")
              .append(ci.quantity).append(" x ").append(Util.money(ci.product.price)).append(" = ")
              .append(Util.money(ci.product.price * ci.quantity)).append("\n");
            total += ci.product.price * ci.quantity;
        }
        sb.append("\nTotal to pay on delivery: ").append(Util.money(total));
        ((TextView) findViewById(R.id.tvSummary)).setText(sb.toString());

        TextInputLayout til = findViewById(R.id.tilAddress);
        TextInputEditText et = findViewById(R.id.etAddress);
        findViewById(R.id.btnPlace).setOnClickListener(v -> {
            til.setError(null);
            String address = et.getText().toString().trim();
            if (address.length() < 10) { til.setError("Enter a complete delivery address (at least 10 characters)"); return; }
            try {
                long orderId = db.placeOrder(uid, address);
                Order o = db.getOrder(orderId);
                db.addNotification(uid, "Order " + o.code + " placed", "Your order of " + Util.money(o.total) + " was placed. Pay on delivery.");
                MembershipManager.recalculate(this, uid);
                new AlertDialog.Builder(this)
                        .setTitle("Order placed \uD83C\uDF89")
                        .setMessage("Order ID: " + o.code + "\nTotal: " + Util.money(o.total) + "\nPayment: Cash on Delivery\n\nThank you for shopping with SmartCart!")
                        .setCancelable(false)
                        .setPositiveButton("View order", (d, w) -> {
                            Intent i = new Intent(this, OrderDetailActivity.class);
                            i.putExtra("oid", orderId);
                            startActivity(i);
                            finish();
                        })
                        .setNegativeButton("Continue shopping", (d, w) -> finish())
                        .show();
            } catch (IllegalStateException e) {
                Util.toast(this, e.getMessage());
            }
        });
    }
}
