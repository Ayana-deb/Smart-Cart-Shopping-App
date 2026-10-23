package com.smartcart.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.ReturnEligibilityManager;
import com.smartcart.model.Constants;
import com.smartcart.model.ReturnRequest;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.ArrayList;
import java.util.List;

/** Customer view of return requests. Risk score and explanation are never shown here. */
public class ReturnHistoryActivity extends AppCompatActivity {
    private SimpleAdapter<ReturnRequest> adapter;
    private TextView tvEmpty;
    private long uid;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_list);
        Util.toolbar(this, "Return history", true);
        uid = Session.of(this).userId();
        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No return requests yet \u21A9\uFE0F\nReturns you request will be tracked here.");
        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleAdapter<>(new ArrayList<>(), R.layout.item_return, this::bind);
        rv.setAdapter(adapter);
    }

    @Override protected void onResume() { super.onResume(); load(); }

    private void load() {
        List<ReturnRequest> list = DBHelper.get(this).getReturns(uid);
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bind(View v, ReturnRequest r) {
        ((TextView) v.findViewById(R.id.tvTitle)).setText("#" + r.id + "  " + r.productName);
        Util.badge(v.findViewById(R.id.tvStatus), r.status, Util.color(r.status));
        String info = "Qty " + r.quantity + "  \u2022  " + r.reason + "  \u2022  " + Util.date(r.requestDate);
        if (r.resolutionDate > 0) info += "\nResolved " + Util.date(r.resolutionDate);
        ((TextView) v.findViewById(R.id.tvInfo)).setText(info);
        ((TextView) v.findViewById(R.id.tvMessage)).setText(r.customerMessage());
        ((TextView) v.findViewById(R.id.tvProgress)).setText(r.progress());
        MaterialButton sim = v.findViewById(R.id.btnSimulate);
        boolean canSim = Constants.APPROVED.equals(r.status) || Constants.PICKUP.equals(r.status);
        sim.setVisibility(canSim ? View.VISIBLE : View.GONE);
        sim.setText(Constants.APPROVED.equals(r.status) ? "Demo: simulate pickup" : "Demo: simulate refund");
        sim.setOnClickListener(x -> { new ReturnEligibilityManager(this).advance(r.id); load(); });
    }
}
