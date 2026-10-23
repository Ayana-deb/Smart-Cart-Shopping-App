package com.smartcart.logic;

import android.content.Context;
import com.smartcart.db.DBHelper;
import com.smartcart.model.Constants;
import com.smartcart.model.CustomerHistory;
import com.smartcart.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rule-based, explainable return risk scoring.
 * fraud-detection model and its output should only be used to route requests for human review.
 *
 * Double counting is avoided by giving each rule its own slice of the history:
 *  - rule 1 uses completed (approved) units only; Pending and Under Review are excluded
 *  - rule 2 uses rejected requests only
 *  - rule 3 uses completed returns of the same product only
 *  - rule 4 uses active requests on the SAME order only
 *  - rule 5 uses non-rejected requests (rejections are already scored by rule 2)
 * Rules based on history (1) only apply once the customer has enough delivered orders.
 */
public class FraudDetectionManager {
    public static class RiskResult {
        public int score;
        public String level;
        public final List<String> reasons = new ArrayList<>();

        void add(int pts, String why) { score += pts; reasons.add("+" + pts + "  " + why); }

        public String explanation() {
            if (reasons.isEmpty()) return "No risk rules were triggered.";
            StringBuilder sb = new StringBuilder();
            for (String r : reasons) sb.append(r).append("\n");
            return sb.toString().trim();
        }
    }

    private final Context ctx;
    private final DBHelper db;

    public FraudDetectionManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.db = DBHelper.get(ctx);
    }

    private int cfg(String key) { return AppConfig.get(ctx, key); }

    /** Evaluates the risk of a NEW request (it is not saved yet, so it is not part of the history). */
    public RiskResult evaluate(long userId, long orderId, long productId) {
        RiskResult r = new RiskResult();
        CustomerHistory h = db.loadHistory(userId);
        long now = System.currentTimeMillis();

        // Rule 1: high return rate, only with enough purchase history
        int minOrders = cfg(AppConfig.MIN_HISTORY_ORDERS);
        double rate = h.returnRatePct();
        if (h.deliveredOrders >= minOrders && rate >= cfg(AppConfig.RATE_THRESHOLD_PCT)) {
            r.add(cfg(AppConfig.PTS_HIGH_RATE), String.format(Locale.US,
                    "High return rate: %.0f%% of delivered units were returned (threshold %d%%, %d delivered orders)",
                    rate, cfg(AppConfig.RATE_THRESHOLD_PCT), h.deliveredOrders));
        }

        // Rule 2: multiple recently rejected requests
        int days = cfg(AppConfig.REJECTED_DAYS);
        int rejected = db.countReturns(userId, "'" + Constants.REJECTED + "'", now - days * Util.DAY, 0, 0);
        if (rejected >= cfg(AppConfig.REJECTED_COUNT)) {
            r.add(cfg(AppConfig.PTS_REJECTED), rejected + " requests were rejected in the last " + days + " days");
        }

        // Rule 3: repeated returns of the same product
        int same = db.countReturns(userId, Constants.COMPLETED_SQL, 0, productId, 0);
        if (same >= cfg(AppConfig.REPEAT_PRODUCT_COUNT)) {
            r.add(cfg(AppConfig.PTS_REPEAT_PRODUCT), "This product was already returned " + same + " time(s) before");
        }

        // Rule 4: multiple active requests for one order
        int others = db.countReturns(userId, Constants.ACTIVE_SQL, 0, 0, orderId);
        if (others >= cfg(AppConfig.MULTI_ACTIVE_COUNT)) {
            r.add(cfg(AppConfig.PTS_MULTI_ACTIVE), others + " other active request(s) exist for the same order");
        }

        // Rule 5: unusually frequent requests (non-rejected only)
        int fdays = cfg(AppConfig.FREQ_DAYS);
        int recent = db.countReturns(userId, Constants.NON_REJECTED_SQL, now - fdays * Util.DAY, 0, 0);
        if (recent >= cfg(AppConfig.FREQ_COUNT)) {
            r.add(cfg(AppConfig.PTS_FREQUENT), recent + " return requests were made in the last " + fdays + " days");
        }

        // Rule 6: no previous completed purchase (the order being returned is itself delivered, so subtract it).
        // Small informational weight only; it can never reach Medium on its own.
        if (h.deliveredOrders - 1 <= 0) {
            r.add(cfg(AppConfig.PTS_NO_HISTORY), "No previous completed purchases (informational, not suspicious by itself)");
        }

        if (r.score >= cfg(AppConfig.RISK_HIGH_MIN)) r.level = Constants.HIGH;
        else if (r.score >= cfg(AppConfig.RISK_MEDIUM_MIN)) r.level = Constants.MEDIUM;
        else r.level = Constants.LOW;
        return r;
    }
}
