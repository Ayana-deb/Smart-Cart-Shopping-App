package com.smartcart.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.AppConfig;
import com.smartcart.logic.MembershipManager;
import com.smartcart.model.Constants;
import com.smartcart.model.CustomerHistory;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.Locale;

public class MembershipActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_membership);
        Util.toolbar(this, "Membership", true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        long uid = Session.of(this).userId();
        String tier = MembershipManager.recalculate(this, uid); // always show an up-to-date tier
        CustomerHistory h = DBHelper.get(this).loadHistory(uid);
        int score = MembershipManager.score(this, h);

        ((TextView) findViewById(R.id.tvMedal)).setText(Constants.GOLD.equals(tier) ? "\uD83E\uDD47" : Constants.SILVER.equals(tier) ? "\uD83E\uDD48" : "\uD83E\uDD49");
        Util.badge(findViewById(R.id.tvTier), tier + " member", Util.color(tier));
        ((TextView) findViewById(R.id.tvBenefits)).setText(MembershipManager.benefits(tier));

        int minHist = AppConfig.get(this, AppConfig.MIN_HISTORY_ORDERS);
        String rate = h.deliveredOrders >= minHist ? String.format(Locale.US, "%.0f%%", h.returnRatePct()) : "not counted yet (needs " + minHist + " delivered orders)";
        ((TextView) findViewById(R.id.tvStats)).setText(
                "Delivered orders: " + h.deliveredOrders
                + "\nUnits purchased: " + h.deliveredUnits
                + "\nTotal spent: " + Util.money(h.totalSpent)
                + "\nCompleted returns: " + h.completedReturnUnits + " unit(s)"
                + "\nRejected returns: " + h.rejectedReturns
                + "\nReturn rate: " + rate
                + "\nMembership score: " + score
                + "\nRepeated flagged return reasons: " + h.repeatedFlaggedReasons);

        String next = MembershipManager.nextTier(tier);
        View card = findViewById(R.id.cardNext);
        if (next == null) {
            ((TextView) findViewById(R.id.tvNextTitle)).setText("You are at the top tier. Thank you for shopping with us!");
            findViewById(R.id.pbScore).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvReq)).setText("Your current score keeps you in Gold.");
            return;
        }
        card.setVisibility(View.VISIBLE);
        findViewById(R.id.pbScore).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tvNextTitle)).setText("Progress to " + next);
        ((ProgressBar) findViewById(R.id.pbScore)).setProgress(MembershipManager.scoreProgress(this, score, next));
        ((TextView) findViewById(R.id.tvReq)).setText(android.text.TextUtils.join("\n", MembershipManager.requirements(this, score, next)));
    }
}
