package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.MembershipManager;
import com.smartcart.model.Constants;
import com.smartcart.model.CustomerHistory;
import com.smartcart.model.ReturnRequest;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin summary statistics plus a filterable list of every return request. */
public class AdminDashboardActivity extends AppCompatActivity {
    private static final String[] FILTERS = {"All", "Pending", "Under Review", "High Risk", "Approved", "Pickup Scheduled", "Refund Processed", "Rejected"};
    private DBHelper db;
    private SimpleAdapter<ReturnRequest> adapter;
    private Spinner spFilter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Session s = Session.of(this);
        if (!s.loggedIn() || !s.isAdmin()) { // restricted to the demo administrator
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_admin_dashboard);
        Util.toolbar(this, "Admin dashboard", false);
        db = DBHelper.get(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        spFilter = findViewById(R.id.spFilter);
        spFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, FILTERS));
        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { loadList(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleAdapter<>(new ArrayList<>(), R.layout.item_admin_request, (v, r) -> {
            ((TextView) v.findViewById(R.id.tvTitle)).setText("#" + r.id + "  " + r.productName);
            Util.badge(v.findViewById(R.id.tvRisk), r.riskLevel + " risk (" + r.riskScore + ")", Util.color(r.riskLevel));
            ((TextView) v.findViewById(R.id.tvInfo)).setText(r.userName + "  \u2022  " + r.userTier + " (" + r.userScore + " pts)  \u2022  Qty " + r.quantity + "  \u2022  " + Util.date(r.requestDate));
            Util.badge(v.findViewById(R.id.tvStatus), r.status, Util.color(r.status));
            v.setOnClickListener(x -> {
                Intent i = new Intent(this, AdminRequestDetailActivity.class);
                i.putExtra("rid", r.id);
                startActivity(i);
            });
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db == null) return;
        LinearLayout r1 = findViewById(R.id.row1), r2 = findViewById(R.id.row2);
        r1.removeAllViews(); r2.removeAllViews();
        stat(r1, db.countReturnsWhere("1=1"), "Total");
        stat(r1, db.countReturnsWhere("status='Pending'"), "Pending");
        stat(r1, db.countReturnsWhere("status='Under Review'"), "Under review");
        stat(r2, db.countReturnsWhere("risk_level='High'"), "High risk");
        stat(r2, db.countReturnsWhere("status IN (" + Constants.COMPLETED_SQL + ")"), "Approved");
        stat(r2, db.countReturnsWhere("status='Rejected'"), "Rejected");
        loadList();
    }

    private void stat(LinearLayout row, int value, String label) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_stat, row, false);
        ((TextView) v.findViewById(R.id.tvValue)).setText(String.valueOf(value));
        ((TextView) v.findViewById(R.id.tvLabel)).setText(label);
        row.addView(v);
    }

    private void loadList() {
        List<ReturnRequest> list = db.getAllReturns((String) spFilter.getSelectedItem());
        Map<Long, Integer> scores = new HashMap<>();
        for (ReturnRequest r : list) {
            Integer score = scores.get(r.userId);
            if (score == null) {
                CustomerHistory history = db.loadHistory(r.userId);
                score = MembershipManager.score(this, history);
                scores.put(r.userId, score);
            }
            r.userScore = score;
            r.userTier = MembershipManager.tierForScore(this, score);
        }
        adapter.setItems(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_config) {
            startActivity(new Intent(this, AdminConfigActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            Session.of(this).logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
