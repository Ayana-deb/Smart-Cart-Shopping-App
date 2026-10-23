package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.model.Order;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {
    private SimpleAdapter<Order> adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_list);
        Util.toolbar(this, "My orders", true);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No orders yet \uD83D\uDCE6\nYour placed orders will appear here.");
        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        DBHelper db = DBHelper.get(this);
        adapter = new SimpleAdapter<>(new ArrayList<>(), R.layout.item_order, (v, o) -> {
            ((TextView) v.findViewById(R.id.tvCode)).setText(o.code);
            Util.badge(v.findViewById(R.id.tvStatus), o.status, Util.color(o.status));
            int n = 0;
            for (com.smartcart.model.OrderItem it : db.getOrderItems(o.id)) n += it.quantity;
            ((TextView) v.findViewById(R.id.tvInfo)).setText("Placed " + Util.date(o.placedAt) + "  \u2022  " + n + " item(s)");
            ((TextView) v.findViewById(R.id.tvTotal)).setText(Util.money(o.total));
            v.setOnClickListener(x -> {
                Intent i = new Intent(this, OrderDetailActivity.class);
                i.putExtra("oid", o.id);
                startActivity(i);
            });
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Order> list = DBHelper.get(this).getOrders(Session.of(this).userId());
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
