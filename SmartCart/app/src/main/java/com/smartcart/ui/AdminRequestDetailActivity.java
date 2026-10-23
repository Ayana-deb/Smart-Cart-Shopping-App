package com.smartcart.ui;

import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.AppConfig;
import com.smartcart.logic.MembershipManager;
import com.smartcart.logic.ReturnEligibilityManager;
import com.smartcart.model.Constants;
import com.smartcart.model.CustomerHistory;
import com.smartcart.model.ReturnRequest;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.Locale;
import java.io.File;

/** Admin review screen: risk explanation, customer history, decision and status controls. */
public class AdminRequestDetailActivity extends AppCompatActivity {
    private DBHelper db;
    private long rid;
    private Spinner spStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Session s = Session.of(this);
        if (!s.loggedIn() || !s.isAdmin()) { finish(); return; }
        setContentView(R.layout.activity_admin_request);
        Util.toolbar(this, "Review request", true);
        db = DBHelper.get(this);
        rid = getIntent().getLongExtra("rid", 0);
        spStatus = findViewById(R.id.spStatus);
        spStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Constants.RETURN_STATUSES));
        TextInputLayout til = findViewById(R.id.tilReject);
        TextInputEditText et = findViewById(R.id.etReject);
        ReturnEligibilityManager mgr = new ReturnEligibilityManager(this);

        findViewById(R.id.btnApprove).setOnClickListener(v -> { mgr.approve(rid); Util.toast(this, "Request approved"); render(); });
        findViewById(R.id.btnReject).setOnClickListener(v -> {
            til.setError(null);
            String reason = et.getText().toString().trim();
            if (reason.length() < 5) { til.setError("Please record a rejection reason (min 5 characters)"); return; }
            mgr.reject(rid, reason);
            et.setText("");
            Util.toast(this, "Request rejected");
            render();
        });
        findViewById(R.id.btnUpdate).setOnClickListener(v -> {
            mgr.updateStatus(rid, (String) spStatus.getSelectedItem());
            Util.toast(this, "Status updated");
            render();
        });
    }

    @Override protected void onResume() { super.onResume(); if (db != null) render(); }

    private void render() {
        ReturnRequest r = db.getReturn(rid);
        if (r == null) { finish(); return; }
        Util.badge(findViewById(R.id.tvStatus), r.status, Util.color(r.status));
        Util.badge(findViewById(R.id.tvRiskBadge), r.riskLevel + " risk", Util.color(r.riskLevel));
        Util.badge(findViewById(R.id.tvTierBadge), r.tier + " at submission", Util.color(r.tier));

        String sum = "Request #" + r.id + "\nCustomer: " + r.userName + "\nProduct: " + r.productName + "  (qty " + r.quantity + ")"
                + "\nOrder ID: " + r.orderId + "\nReason: " + r.reason
                + (r.description == null || r.description.isEmpty() ? "" : "\nDescription: " + r.description)
                + "\nSubmitted: " + Util.dateTime(r.requestDate)
                + "\nResolved: " + (r.resolutionDate > 0 ? Util.dateTime(r.resolutionDate) : "not yet");
        ((TextView) findViewById(R.id.tvSummary)).setText(sum);

        ImageView evidence = findViewById(R.id.ivEvidence);
        TextView noEvidence = findViewById(R.id.tvNoEvidence);
        if (r.photoPath != null && new File(r.photoPath).isFile()) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(r.photoPath, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            int sampleSize = 1;
            while (bounds.outWidth / sampleSize > 1600 || bounds.outHeight / sampleSize > 1600) sampleSize *= 2;
            options.inSampleSize = sampleSize;
            Bitmap photo = BitmapFactory.decodeFile(r.photoPath, options);
            evidence.setImageBitmap(photo);
            evidence.setVisibility(photo == null ? View.GONE : View.VISIBLE);
            noEvidence.setVisibility(photo == null ? View.VISIBLE : View.GONE);
        } else {
            evidence.setImageDrawable(null);
            evidence.setVisibility(View.GONE);
            noEvidence.setVisibility(View.VISIBLE);
        }

        ((TextView) findViewById(R.id.tvRisk)).setText("Score: " + r.riskScore + "  (" + r.riskLevel + ")\n\n" + r.riskExplanation
                + "\n\nThis is a rule-based prototype score, not a proven fraud model. Use it to prioritise review, not to reject automatically.");

        CustomerHistory h = db.loadHistory(r.userId);
        int minHist = AppConfig.get(this, AppConfig.MIN_HISTORY_ORDERS);
        int membershipScore = MembershipManager.score(this, h);
        String currentTier = MembershipManager.tierForScore(this, membershipScore);
        ((TextView) findViewById(R.id.tvHistory)).setText(
                "Delivered orders: " + h.deliveredOrders + "\nUnits purchased: " + h.deliveredUnits
                + "\nTotal spent: " + Util.money(h.totalSpent)
                + "\nTotal return requests: " + h.totalRequests
                + "\nCompleted return units: " + h.completedReturnUnits
                + "\nRejected requests: " + h.rejectedReturns
                + "\nReturn rate: " + (h.deliveredOrders >= minHist ? String.format(Locale.US, "%.0f%%", h.returnRatePct()) : "n/a (not enough history)")
                + "\nMembership score: " + membershipScore
                + "\nCurrent tier: " + currentTier);

        StringBuilder tl = new StringBuilder();
        for (String[] e : db.getEvents(r.id)) tl.append("\u2022 ").append(Util.dateTime(Long.parseLong(e[2]))).append("\n   ").append(e[0]).append(": ").append(e[1]).append("\n");
        if (r.rejectionReason != null && !r.rejectionReason.isEmpty()) tl.append("\nRejection reason: ").append(r.rejectionReason);
        ((TextView) findViewById(R.id.tvTimeline)).setText(tl.toString().trim());

        for (int i = 0; i < Constants.RETURN_STATUSES.length; i++)
            if (Constants.RETURN_STATUSES[i].equals(r.status)) spStatus.setSelection(i);
    }
}
