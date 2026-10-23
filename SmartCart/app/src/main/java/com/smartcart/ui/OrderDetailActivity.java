package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.AppConfig;
import com.smartcart.logic.MembershipManager;
import com.smartcart.model.Constants;
import com.smartcart.model.Order;
import com.smartcart.model.OrderItem;
import com.smartcart.util.Session;
import com.smartcart.util.Util;

public class OrderDetailActivity extends AppCompatActivity {
    private DBHelper db;
    private long oid;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_order_detail);
        Util.toolbar(this, "Order details", true);
        db = DBHelper.get(this);
        oid = getIntent().getLongExtra("oid", 0);

        findViewById(R.id.btnAdvance).setOnClickListener(v -> {
            String st = db.advanceOrder(oid);
            if (st != null) {
                Order o = db.getOrder(oid);
                db.addNotification(o.userId, "Order " + o.code + " " + st.toLowerCase(), "Your order is now: " + st + ".");
                MembershipManager.recalculate(this, o.userId); // delivery changes purchase history
            }
            render();
        });
        findViewById(R.id.btnBackdate).setOnClickListener(v -> { db.setDeliveredAt(oid, Util.daysAgo(8)); render(); });
        findViewById(R.id.btnResetDate).setOnClickListener(v -> { db.setDeliveredAt(oid, System.currentTimeMillis()); render(); });
    }

    @Override protected void onResume() { super.onResume(); render(); }

    private void render() {
        Order o = db.getOrder(oid);
        if (o == null || o.userId != Session.of(this).userId()) { finish(); return; }
        ((TextView) findViewById(R.id.tvCode)).setText(o.code);
        Util.badge(findViewById(R.id.tvStatus), o.status, Util.color(o.status));

        int stage = Constants.PLACED.equals(o.status) ? 0 : Constants.SHIPPED.equals(o.status) ? 1 : 2;
        String[] steps = {"Placed", "Shipped", "Delivered"};
        StringBuilder pr = new StringBuilder();
        for (int i = 0; i < 3; i++) pr.append(i <= stage ? "\u25CF " : "\u25CB ").append(steps[i]).append(i < 2 ? "  \u2500  " : "");
        ((TextView) findViewById(R.id.tvProgress)).setText(pr.toString());

        StringBuilder d = new StringBuilder("Placed: " + Util.dateTime(o.placedAt));
        if (o.shippedAt > 0) d.append("\nShipped: ").append(Util.dateTime(o.shippedAt));
        boolean delivered = Constants.DELIVERED.equals(o.status);
        if (delivered) {
            int window = AppConfig.get(this, AppConfig.RETURN_WINDOW_DAYS);
            int days = Util.daysSince(o.deliveredAt);
            d.append("\nDelivered: ").append(Util.dateTime(o.deliveredAt));
            d.append(days <= window ? "\nReturns open for " + (window - days) + " more day(s)" : "\nReturn window closed (" + window + " days)");
        }
        ((TextView) findViewById(R.id.tvDates)).setText(d.toString());

        LinearLayout items = findViewById(R.id.llItems);
        items.removeAllViews();
        for (OrderItem it : db.getOrderItems(oid)) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_order_line, items, false);
            Util.emoji((TextView) row.findViewById(R.id.tvEmoji), it.emoji, "x" + it.name);
            ((TextView) row.findViewById(R.id.tvName)).setText(it.name);
            ((TextView) row.findViewById(R.id.tvQty)).setText(it.quantity + " x " + Util.money(it.price));
            View btn = row.findViewById(R.id.btnReturn);
            if (delivered) {
                btn.setVisibility(View.VISIBLE);
                btn.setOnClickListener(v -> {
                    Intent i = new Intent(this, ReturnRequestActivity.class);
                    i.putExtra("oid", oid);
                    i.putExtra("pid", it.productId);
                    startActivity(i);
                });
            }
            items.addView(row);
        }
        ((TextView) findViewById(R.id.tvTotal)).setText("Total: " + Util.money(o.total));
        ((TextView) findViewById(R.id.tvAddress)).setText("Deliver to:\n" + o.address + "\n\nPayment: " + o.payment);

        TextView adv = findViewById(R.id.btnAdvance);
        adv.setVisibility(delivered ? View.GONE : View.VISIBLE);
        adv.setText(Constants.PLACED.equals(o.status) ? "Demo: mark as Shipped" : "Demo: mark as Delivered");
        findViewById(R.id.btnBackdate).setVisibility(delivered ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnResetDate).setVisibility(delivered ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvDemoLabel).setVisibility(View.VISIBLE);
    }
}
