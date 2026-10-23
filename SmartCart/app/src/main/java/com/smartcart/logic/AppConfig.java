package com.smartcart.logic;

import android.content.Context;
import com.smartcart.db.DBHelper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every configurable number lives here. Defaults are defined below; an admin can override
 * any value from the "Rules and settings" screen (overrides are stored in the SQLite settings table).
 */
public final class AppConfig {
    private AppConfig() {}

    public static final String RETURN_WINDOW_DAYS = "return_window_days";
    public static final String MIN_HISTORY_ORDERS = "min_history_orders";
    public static final String RATE_THRESHOLD_PCT = "rate_threshold_pct";
    public static final String PTS_HIGH_RATE = "pts_high_rate";
    public static final String REJECTED_COUNT = "rejected_count";
    public static final String REJECTED_DAYS = "rejected_days";
    public static final String PTS_REJECTED = "pts_rejected";
    public static final String REPEAT_PRODUCT_COUNT = "repeat_product_count";
    public static final String PTS_REPEAT_PRODUCT = "pts_repeat_product";
    public static final String MULTI_ACTIVE_COUNT = "multi_active_count";
    public static final String PTS_MULTI_ACTIVE = "pts_multi_active";
    public static final String FREQ_COUNT = "freq_count";
    public static final String FREQ_DAYS = "freq_days";
    public static final String PTS_FREQUENT = "pts_frequent";
    public static final String PTS_NO_HISTORY = "pts_no_history";
    public static final String RISK_MEDIUM_MIN = "risk_medium_min";
    public static final String RISK_HIGH_MIN = "risk_high_min";
    public static final String MEMBERSHIP_PURCHASE_POINTS = "membership_purchase_points";
    public static final String MEMBERSHIP_REPEAT_REASON_PENALTY = "membership_repeat_reason_penalty";
    public static final String SILVER_MIN_SCORE = "silver_min_score";
    public static final String GOLD_MIN_SCORE = "gold_min_score";

    public static final Map<String, Integer> DEFAULTS = new LinkedHashMap<>();
    public static final Map<String, String> LABELS = new LinkedHashMap<>();

    private static void def(String key, int value, String label) { DEFAULTS.put(key, value); LABELS.put(key, label); }

    static {
        def(RETURN_WINDOW_DAYS, 7, "Return window (days after delivery)");
        def(MIN_HISTORY_ORDERS, 3, "Min delivered orders before return-rate rules apply");
        def(RATE_THRESHOLD_PCT, 40, "Return rate threshold (%)");
        def(PTS_HIGH_RATE, 25, "Points: high return rate");
        def(REJECTED_COUNT, 2, "Recent rejected requests needed to trigger");
        def(REJECTED_DAYS, 90, "Window for rejected requests (days)");
        def(PTS_REJECTED, 25, "Points: multiple recent rejections");
        def(REPEAT_PRODUCT_COUNT, 1, "Earlier completed returns of same product needed");
        def(PTS_REPEAT_PRODUCT, 20, "Points: repeated return of same product");
        def(MULTI_ACTIVE_COUNT, 1, "Other active requests on same order needed");
        def(PTS_MULTI_ACTIVE, 15, "Points: multiple active requests on one order");
        def(FREQ_COUNT, 3, "Earlier requests in window needed (frequent returns)");
        def(FREQ_DAYS, 30, "Window for frequent returns (days)");
        def(PTS_FREQUENT, 15, "Points: unusually frequent requests");
        def(PTS_NO_HISTORY, 5, "Points: no previous completed purchases");
        def(RISK_MEDIUM_MIN, 25, "Medium risk starts at score");
        def(RISK_HIGH_MIN, 50, "High risk starts at score");
        def(MEMBERSHIP_PURCHASE_POINTS, 10, "Membership points per purchase");
        def(MEMBERSHIP_REPEAT_REASON_PENALTY, 10, "Membership penalty per repeated flagged return reason");
        def(SILVER_MIN_SCORE, 30, "Silver: minimum membership score");
        def(GOLD_MIN_SCORE, 60, "Gold: minimum membership score");
    }

    public static int get(Context c, String key) {
        return DBHelper.get(c).getSettingInt(key, DEFAULTS.get(key));
    }
}
